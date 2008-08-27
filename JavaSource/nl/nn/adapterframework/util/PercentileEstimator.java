/*
 * $Log: PercentileEstimator.java,v $
 * Revision 1.4  2008-08-27 16:25:16  europe\L190409
 * added clear()
 *
 * Revision 1.3  2006/09/07 08:37:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added sample return facility
 *
 * Revision 1.2  2005/03/10 09:52:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked percentile estimation
 *
 * Revision 1.1  2005/02/02 16:30:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	double getPercentileEstimate(int index, long count, long min, long max);

	int getSampleCount(long count, long min, long max);
	XmlBuilder getSample(int index, long count, long min, long max);
	
	public void clear();

}
