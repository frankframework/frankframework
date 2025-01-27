/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.senders;

import jakarta.annotation.Nonnull;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.stream.Message;

/**
 * Sender that sleeps for a specified time, which defaults to 5000 msecs.
 * Useful for testing purposes.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@Category(Category.Type.ADVANCED)
public class DelaySender extends AbstractSender {

	private long delayTime=5000;

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		try {
			log.info("starts waiting for {} ms.", getDelayTime());
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			throw new SenderException("delay interrupted", e);
		}
		log.info("ends waiting for {} ms.", getDelayTime());
		return new SenderResult(message);
	}

	/**
	 * The time <i>in milliseconds</i> the thread will be put to sleep
	 * @ff.default 5000 [ms]
	 */
	public void setDelayTime(long l) {
		delayTime = l;
	}
	public long getDelayTime() {
		return delayTime;
	}

}
