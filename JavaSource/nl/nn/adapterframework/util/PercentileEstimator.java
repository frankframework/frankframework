/*
 * $Log: PercentileEstimator.java,v $
 * Revision 1.1  2005-02-02 16:30:07  L190409
 * modular percentile estimation
 *
 */
package nl.nn.adapterframework.util;

/**
 * Interface to define estimators for percentiles.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public interface PercentileEstimator {
	
	int getNumPercentiles();
	int getPercentage(int index);
	void addValue(long value, long count, long min, long max);
	double getPercentileEstimate(int index, long count);
	
}
