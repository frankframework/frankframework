/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.statistics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * Counter value that is maintained with statistics.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class CounterStatistic extends ScalarMetricBase<Counter> {

	long mark;

	public CounterStatistic(int startValue) {
		mark=startValue;
	}

	@Override
	public void initMetrics(MeterRegistry registry, String groupName, Iterable<Tag> tags, String scalarname) {
		meter = Counter.builder(groupName+"."+scalarname).tags(tags).register(registry);
		meter.increment(mark);
	}

	public void performAction(HasStatistics.Action action) {
		switch (action) {
		case FULL:
		case SUMMARY:
			return;
		case MARK_FULL:
		case MARK_MAIN:
			synchronized (this) {
				mark=getValue();
			}
			return;
		default:
			throw new IllegalArgumentException("unknown Action ["+action+"]");
		}
	}

	public synchronized void increase() {
		meter.increment();
	}

	@Override
	public long getValue() {
		if (meter==null) return 0;
		return (long)meter.count();
	}

	public synchronized long getIntervalValue() {
		return getValue()-mark;
	}

}
