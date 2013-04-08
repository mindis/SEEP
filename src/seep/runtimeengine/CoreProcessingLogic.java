package seep.runtimeengine;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import seep.buffer.Buffer;
import seep.comm.serialization.ControlTuple;
import seep.comm.serialization.controlhelpers.Ack;
import seep.comm.serialization.controlhelpers.BackupNodeState;
import seep.comm.serialization.controlhelpers.BackupOperatorState;
import seep.comm.serialization.controlhelpers.BackupRI;
import seep.comm.serialization.controlhelpers.InitNodeState;
import seep.comm.serialization.controlhelpers.InitOperatorState;
import seep.comm.serialization.controlhelpers.InitRI;
import seep.comm.serialization.controlhelpers.ScaleOutInfo;
import seep.infrastructure.NodeManager;
import seep.operator.State;
import seep.operator.OperatorContext.PlacedOperator;
import seep.processingunit.PUContext;
import seep.processingunit.ProcessingUnit;


public class CoreProcessingLogic implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private CoreRE owner;
	private ProcessingUnit pu;
	private PUContext puCtx;
	
	
	public void setOwner(CoreRE owner) {
		this.owner = owner;
	}
	public void setOpContext(PUContext puCtx) {
		this.puCtx = puCtx;
	}
	public void setProcessingUnit(ProcessingUnit processingUnit){
		this.pu = processingUnit;
	}
	
	//map where it is saved the ack received by each downstream
	private Map<Integer, Long> downstreamLastAck = new HashMap<Integer, Long>();
	
	//routing information, operatorCLASS - backupRI
