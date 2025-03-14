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
package org.frankframework.pipes;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.stream.Message;

/**
 * Pipe that sleeps for a specified time, which defaults to 5000 msecs. 
 * It is useful for testing purposes.
 *
 * @author L180564 - Johan Verrips
 */
@Category(Category.Type.BASIC)
public class DelayPipe extends FixedForwardPipe {

	private long delayTime=5000;

	@Override
	public PipeRunResult doPipe (Message message, PipeLineSession session) throws PipeRunException {
		try {
			log.info("starts waiting for {} ms.", getDelayTime());
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			throw new PipeRunException(this, "delay interrupted", e);
		}
		log.info("ends waiting for {} ms.", getDelayTime());
		return new PipeRunResult(getSuccessForward(),message);
	}


	/**
	 * The time <i>in milliseconds</i> that the thread will be put to sleep. 
	 * @ff.default 5000
	 */
	public void setDelayTime(long l) {
		delayTime = l;
	}
	public long getDelayTime() {
		return delayTime;
	}


}
