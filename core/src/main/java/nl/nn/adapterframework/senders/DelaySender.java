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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;

/**
 * Sender that sleeps for a specified time, which defaults to 5000 msecs.
 * Useful for testing purposes.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class DelaySender extends SenderBase {

	private long delayTime=5000;

	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {
		try {
			log.info(getLogPrefix()+"starts waiting for " + getDelayTime() + " ms.");
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"delay interrupted", e);
		}
		log.info(getLogPrefix()+"ends waiting for " + getDelayTime() + " ms.");
		return message;
	}

	/**
	 * the time the thread will be put to sleep.
	 */
	@IbisDoc({"the time the thread will be put to sleep", "5000 [ms]"})
	public void setDelayTime(long l) {
		delayTime = l;
	}
	public long getDelayTime() {
		return delayTime;
	}

}
