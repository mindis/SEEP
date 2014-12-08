import java.util.HashMap;
import java.util.Map;

import uk.ac.imperial.lsds.seep.multi.IMicroOperatorCode;
import uk.ac.imperial.lsds.seep.multi.IQueryBuffer;
import uk.ac.imperial.lsds.seep.multi.ITupleSchema;
import uk.ac.imperial.lsds.seep.multi.IWindowAPI;
import uk.ac.imperial.lsds.seep.multi.WindowBatch;
import uk.ac.imperial.lsds.streamsql.expressions.Expression;
import uk.ac.imperial.lsds.streamsql.expressions.ExpressionsUtil;
import uk.ac.imperial.lsds.streamsql.expressions.eint.IntColumnReference;
import uk.ac.imperial.lsds.streamsql.expressions.elong.LongColumnReference;

public class LRBQ2MicroOpCode implements IMicroOperatorCode {

	private IntColumnReference vehicleAttribute;

	private ITupleSchema outSchema;
	private int byteSizeOfOutTuple;

	public LRBQ2MicroOpCode() {
		this.vehicleAttribute = new IntColumnReference(3);

		Expression[] tmpAllOutAttributes = new Expression[4];
		tmpAllOutAttributes[0] = new LongColumnReference(0);
		tmpAllOutAttributes[1] = new IntColumnReference(1);
		tmpAllOutAttributes[2] = new IntColumnReference(2);
		tmpAllOutAttributes[3] = new IntColumnReference(3);

		this.outSchema = ExpressionsUtil
				.getTupleSchemaForExpressions(tmpAllOutAttributes);
		this.byteSizeOfOutTuple = outSchema.getByteSizeOfTuple();
	}

	@Override
	public void processData(WindowBatch windowBatch, IWindowAPI api) {

		int[] startPointers = windowBatch.getWindowStartPointers();
		int[] endPointers = windowBatch.getWindowEndPointers();

		IQueryBuffer inBuffer = windowBatch.getBuffer();
		ITupleSchema inSchema = windowBatch.getSchema();
		int byteSizeOfInTuple = inSchema.getByteSizeOfTuple();

		IQueryBuffer outBuffer = windowBatch.getBuffer();

		Map<Integer, Integer> lastPerVehicleInLast30Sec = new HashMap<>();

		int prevWindowStart = -1;
		int prevWindowEnd = -1;

		for (int currentWindow = 0; currentWindow < startPointers.length; currentWindow++) {
			int inWindowStartOffset = startPointers[currentWindow];
			int inWindowEndOffset = endPointers[currentWindow];

			// empty window?
			if (inWindowStartOffset == -1) {
				if (prevWindowStart != -1) {
					for (int i = prevWindowStart; i < inWindowStartOffset; i += byteSizeOfInTuple) {

						int vehicleValue = vehicleAttribute.eval(inBuffer,
								inSchema, i);
						lastPerVehicleInLast30Sec.remove(vehicleValue);
					}
				}

				evaluateWindow(api, inBuffer, lastPerVehicleInLast30Sec,
						outBuffer, startPointers, endPointers, currentWindow);

			} else {

				/*
				 * Tuples in previous window that are not in current window
				 */
				if (prevWindowStart != -1) {
					for (int i = prevWindowStart; i < inWindowStartOffset; i += byteSizeOfInTuple) {
						int vehicleValue = vehicleAttribute.eval(inBuffer,
								inSchema, i);
						lastPerVehicleInLast30Sec.remove(vehicleValue);
					}
				}

				/*
				 * Tuples in current window that have not been in the previous
				 * window
				 */
				if (prevWindowStart != -1) {
					for (int i = prevWindowEnd; i <= inWindowEndOffset; i += byteSizeOfInTuple) {

						int vehicleValue = vehicleAttribute.eval(inBuffer,
								inSchema, i);
						lastPerVehicleInLast30Sec.put(vehicleValue, i);
					}
				} else {
					for (int i = inWindowStartOffset; i <= inWindowEndOffset; i += byteSizeOfInTuple) {

						int vehicleValue = vehicleAttribute.eval(inBuffer,
								inSchema, i);
						lastPerVehicleInLast30Sec.put(vehicleValue, i);
					}
				}

				evaluateWindow(api, inBuffer, lastPerVehicleInLast30Sec,
						outBuffer, startPointers, endPointers, currentWindow);

				prevWindowStart = inWindowStartOffset;
				prevWindowEnd = inWindowEndOffset;
			}
		}
	}

	//
	// private void evaluateWindow(IWindowAPI api) {
	//
	// MultiOpTuple[] windowResult = new
	// MultiOpTuple[this.lastPerVehicleInLast30Sec
	// .keySet().size()];
	//
	// int resultCount = 0;
	// for (Integer vehicle : this.lastPerVehicleInLast30Sec.keySet()) {
	//
	// MultiOpTuple old = this.lastPerVehicleInLast30Sec.get(vehicle);
	// MultiOpTuple t = new MultiOpTuple();
	//
	// t.values = new PrimitiveType[this.projectionIndices.length];
	// for (int i = 0; i < this.projectionIndices.length; i++)
	// t.values[i] = old.values[i];
	//
	// t.timestamp = old.timestamp;
	// t.instrumentation_ts = old.instrumentation_ts;
	//
	// windowResult[resultCount++] = t;
	// }
	//
	// api.outputWindowResult(windowResult);
	// }

	private void evaluateWindow(IWindowAPI api, IQueryBuffer inBuffer,
			Map<Integer, Integer> lastPerVehicleInLast30Sec,
			IQueryBuffer outBuffer, int[] startPointers, int[] endPointers,
			int currentWindow) {

		if (lastPerVehicleInLast30Sec.keySet().isEmpty()) {
			startPointers[currentWindow] = -1;
			endPointers[currentWindow] = -1;
		} else {
			startPointers[currentWindow] = outBuffer.position();
			for (Integer key : lastPerVehicleInLast30Sec.keySet()) {
				int partitionOffset = lastPerVehicleInLast30Sec.get(key);
				outBuffer.put(inBuffer, partitionOffset, byteSizeOfOutTuple);
			}

			endPointers[currentWindow] = outBuffer.position() - 1;
		}
	}
}