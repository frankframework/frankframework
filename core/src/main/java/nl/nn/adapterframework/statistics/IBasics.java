package nl.nn.adapterframework.statistics;

public interface IBasics<D extends IBasics<D>> extends ItemList {
	public static final int NUM_BASIC_ITEMS=6;   

	public void mark(D other);
	
	public void addValue(long value);
	public void checkMinMax(long value);

	
	public long getCount();
	public long getIntervalCount(D mark);

	public long getMax();

	public long getMin();

	public long getSum();
	public long getSumOfSquares();

	public long getIntervalSum(D mark);
	public long getIntervalSumOfSquares(D mark);

	public double getAverage();
	public double getIntervalAverage(D mark);

	public double getVariance();
	public double getIntervalVariance(D mark);

	public double getStdDev();

}