//	private Seep.BackupRI backupRI = null;
	private HashMap<String, BackupRI> backupRoutingInformation = new HashMap<String, BackupRI>();
	
	/// \todo{check if i can avoid operations in the data structure when receiving same msg again and again}
	public void storeBackupRI(BackupRI backupRI){
		//Get operator Class
		String operatorType = backupRI.getOperatorType();
		//Save in the provided map the backupRI for this upstream, if there are replicas then we will have replicated info here...
		backupRoutingInformation.put(operatorType, backupRI);
	}
	
	/**  installRI receives a message with the routing information that this operator must implement 
	 * there are N indexes, and thus, there must be N load balancers. Each of these load balancers must have the same indexes,
	 * this is, the same routing information.
	 * Consequently, I have to locate the load balancers that this operator has right now and change the routing information.
	 * Then, create the new required ones with the new information**/
	
	public synchronized void installRI(InitRI initRI){
		NodeManager.nLogger.info("-> Installing RI from Node: "+initRI.getNodeId());
		//Create new LB with the information received
		ArrayList<Integer> indexes = new ArrayList<Integer>(initRI.getIndex());
		ArrayList<Integer> keys = new ArrayList<Integer>(initRI.getKey());
		ArrayList<Integer> downstreamIds = new ArrayList<Integer>();
		for(Integer index : indexes){
			// opId from the most downstream operator
			int opId = pu.getMostDownstream().getOpContext().getDownOpIdFromIndex(index);
			downstreamIds.add(opId);
		}
		// Reconfigure routing info of the most downstream operator
		pu.getMostDownstream().getRouter().reconfigureRoutingInformation(downstreamIds, indexes, keys);
	}
	
	public void sendRoutingInformation(int opId, String operatorType){
		//Get index from opId
		int upstreamIndex = 0;
		for(PlacedOperator op : pu.getMostUpstream().getOpContext().upstreams){
			if(op.opID() == opId){
				 upstreamIndex = op.index();
			}
		}
		System.out.println("## NEW UPSTREAM, op: "+opId+" type: "+operatorType);
		//If i dont have backup for that upstream, I am not in charge...
		System.out.println("BACKUPROUTINGINFO: "+backupRoutingInformation.toString());
		System.out.println("OP TYPE: "+operatorType);
		if(!backupRoutingInformation.containsKey(operatorType)){
			NodeManager.nLogger.info("-> NO routing info to send");
			return;
		}
		NodeManager.nLogger.info("-> Sending backupRI to upstream");
System.out.println("KEY: "+operatorType);
		//Otherwise I pick the backupRI msg from the data structure where i am storing these ones
		BackupRI backupRI = backupRoutingInformation.get(operatorType);
		//Form the message
		ControlTuple ct = new ControlTuple().makeInitRI(owner.getNodeDescr().getNodeId(), backupRI.getIndex(), backupRI.getKey());
		//Send the message
		owner.getControlDispatcher().sendUpstream(ct, upstreamIndex);
	}
	
	public void processBackupState(BackupNodeState ct){
		int opId = ct.getUpstreamOpId();
		CommunicationChannel downStream = puCtx.getCCIfromOpId(opId, "d");
		long ts_e = ct.getBackupOperatorStateWithOpId(ct.getUpstreamOpId()).getState().getData_ts();
		///\todo{ check if ts_e is the last thing processed by the most upstream op in the downstream node, or the most downstream op in the down node}
		if(downStream.reconf_ts <= ts_e){
			/** DONT really understand the objective of the next 6 lines, CHECK**/
			int eopId = -5;
			if(puCtx.getBuffer(opId).getBackupState() != null){
				if(puCtx.getBuffer(opId).getBackupState().getBackupOperatorState() != null){
					eopId = puCtx.getBuffer(opId).getBackupState().getBackupOperatorState()[0].getOpId();
				}
			}
			/** **/
			puCtx.getBuffer(opId).replaceBackupNodeState(ct);
		}
		else{
			NodeManager.nLogger.warning("-> Received state generated after the beginning of the reconfigure process");
		}
	}

	//Send ACK tuples to the upstream nodes and update TS_ACK
	/// \todo{now this method is syncrhonized, with ACK Tile mechanism we can avoid most of the method, but it is not finished yet}
	public synchronized void processAck(Ack ct){
		int opId = ct.getOpId();
		long current_ts = ct.getTs();
		int counter = 0;
		long minWithCurrent = current_ts;
		long minWithoutCurrent = Long.MAX_VALUE;
		Buffer buffer = puCtx.getBuffer(opId);
		if(buffer != null){
			buffer.trim(current_ts);
		}
		//i.e. if the operator is stateless (we should use a tagging interface). Stateless and NOT first operator
//		if ((owner.subclassOperator instanceof StatelessOperator) && !((opContext.upstreams.size()) == 0)) {
		for( Map.Entry<Integer, Long> entry: downstreamLastAck.entrySet()) {
			long ts = entry.getValue();
			if (entry.getKey() != opId) {
				if(ts < minWithCurrent){ 
					minWithCurrent = ts;
				}
			}
			if(ts < minWithoutCurrent){
				minWithoutCurrent = ts;
			}
			counter++;
		}
		downstreamLastAck.put(opId, current_ts);
			//FIXME if I remove a downstream (scale down) I should remove from map/clear the map
	/// \todo {scaling down operators introduce a new bunch of possibilities}
		if(downstreamLastAck.size() != pu.getMostDownstream().getOpContext().downstreams.size()) {
		//			System.out.println("missing some acks suppressing ACK");
		}
		else if (minWithCurrent==minWithoutCurrent) {
		//			System.out.println("min did not changed, suppress ACK");
		}
		else{
			owner.ack(minWithCurrent);
		}
//		}
		//if the operator is stateful, we backpropagate the ACK directly. with batching, we will always propagate unique ACK's
		//else if (owner.subclassOperator instanceof StatefullOperator){
//		else{
//			owner.ack(current_ts);
//		}
		// To indicate that this is the last ack processed by this operator
		owner.setTs_ack(current_ts);
	}
	
	public void splitState(int oldOpId, int newOpId, int key) {
//		StateSplitI operatorToSplit = (StateSplitI)owner.getSubclassOperator();
//		BackupOperatorState oldState = puCtx.getBuffer(oldOpId).getBackupState();
		BackupNodeState backupNodeState = puCtx.getBuffer(oldOpId).getBackupState();
		for(BackupOperatorState bos : backupNodeState.getBackupOperatorState()){
			System.out.println("% Got Buffer from OP: "+oldOpId+" and BOS opId is: "+bos.getOpId());
		}
		
		/** Small hack
		BackupOperatorState backupOperatorState = backupNodeState.getBackupOperatorStateWithOpId(oldOpId);
		**/
		/** DUE to the last refactorization, we added a layer of indirection, making an explicit differentiation between node and operator state.
		 * the problem of this approach is that it was breaking here. I have to access the state directly, something that without the node layer
		 * was assumed. With the node layer, tough, the system looks for the particular ID, that might have been lost in previous calls to splitState**/
		BackupOperatorState backupOperatorState = backupNodeState.getBackupOperatorState()[0];
		
		
		State stateToSplit = backupOperatorState.getState();
		int stateCheckpointInterval = stateToSplit.getCheckpointInterval();
		String stateTag = stateToSplit.getStateTag();
		long data_ts = stateToSplit.getData_ts();
		State splitted[] = stateToSplit.splitState(stateToSplit, key);
		// Create the two states, and fill the necessary info, as ts, etc.
		//Set the old partition
		State oldStatePartition = splitted[0];
		oldStatePartition.setOwnerId(oldOpId);
		oldStatePartition.setCheckpointInterval(stateCheckpointInterval);
		oldStatePartition.setStateTag(stateTag);
		oldStatePartition.setData_ts(data_ts);
		// Set the new partition
		State newStatePartition = splitted[1];
		newStatePartition.setOwnerId(newOpId);
		newStatePartition.setCheckpointInterval(stateCheckpointInterval);
		newStatePartition.setStateTag(stateTag);
		newStatePartition.setData_ts(data_ts);
		
		BackupOperatorState newPartition = new BackupOperatorState();
		BackupOperatorState oldPartition = new BackupOperatorState();
		
		newPartition.setState(newStatePartition);
		oldPartition.setState(oldStatePartition);

		NodeManager.nLogger.severe("-> Replacing backup states for OP: "+oldOpId+" and OP: "+newOpId);
		
		// Replace state in the old operator
		
		backupNodeState.replaceOpBackupWithId(oldOpId, oldPartition);
		BackupNodeState newBackupNodeState = new BackupNodeState();
//		BackupNodeState oldBackupNodeState = new BackupNodeState();
		// Create the new state for the new replica
		BackupOperatorState[] newBackupOperatorState = new BackupOperatorState[1];
//		BackupOperatorState[] oldBackupOperatorState = new BackupOperatorState[1]; // ASDF
		newBackupOperatorState[0] = newPartition;
//		oldBackupOperatorState[0] = oldPartition; // ASDF
		newBackupNodeState.setBackupOperatorState(newBackupOperatorState);
//		puCtx.getBuffer(oldOpId).replaceBackupNodeState(oldBackupNodeState); // ASDF
		puCtx.getBuffer(newOpId).replaceBackupNodeState(newBackupNodeState);
		//Indicate that we are now managing both these states
		NodeManager.nLogger.severe("-> Registering management of state for OP: "+oldOpId+" and OP: "+newOpId);
		// We state we manage the state of the old and new ops replicas
		pu.registerManagedState(oldOpId);
		pu.registerManagedState(newOpId);
		
		
		System.out.println("% After SPLIT, oldOpId: "+puCtx.getBuffer(oldOpId).getBackupState().getBackupOperatorState()[0].getOpId()+" newOpId: "+puCtx.getBuffer(newOpId).getBackupState().getBackupOperatorState()[0].getOpId());
	}
	
	public synchronized void scaleOut(ScaleOutInfo scaleOutInfo, int newOpIndex, int oldOpIndex){
		int oldOpId = scaleOutInfo.getOldOpId();
		int newOpId = scaleOutInfo.getNewOpId();
		boolean isStatefulScaleOut = scaleOutInfo.isStatefulScaleOut();
		
		//if operator is stateful and (this) can split state
//		if(opContext.isDownstreamOperatorStateful(oldOpId) && owner.subclassOperator instanceof StateSplitI){
		//If scaling operator is stateful
		if(isStatefulScaleOut){
			NodeManager.nLogger.info("-> Scaling out STATEFUL op");
			scaleOutStatefulOperator(oldOpId, newOpId, oldOpIndex, newOpIndex);
		}
		
		// If operator splitting is stateless...
//		else if (!opContext.isDownstreamOperatorStateful(oldOpId)){
		else{
			NodeManager.nLogger.info("-> Scaling out STATELESS op");
//			configureNewDownstreamStatelessOperatorPartition(oldOpId, newOpId, oldOpIndex, newOpIndex);
			pu.getMostDownstream().getRouter().newOperatorPartition(oldOpId, newOpId, oldOpIndex, newOpIndex);
		}
	}
	
	public void scaleOutStatefulOperator(int oldOpId, int newOpId, int oldOpIndex, int newOpIndex){
		//All operators receiving the scale-out message have to change their routing information
		int newKey = configureNewDownstreamStatefulOperatorPartition(oldOpId, newOpId, oldOpIndex, newOpIndex);
		//I have configured the new split, check if I am also in charge of splitting the state or not
		if(pu.isManagingStateOf(oldOpId)){
			NodeManager.nLogger.info("-> Splitting state");
			splitState(oldOpId, newOpId, newKey);
			pu.setSystemStatus(ProcessingUnit.SystemStatus.WAITING_FOR_STATE_ACK);
			//Just one operator needs to send routing information backup, cause downstream is saving this info according to op type.
			NodeManager.nLogger.info("-> Generating and sending RI backup");
//			backupRoutingInformation(oldOpId);
		}
		else{
			NodeManager.nLogger.info("-> NOT in charge of split state");
		}
		
		pu.setSystemStatus(ProcessingUnit.SystemStatus.WAITING_FOR_STATE_ACK);
		//Just one operator needs to send routing information backup, cause downstream is saving this info according to op type.
		NodeManager.nLogger.info("-> Generating and sending RI backup");
//		backupRoutingInformation(oldOpId);
		
		//I always backup the routing info. This leads to replicate messages, but I cant avoid it easily since I can have more than one type of upstream
		///\fixme{FIX THIS INEFFICIENCY}
		backupRoutingInformation(oldOpId);
	}
	
	public int configureNewDownstreamStatefulOperatorPartition(int oldOpId, int newOpId, int oldOpIndex, int newOpIndex){
		int newKey = -1;
		
		/** BLOCK OF CODE TO REFACTOR **/
		//. stop sending data to op1 remember last data sent
		CommunicationChannel oldConnection = ((CommunicationChannel)puCtx.getDownstreamTypeConnection().get(oldOpIndex));
		// necessary to ignore state checkpoints of the old operator before split.
		long last_ts = oldConnection.getLast_ts();
		oldConnection.setReconf_ts(last_ts);
		/** END BLOCK OF CODE **/
				
		//Stop connections to perform the update
		NodeManager.nLogger.info("-> Stopping connections of oldOpId: "+oldOpId+" and newOpId: "+newOpId);
		pu.stopConnection(oldOpId);
		pu.stopConnection(newOpId);

		newKey = pu.getMostDownstream().getRouter().newOperatorPartition(oldOpId, newOpId, oldOpIndex, newOpIndex);
		return newKey;
		
	}
	
	private void backupRoutingInformation(int oldOpId) {
		//Get routing information of the operator that has scaled out
		ArrayList<Integer> indexes = pu.getRouterIndexesInformation(oldOpId);
		ArrayList<Integer> keys = pu.getRouterKeysInformation(oldOpId);
System.out.println("BACKUP INDEXES: "+indexes.toString());
System.out.println("BACKUP KEYS: "+keys.toString());

		//Create message
System.out.println("REGISTERED CLASS: "+pu.getMostDownstream().getClass().getName());
		ControlTuple msg = new ControlTuple().makeBackupRI(owner.getNodeDescr().getNodeId(), indexes, keys, pu.getMostDownstream().getClass().getName());
		//Send message to downstreams (for now, all downstreams)
		/// \todo{make this more efficient, not sending to all (same mechanism upstreamIndexBackup than downstreamIndexBackup?)}
		for(Integer index : indexes){
			owner.getControlDispatcher().sendDownstream(msg, index);
		}
	}
	
	public void replayState(int opId) {
		//Get channel information
		CommunicationChannel cci = puCtx.getCCIfromOpId(opId, "d");
		Buffer buffer = cci.getBuffer();
		Socket controlDownstreamSocket = cci.getDownstreamControlSocket();

		//Get a proper init state and just send it
		BackupNodeState bs = buffer.getBackupState();
		BackupOperatorState[] backupOperatorState = bs.getBackupOperatorState();
//		ControlTuple ct = new ControlTuple().makeInitState(owner.getNodeDescr().getNodeId(), bs.getTs_e(), bs.getState());
		InitOperatorState[] initOperatorState = new InitOperatorState[backupOperatorState.length];
		for(int i = 0; i< initOperatorState.length; i++){
			BackupOperatorState current = backupOperatorState[i];
			// Create initOperatorState with the id of the operator that will own the state and the state itself
			initOperatorState[i] = new InitOperatorState(current.getState().getOwnerId(), current.getState());
		}
		ControlTuple ct = new ControlTuple().makeInitNodeState(pu.getMostDownstream().getOperatorId(), owner.getNodeDescr().getNodeId(), initOperatorState);		
		try {
			owner.getControlDispatcher().initStateMessage(ct, controlDownstreamSocket.getOutputStream());
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void processInitState(InitNodeState ct){
		//Reconfigure backup stream index
		//Pick one of the opIds of the message
		int opId = ct.getSenderOperatorId();
		owner.manageBackupUpstreamIndex(opId);
		//Clean the data processing channel from remaining tuples in old batch
		NodeManager.nLogger.info("Changing to INITIALISING STATE, stopping all incoming comm");
		pu.setSystemStatus(ProcessingUnit.SystemStatus.INITIALISING_STATE);
//		pu.cleanInputQueue();
		//Give state to processing unit for it to manage it
		pu.installState(ct.getInitOperatorState());
		//Once the state has been installed, recover dataProcessingChannel
		
		//Send a msg to ask for the rest of information. (tuple replaying)
		NodeManager.nLogger.info("-> Sending STATE_ACK");
		ControlTuple rb = new ControlTuple().makeStateAck(owner.getNodeDescr().getNodeId(), pu.getMostUpstream().getOperatorId());
		owner.getControlDispatcher().sendAllUpstreams(rb);
//		pu.cleanInputQueue();
		pu.setSystemStatus(ProcessingUnit.SystemStatus.NORMAL);
		System.out.println("Changing to NORMAL mode, recovering data processing");
	}
	
	
	//This method simply backups its state. It is useful for making the upstream know that is in charge of managing this state.
	public void sendInitialStateBackup(){
		//Without waiting for the counter, we backup the state right now, (in case operator is stateful)
		if(pu.isNodeStateful()){
		//if(owner.subclassOperator instanceof StatefulOperator){
System.out.println("NODE: "+owner.getNodeDescr().getNodeId()+" INITIAL BACKUP!!!!!!#############");
//			((StatefulOperator)owner.subclassOperator).generateBackupState();
			pu.checkpointAndBackupState();
		}
	}
}