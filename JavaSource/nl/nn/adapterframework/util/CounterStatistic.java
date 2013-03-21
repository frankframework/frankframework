/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.util;

import nl.nn.adapterframework.statistics.HasStatistics;

/**
 * Counter value that is maintained with statistics.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
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
