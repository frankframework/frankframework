package nl.nn.adapterframework.util;

import java.util.Date;
/**
 * A message for the MessageKeeper. <br/>
 * Although this could be an inner class of the MessageKeeper,
 * it's made "standalone" to provide the use of iterators and
 * enumerators with the MessageKeeper.
 *
 * @author Johan Verrips IOS
 */
public class MessageKeeperMessage {
	public static final String version="$Id: MessageKeeperMessage.java,v 1.1 2004-02-04 08:36:09 a1909356#db2admin Exp $";
	

	private Date messageDate=new Date();
	private String messageText;
	

public MessageKeeperMessage() {
	super();
}
/**
* Set the messagetext of this message. The text will be xml-encoded.
*/
public MessageKeeperMessage(String message){
	this.messageText=XmlUtils.encodeChars(message);
}
/**
* Set the messagetext and -date of this message. The text will be xml-encoded.
*/
public MessageKeeperMessage(String message, Date date) {
	this.messageText=XmlUtils.encodeChars(message);
	this.messageDate=date;
}
public Date getMessageDate() {
	return messageDate;
}
public String getMessageText() {
	return messageText;
}
}
