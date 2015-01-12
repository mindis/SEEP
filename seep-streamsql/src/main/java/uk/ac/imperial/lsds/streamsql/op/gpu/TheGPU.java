package uk.ac.imperial.lsds.streamsql.op.gpu;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class TheGPU {
	
	private static final String library = "/mnt/data/cccad3/akolious/SEEP/seep-streamsql/clib/GPU.so";
	
	private static final TheGPU instance = new TheGPU ();
	
	private static Unsafe theUnsafe;
	
	private static Unsafe getUnsafeMemory () {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			return (Unsafe) theUnsafe.get(null);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	static {
		try {
			System.load (library);
		} catch (final UnsatisfiedLinkError e) {
			System.err.println(e.getMessage());
			System.err.println("error: failed to load GPU library");
			System.exit(1);
		}
		theUnsafe = getUnsafeMemory ();
	}
	
	public static TheGPU getInstance () { return instance; }
	
	private byte [] inputArray;
	private byte [] outputArray;
	
	public void setInputBuffer (byte [] inputArray) {
		this.inputArray = inputArray;
	}
	
	public void setOutputBuffer (byte [] outputArray) {
		this.outputArray = outputArray;
	}
	
	public void inputDataMovementCallback (long inputAddr, int size) {
		theUnsafe.copyMemory (
				inputArray, 
				Unsafe.ARRAY_BYTE_BASE_OFFSET, 
				null, 
				inputAddr, 
				size
			);
	}
	
	public void outputDataMovementCallback (long outputAddr, int size, int offset) {
		theUnsafe.copyMemory(
				null, 
				outputAddr, 
				outputArray, 
				Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, 
				size
			);
	}
	
	public native int init ();
	public native int getQuery (String source, int kernels, int inputs, int outputs);
	public native int setInput (int queryId, int index, int size);
	public native int setOutput (int queryId, int index, int size);
	public native int execute (int queryId, int threads, int threadsPerGroup);
	public native int free ();
	/* Operator-specific function calls */
	public native int setKernelIdentity   (int queryId);
	public native int setKernelProjection (int queryId);
	public native int setKernelSelection  (int queryId);
	public native int setKernelReduction  (int queryId);
}
