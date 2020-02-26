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

import java.util.Date;

import org.apache.logging.log4j.Logger;

/**
 * Keeps a list of <code>MessageKeeperMessage</code>s.
 * <br/>
 * @author  Johan Verrips IOS
 * @see MessageKeeperMessage
 */
public class MessageKeeper extends SizeLimitedVector {
	protected Logger log = LogUtil.getLogger(this);

	public MessageKeeper() {
		super();
	}

	public MessageKeeper(int maxSize) {
		super(maxSize);
	}
	
	public synchronized void add(String message) {
		add(message, MessageKeeperMessage.INFO_LEVEL);
	}
	public synchronized void add(String message, String level) {
		super.add(new MessageKeeperMessage(message, level));
	}
	public synchronized void add(String message, Date date) {
		add(message, date, MessageKeeperMessage.INFO_LEVEL);
	}
	public synchronized void add(String message, Date date, String level) {
		super.add(new MessageKeeperMessage(message, date, level));
	}
	/**
	 * Get a message by number
	 * @see MessageKeeperMessage
	 */
	public MessageKeeperMessage getMessage(int i) {
		return (MessageKeeperMessage)super.get(i);
	}

	/**
	 * Add an error message to the {@link #MessageKeeper} and log it as a warning
	 */
	public void add(String message, Throwable t) {
		String msgToLog = message;
		if(t.getMessage() != null) {
			msgToLog += ": "+t.getMessage();
		}
		add(msgToLog);
		log.warn(msgToLog, t);
	}
}
