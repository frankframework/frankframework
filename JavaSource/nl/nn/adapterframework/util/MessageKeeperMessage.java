/*
 * $Log: MessageKeeperMessage.java,v $
 * Revision 1.5  2005-10-18 08:17:14  europe\L190409
 * corrected version string
 * cosmetic changes
 *
 */
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
	public static final String version="$RCSfile: MessageKeeperMessage.java,v $ $Revision: 1.5 $ $Date: 2005-10-18 08:17:14 $";

	private Date messageDate=new Date();
	private String messageText;
	
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
