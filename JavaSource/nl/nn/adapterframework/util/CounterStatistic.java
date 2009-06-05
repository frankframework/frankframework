/*
 * $Log: CounterStatistic.java,v $
 * Revision 1.3  2009-06-05 07:34:17  L190409
 * support for adapter level only statistics
 *
 * Revision 1.2  2008/09/22 13:25:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed methodname
 *
 * Revision 1.1  2008/09/17 09:58:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.util;

/**
 * Counter value that is maintained with statistics.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class CounterStatistic extends Counter {

	long mark;
	
	public CounterStatistic(int startValue) {
		super(startValue);
		mark=startValue;
	}

	public void performAction(int action) {
		if (action==HasStatistics.STATISTICS_ACTION_FULL || action==HasStatistics.STATISTICS_ACTION_SUMMARY) {
			return;
		}
		if (action==HasStatistics.STATISTICS_ACTION_RESET) {
			clear();
		}
		if (action==HasStatistics.STATISTICS_ACTION_MARK_FULL || action==HasStatistics.STATISTICS_ACTION_MARK_MAIN) {
			synchronized (this) {
				mark=getValue();
			}
		}
	}

	public synchronized long getIntervalValue() {
		return getValue()-mark;
	}

	public synchronized void clear() {
		super.clear();
		mark=0;	
	}

}
