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
/*
 * $Log: MessageKeeper.java,v $
 * Revision 1.7  2013-03-13 14:37:31  europe\m168309
 * added level (INFO, WARN or ERROR) to adapter/receiver messages
 *
 * Revision 1.6  2011/11/30 13:51:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2009/08/26 15:37:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed threading problem
 *
 */
package nl.nn.adapterframework.util;

import java.util.Date;

/**
 * Keeps a list of <code>MessageKeeperMessage</code>s.
 * <br/>
 * @version $Id$
 * @author  Johan Verrips IOS
 * @see MessageKeeperMessage
 */
public class MessageKeeper extends SizeLimitedVector {

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
	 * @return MessageKeeperMessage the Message
	 * @see MessageKeeperMessage
	 */
	public MessageKeeperMessage getMessage(int i) {
		return (MessageKeeperMessage)super.get(i);
	}
}
