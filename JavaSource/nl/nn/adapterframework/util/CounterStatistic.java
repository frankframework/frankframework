/*
 * $Log: CounterStatistic.java,v $
 * Revision 1.2  2008-09-22 13:25:45  europe\L190409
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
		if (action==HasStatistics.STATISTICS_ACTION_NONE) {
			return;
		}
		if (action==HasStatistics.STATISTICS_ACTION_RESET) {
			clear();
		}
		if (action==HasStatistics.STATISTICS_ACTION_MARK) {
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
