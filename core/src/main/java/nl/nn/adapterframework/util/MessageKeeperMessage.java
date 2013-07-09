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
/**
 * A message for the MessageKeeper. <br/>
 * Although this could be an inner class of the MessageKeeper,
 * it's made "standalone" to provide the use of iterators and
 * enumerators with the MessageKeeper.
 * @version $Id$
 * @author Johan Verrips IOS
 */
public class MessageKeeperMessage {
	public static final String version="$RCSfile: MessageKeeperMessage.java,v $ $Revision: 1.8 $ $Date: 2013-03-13 14:37:58 $";

	public static final String INFO_LEVEL = "INFO";
	public static final String WARN_LEVEL = "WARN";
	public static final String ERROR_LEVEL = "ERROR";
	
	private Date messageDate=new Date();
	private String messageText;
	private String messageLevel;
	
	/**
	* Set the messagetext of this message. The text will be xml-encoded.
	*/
	public MessageKeeperMessage(String message, String level){
	//	this.messageText=XmlUtils.encodeChars(message);
		this.messageText=message;
		this.messageLevel=level;
	}
	/**
	* Set the messagetext and -date of this message. The text will be xml-encoded.
	*/
	public MessageKeeperMessage(String message, Date date, String level) {
	//	this.messageText=XmlUtils.encodeChars(message);
		this.messageText=message;
		this.messageDate=date;
		this.messageLevel=level;
	}
	public Date getMessageDate() {
		return messageDate;
	}
	public String getMessageText() {
		return messageText;
	}
	public String getMessageLevel() {
		return messageLevel;
	}
}
