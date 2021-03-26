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

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;

import java.util.Date;
import java.util.Set;
/**
 * A message for the MessageKeeper. <br/>
 * Although this could be an inner class of the MessageKeeper,
 * it's made "standalone" to provide the use of iterators and
 * enumerators with the MessageKeeper.
 * @author Johan Verrips IOS
 */
public class MessageKeeperMessage {

	private Date messageDate=new Date();
	private String messageText;
	private MessageKeeperLevel messageLevel;
	
	/**
	* Set the messagetext of this message. The text will be xml-encoded.
	*/
	public MessageKeeperMessage(String message, MessageKeeperLevel level){
	//	this.messageText=XmlUtils.encodeChars(message);
		this.messageText=maskMessage(message);
		this.messageLevel=level;
	}
	/**
	* Set the messagetext and -date of this message. The text will be xml-encoded.
	*/
	public MessageKeeperMessage(String message, Date date, MessageKeeperLevel level) {
	//	this.messageText=XmlUtils.encodeChars(message);
		this.messageText=maskMessage(message);
		this.messageDate=date;
		this.messageLevel=level;
	}

	private String maskMessage(String message) {
		if (StringUtils.isNotEmpty(message)) {
			Set<String> hideRegex = IbisMaskingLayout.getGlobalReplace();
			message = Misc.hideAll(message, hideRegex);

			Set<String> threadHideRegex = IbisMaskingLayout.getThreadLocalReplace();
			message = Misc.hideAll(message, threadHideRegex);
		}
		return message;
	}
	
	public Date getMessageDate() {
		return messageDate;
	}
	public String getMessageText() {
		return messageText;
	}
	public String getMessageLevel() {
		return messageLevel.name();
	}
}
