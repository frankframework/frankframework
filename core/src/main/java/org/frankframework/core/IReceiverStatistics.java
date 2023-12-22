/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package org.frankframework.core;

import org.frankframework.statistics.StatisticsKeeper;
/**
 * Methods for Receivers to supply statistics to a maintenance clients. Receivers indicate
 * by implementing this interface that process- and idle statistics may be available for
 * displaying.
 *
 * @author Gerrit van Brakel
 */
public interface IReceiverStatistics  {
	/**
	 * @return an iterator of {@link StatisticsKeeper}s describing the durations of time that
	 * the receiver has been waiting between messages.
	 */
	Iterable<StatisticsKeeper> getIdleStatistics();

	/**
	 * @return an iterator of {@link StatisticsKeeper}s describing the durations of time that
	 * the receiver has been waiting for the adapter to process messages.
	 */
	Iterable<StatisticsKeeper> getProcessStatistics();
}
