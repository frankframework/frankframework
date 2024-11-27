/*
   Copyright 2019, 2020 Nationale-Nederlanden

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
package org.frankframework.pipes;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.ProcessMetrics;

/**
 * Pipe that returns metrics about current memory usage
 *
 * 	Output example:
 *
 * 	<processMetrics>
 * 		<properties>
 * 			<property name="freeMemory">201M</property>
 * 			<property name="totalMemory">245M</property>
 * 			<property name="heapSize">43M</property>
 * 			<property name="maxMemory">480M</property>
 * 			<property name="currentTime">2018-12-24 14:27:29.730</property>
 * 		</properties>
 * 	</processMetrics>
 *
 * @author	Laurens Mäkel
 */

public class IbisMetricsPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		return new PipeRunResult(getSuccessForward(), ProcessMetrics.toXml());
	}

}
