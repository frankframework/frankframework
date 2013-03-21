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

import nl.nn.adapterframework.core.SenderException;

/**
 * Sender that sleeps for a specified time, which defaults to 5000 msecs.
 * Useful for testing purposes.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDelayTime(long) delayTime}</td><td>the time the thread will be put to sleep</td><td>5000 [ms]</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public class DelaySender extends SenderBase {

	private long delayTime=5000;


	public String sendMessage(String correlationID, String message) throws SenderException {
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
	public void setDelayTime(long l) {
		delayTime = l;
	}
	public long getDelayTime() {
		return delayTime;
	}

}
