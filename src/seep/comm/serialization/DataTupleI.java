package seep.comm.serialization;

public interface DataTupleI {

	/**
	 * For maximum performance we can provide direct access methods, otherwise we will always do a map lookup first
	 */
	
	public Object getValue(String attribute);
	public String getString(String attribute);
	public String[] getStringArray(String attribute);
	public Character getChar(String attribute);
	public Byte getByte(String attribute);
	public byte[] getByteArray(String attribute);
	public Integer getInt(String attribute);
	public int[] getIntArray(String attribute);
	public Short getShort(String attribute);
	public Long getLong(String attribute);
	public Float getFloat(String attribute);
	public Double getDouble(String attribute);
	public double[] getDoubleArray(String attribute);
	
}
