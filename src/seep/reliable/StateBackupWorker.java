package seep.reliable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import seep.P;
import seep.infrastructure.NodeManager;
import seep.operator.State;
import seep.processingunit.StatefulProcessingUnit;

/**
* StateBackupWorker. This class is in charge of checking when the associated operator has a state to do backup and doing the backup of such state. This is operator dependant.
*/

public class StateBackupWorker implements Runnable, Serializable{

	private static final long serialVersionUID = 1L;
	
	private long initTime = 0;
	
	private StatefulProcessingUnit processingUnit;
	private boolean goOn = true;
	private int checkpointInterval = 0;
	private State state;
	
	// Original partitioning key
	private int[] partitioningRange = new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
	
	public void stop(){
		this.goOn = false;
	}

	public StateBackupWorker(StatefulProcessingUnit processingUnit, State s){
		this.processingUnit = processingUnit;
		this.state = s;
	}
	
	public void setPartitioningRange(ArrayList<Integer> partitioningRange) {
		this.partitioningRange[0] = partitioningRange.get(0);
		this.partitioningRange[1] = partitioningRange.get(1);
		NodeManager.nLogger.info("-> Configured new partitioning range. From: "+this.partitioningRange[0]+" to "+this.partitioningRange[1]);
	}
	
	public ArrayList<Integer> getPartitioningRange(){
		ArrayList<Integer> aux = new ArrayList<Integer>();
		aux.add(partitioningRange[0]);
		aux.add(partitioningRange[1]);
		return aux;
	}
	
	public void run(){
		initTime = System.currentTimeMillis();
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		processingUnit.checkpointAndBackupState();
		processingUnit.lockFreeParallelCheckpointAndBackupState(partitioningRange);
		checkpointInterval = state.getCheckpointInterval();
		while(goOn){
			long elapsedTime = System.currentTimeMillis() - initTime;
			if(elapsedTime > checkpointInterval){
				//synch this call
				if(P.valueFor("eftMechanismEnabled").equals("true")){
					//if not initialisin state...
					if(!processingUnit.getSystemStatus().equals(StatefulProcessingUnit.SystemStatus.INITIALISING_STATE)){
						long startCheckpoint = System.currentTimeMillis();
//						processingUnit.checkpointAndBackupState();
//						processingUnit.directCheckpointAndBackupState();
//						processingUnit.directParallelCheckpointAndBackupState();
//						processingUnit.blindCheckpointAndBackupState();
//						processingUnit.blindParallelCheckpointAndBackupState();
						
						// Blocking call
						processingUnit.getOwner().signalOpenBackupSession();
//						processingUnit.getOwner()._signalOpenBackupSession();
						
						processingUnit.lockFreeParallelCheckpointAndBackupState(partitioningRange);

						processingUnit.getOwner().signalCloseBackupSession();
//						processingUnit.getOwner()._signalCloseBackupSession();
						
//						long startGC = System.currentTimeMillis();
//						System.gc();
//						long stopGC = System.currentTimeMillis();
//						System.out.println("%% Total GC: "+(stopGC-startGC));
						
						long stopCheckpoint = System.currentTimeMillis();
						System.out.println("%% Total Checkpoint: "+(stopCheckpoint-startCheckpoint));
					}
				}
				initTime = System.currentTimeMillis();
			}
			else{
				try {
					int sleep = (int) (checkpointInterval - (System.currentTimeMillis() - initTime));
					if(sleep > 0){
						Thread.sleep(sleep);
					}
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
