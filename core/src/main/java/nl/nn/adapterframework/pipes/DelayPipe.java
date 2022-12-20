/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.stream.Message;

/**
 * Pipe that sleeps for a specified time, which defaults to 5000 msecs.
 * Usefull for testing purposes.
 *
 * @author L180564 - Johan Verrips
 */
@Category("Basic")
public class DelayPipe extends FixedForwardPipe {

	private long delayTime=5000;

	@Override
	public PipeRunResult doPipe (Message message, PipeLineSession session) throws PipeRunException {
		try {
			log.info("starts waiting for " + getDelayTime() + " ms.");
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			throw new PipeRunException(this, "delay interrupted", e);
		}
		log.info("ends waiting for " + getDelayTime() + " ms.");
		return new PipeRunResult(getSuccessForward(),message);
	}


	/**
	 * The time <i>in milliseconds</i> the thread will be put to sleep
	 * @ff.default 5000
	 */
	public void setDelayTime(long l) {
		delayTime = l;
	}
	public long getDelayTime() {
		return delayTime;
	}


}
