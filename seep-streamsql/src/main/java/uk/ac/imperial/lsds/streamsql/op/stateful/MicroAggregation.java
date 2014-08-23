package uk.ac.imperial.lsds.streamsql.op.stateful;

import java.util.HashMap;
import java.util.Map;

import uk.ac.imperial.lsds.seep.operator.compose.micro.IMicroOperatorCode;
import uk.ac.imperial.lsds.seep.operator.compose.micro.IStatefulMicroOperator;
import uk.ac.imperial.lsds.seep.operator.compose.multi.MultiOpTuple;
import uk.ac.imperial.lsds.seep.operator.compose.window.IMicroIncrementalComputation;
import uk.ac.imperial.lsds.seep.operator.compose.window.IWindowAPI;
import uk.ac.imperial.lsds.seep.operator.compose.window.IWindowBatch;
import uk.ac.imperial.lsds.streamsql.op.IStreamSQLOperator;
import uk.ac.imperial.lsds.streamsql.op.stateful.MicroAggregation.AggregationType;
import uk.ac.imperial.lsds.streamsql.op.stateless.Selection;
import uk.ac.imperial.lsds.streamsql.types.FloatType;
import uk.ac.imperial.lsds.streamsql.types.PrimitiveType;
import uk.ac.imperial.lsds.streamsql.visitors.OperatorVisitor;

public class MicroAggregation implements IStreamSQLOperator, IMicroOperatorCode, IMicroIncrementalComputation, IStatefulMicroOperator {

	private int[] groupByAttributes;
	private PrimitiveType[] typesGroupByAttributes = null;

	private int aggregationAttribute;
			
	private AggregationType aggregationType;
	
	private Selection havingSel;
	
	/*
	 * State used for incremental computation
	 */
	private Map<Integer, PrimitiveType> values = new HashMap<>();
	private Map<Integer, Integer> countInPartition = new HashMap<>();
	private Map<Integer, MultiOpTuple> objectStore = new HashMap<>();
	
	private long lastTimestampInWindow = 0;
	private long lastInstrumentationTimestampInWindow = 0;
	
	public enum AggregationType {
		MAX, MIN, COUNT, SUM, AVG;
		
		public String asString(int s) {
			return this.toString() + "(" + s + ")";
		}
	}

	
	public MicroAggregation(AggregationType aggregationType, int aggregationAttribute) {
		this(aggregationType, aggregationAttribute, new int[0], new PrimitiveType[0], null);
	}

	public MicroAggregation(AggregationType aggregationType, int aggregationAttribute, int[] groupByAttributes, PrimitiveType[] typesGroupByAttributes, Selection havingSel) {
		this.aggregationType = aggregationType;
		this.aggregationAttribute = aggregationAttribute;
		this.groupByAttributes = groupByAttributes;
		this.typesGroupByAttributes = typesGroupByAttributes;
		this.havingSel = havingSel;
	}

	public MicroAggregation(AggregationType aggregationType, int aggregationAttribute, int[] groupByAttributes,
			PrimitiveType[] typesGroupByAttributes) {
		this(aggregationType, aggregationAttribute, groupByAttributes, typesGroupByAttributes, null);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(aggregationType.asString(aggregationAttribute) + " ");
		return sb.toString();
	}
	
	private int getGroupByKey(MultiOpTuple tuple) {
		int result = 0;
		for (int i = 0; i < this.groupByAttributes.length; i++)
			result = 37 * result + tuple.values[this.groupByAttributes[i]].hashCode();
		
		return result;
	}
	
	@Override
	public void accept(OperatorVisitor ov) {
		ov.visit(this);
	}	
	
	@Override
	public void processData(Map<Integer, IWindowBatch> windowBatches,
			IWindowAPI api) {
		
		assert(windowBatches.keySet().size() == 1);
		
		switch (aggregationType) {
		case COUNT:
		case SUM:
		case AVG:
			values = new HashMap<>();;
			countInPartition = new HashMap<>();;
			windowBatches.values().iterator().next().performIncrementalComputation(this, api);
			break;
		case MAX:
		case MIN:
			processDataPerWindow(windowBatches, api);
			break;
		default:
			break;
		}
	}

