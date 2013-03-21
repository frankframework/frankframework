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
package nl.nn.adapterframework.statistics;

import nl.nn.adapterframework.core.SenderException;

/**
 * Interface to be implemented by objects like Pipes or Senders that maintain additional statistics themselves.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public interface HasStatistics {

	public static final int STATISTICS_ACTION_SUMMARY=0;
	public static final int STATISTICS_ACTION_FULL=1;
	public static final int STATISTICS_ACTION_RESET=2;
	public static final int STATISTICS_ACTION_MARK_MAIN=3;
	public static final int STATISTICS_ACTION_MARK_FULL=4;

	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException ;
}
