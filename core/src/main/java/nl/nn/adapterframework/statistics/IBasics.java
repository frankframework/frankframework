package nl.nn.adapterframework.statistics;

public interface IBasics<S> extends ItemList {
	public static final int NUM_BASIC_ITEMS=6;   

	public S takeSnapshot();
	
	public void addValue(long value);
	public void checkMinMax(long value);

	
	public long getCount();
	public long getIntervalCount(S mark);
	public long getIntervalMin(S mark);
	public long getIntervalMax(S mark);
	public void updateIntervalMinMax(S mark, long value);
	
	public long getMax();

	public long getMin();

	public long getSum();
	public long getSumOfSquares();

	public long getIntervalSum(S mark);
	public long getIntervalSumOfSquares(S mark);

	public double getAverage();
	public double getIntervalAverage(S mark);

	public double getVariance();
	public double getIntervalVariance(S mark);

	public double getStdDev();

}