	private void processDataPerWindow(Map<Integer, IWindowBatch> windowBatches,
			IWindowAPI api) {
		
		assert(this.aggregationType.equals(AggregationType.MAX)||this.aggregationType.equals(AggregationType.MIN));
		
		long lastTimestampInWindow = 0;
		long lastInstrumentationTimestampInWindow = 0;

		IWindowBatch batch = windowBatches.values().iterator().next();
		
		int[] startPointers = batch.getWindowStartPointers();
		int[] endPointers = batch.getWindowEndPointers();
		
		for (int currentWindow = 0; currentWindow < startPointers.length; currentWindow++) {
			int windowStart = startPointers[currentWindow];
			int windowEnd = endPointers[currentWindow];
			
			// empty window?
			if (windowStart == -1) {
				api.outputWindowResult(new MultiOpTuple[0]);
			}
			else {

				Map<Integer, PrimitiveType> values = new HashMap<>();
				Map<Integer, MultiOpTuple> objects = new HashMap<>();
	
				MultiOpTuple[] windowResult = new MultiOpTuple[windowEnd-windowStart+1];
				
				for (int i = 0; i < windowEnd-windowStart+1; i++) {
					
					MultiOpTuple tuple = batch.get(windowStart + i);
					int key = getGroupByKey(tuple);
					objects.put(key, tuple);
					
					PrimitiveType newValue = (PrimitiveType) tuple.values[this.aggregationAttribute];
					
					lastTimestampInWindow = tuple.timestamp;
					lastInstrumentationTimestampInWindow = tuple.instrumentation_ts;
					
					if (values.containsKey(key)) {
						if (values.get(key).compareTo(newValue) < 0 && this.aggregationType.equals(AggregationType.MAX))
							values.put(key, newValue);
						if (values.get(key).compareTo(newValue) > 0 && this.aggregationType.equals(AggregationType.MIN))
							values.put(key, newValue);
					}
					else
						values.put(key, newValue);
				}
	
				int keyCount = 0;
				for (int partitionKey : values.keySet()) {
					MultiOpTuple tuple = prepareOutputTuple(objects.get(partitionKey), values.get(partitionKey), lastTimestampInWindow, lastInstrumentationTimestampInWindow);
					if (havingSel != null) {
						if (havingSel.getPredicate().satisfied(tuple))
							windowResult[keyCount++] = tuple;
					}
					else {
						windowResult[keyCount++] = tuple;
					}
				}
				api.outputWindowResult(windowResult);
			}			
		}
	}

	
	@Override
	public void enteredWindow(MultiOpTuple tuple) {
		assert(this.aggregationType.equals(AggregationType.COUNT)
				||this.aggregationType.equals(AggregationType.SUM)
				||this.aggregationType.equals(AggregationType.AVG));

		int key = getGroupByKey(tuple);

		lastTimestampInWindow = tuple.timestamp;
		lastInstrumentationTimestampInWindow = tuple.instrumentation_ts;
		
		PrimitiveType newValue;
		
		switch (aggregationType) {
		case COUNT:
			/*
			 * Nothing to do here, since we get the value directly from the countInPartition map
			 */
			break;
		case SUM:
		case AVG:
			newValue = (PrimitiveType) tuple.values[this.aggregationAttribute];
			if (values.containsKey(key))
				values.put(key,values.get(key).add(newValue));
			else
				values.put(key,newValue);
			
			break;
			
		default:
			break;
		}
		
		if (countInPartition.containsKey(key))
			countInPartition.put(key,countInPartition.get(key) + 1);
		else {
			countInPartition.put(key,1);
			objectStore.put(key, tuple);
		}
	}

	@Override
	public void exitedWindow(MultiOpTuple tuple) {
		
		assert(this.aggregationType.equals(AggregationType.COUNT)
				||this.aggregationType.equals(AggregationType.SUM)
				||this.aggregationType.equals(AggregationType.AVG));
		
		int key = getGroupByKey(tuple);
		
		PrimitiveType newValue;
		
		switch (aggregationType) {
		case COUNT:
			/*
			 * Nothing to do here, since we get the value directly from the countInPartition map
			 */
			break;
		case AVG:
		case SUM:
			newValue = (PrimitiveType) tuple.values[this.aggregationAttribute];
			if (values.containsKey(key)) {
				values.put(key,values.get(key).sub(newValue));
			}			
			break;
		default:
			break;
		}
		
		if (countInPartition.containsKey(key)) {
			countInPartition.put(key,countInPartition.get(key) - 1);
			if (countInPartition.get(key) <= 0) {
				countInPartition.remove(key);
				values.remove(key);
				objectStore.remove(key);
			}
		}
	}

	private MultiOpTuple prepareOutputTuple(MultiOpTuple object, PrimitiveType partitionValue, long timestamp, long instrumentation_ts) {

		Object[] values = new Object[this.groupByAttributes.length + 1];
		for (int i = 0; i < this.groupByAttributes.length; i++)
			values[i] = object.values[this.groupByAttributes[i]];
		
		values[values.length - 1] = partitionValue;
		
		return MultiOpTuple.newInstance(values, timestamp, instrumentation_ts);
	}
	
	@Override
	public void evaluateWindow(IWindowAPI api) {

		assert(this.aggregationType.equals(AggregationType.COUNT)
				||this.aggregationType.equals(AggregationType.SUM)
				||this.aggregationType.equals(AggregationType.AVG));

		MultiOpTuple[] windowResult = new MultiOpTuple[values.keySet().size()];

		switch (aggregationType) {
		case AVG:
			int keyCount = 0;
			for (int partitionKey : values.keySet()) {
				PrimitiveType partitionValue = this.values.get(partitionKey).div(new FloatType(countInPartition.get(partitionKey)));
				windowResult[keyCount++] = prepareOutputTuple(this.objectStore.get(partitionKey), partitionValue, this.lastTimestampInWindow, this.lastInstrumentationTimestampInWindow);
			}
			api.outputWindowResult(windowResult);
			break;
		case COUNT:
		case SUM:
			keyCount = 0;
			for (int partitionKey : values.keySet()) 
				windowResult[keyCount++] = prepareOutputTuple(this.objectStore.get(partitionKey), this.values.get(partitionKey), this.lastTimestampInWindow, this.lastInstrumentationTimestampInWindow);
			
			api.outputWindowResult(windowResult);
			break;
		default:
			break;
		}
	}

	@Override
	public IMicroOperatorCode getNewInstance() {
		return new MicroAggregation(this.aggregationType, this.aggregationAttribute, this.groupByAttributes, this.typesGroupByAttributes, this.havingSel);
	}

}