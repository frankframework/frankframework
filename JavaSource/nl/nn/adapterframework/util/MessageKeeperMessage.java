package nl.nn.adapterframework.util;

import java.util.Date;
/**
 * A message for the MessageKeeper. <br/>
 * Although this could be an inner class of the MessageKeeper,
 * it's made "standalone" to provide the use of iterators and
 * enumerators with the MessageKeeper.
 * @version Id
 * @author Johan Verrips IOS
 */
public class MessageKeeperMessage {
	public static final String version="$Id: MessageKeeperMessage.java,v 1.4 2005-10-18 06:59:49 europe\L190409 Exp $";
	

	private Date messageDate=new Date();
	private String messageText;
	

public MessageKeeperMessage() {
	super();
}
/**
* Set the messagetext of this message. The text will be xml-encoded.
*/
public MessageKeeperMessage(String message){
//	this.messageText=XmlUtils.encodeChars(message);
	this.messageText=message;
}
/**
* Set the messagetext and -date of this message. The text will be xml-encoded.
*/
public MessageKeeperMessage(String message, Date date) {
//	this.messageText=XmlUtils.encodeChars(message);
	this.messageText=message;
	this.messageDate=date;
}
public Date getMessageDate() {
	return messageDate;
}
public String getMessageText() {
	return messageText;
}
}
