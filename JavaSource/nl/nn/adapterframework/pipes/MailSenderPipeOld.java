package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;
import org.w3c.dom.Element;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;


/**
 * Previous implementation of the current {@link MailSenderPipe}, not to be used anymore.
 *
 * Sample email.xml:<br/><code><pre>
 *	&lt;email&gt;
 *	    &lt;recipients&gt;
 *	        &lt;recipient&gt;***@natned&lt;/recipient&gt;
 *	        &lt;recipient&gt;***@nn.nl&lt;/recipient&gt;
 *	    &lt;/recipients&gt;
 *	    &lt;from&gt;***@nn.nl&lt;/from&gt;
 *	    &lt;subject&gt;this is the subject&lt;/subject&gt;
 *	    &lt;message&gt;dit is de message&lt;/message&gt;
 *	&lt;/email&gt;
 * </pre></code> <br/>
 * Notice: it must be valid XML. Therefore, especially the message element
 * must be plain text or be wrapped as CDATA.<br/><br/>
 * example:<br/><code><pre>
 * &lt;message&gt;&lt;![CDATA[&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;]]&gt;&lt;/message&gt;
 * </pre></code><br/>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setSmtpHost(String) smtpHost}</td><td>name of the host by which the messages are to be send</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpUserid(String) smtpUserid}</td><td>userid on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpPassword(String) smtpPassword}</td><td>password of userid on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultFrom(String) defaultFrom}</td><td>value of the From: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultSubject(String) defaultSubject}</td><td>value of the Subject: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>

 * </table>
 * </p>
 *
 * <b>Note:</b> spelling of all occurrences of 'recepient' has been corrected into 'recipient'
 * <p>$Id: MailSenderPipeOld.java,v 1.2 2004-02-04 10:01:56 a1909356#db2admin Exp $</p>
 * @deprecated Replaced by {@link MailSenderPipe}
 * @author Johan Verrips
 */

public class MailSenderPipeOld extends FixedForwardPipe {
	private String smtpHost;
	private Session session;
	private Transport transport;
	private String smtpUserid;
	private String smtpPassword;
	private Properties properties;


	
	// defaults
	private String defaultSubject;
	private String defaultFrom;


public void configure() throws ConfigurationException{
	super.configure();
	properties = System.getProperties();
	properties.put("mail.smtp.host",smtpHost);

}
/**
 * Send a mail conforming to the XML input
 */
public PipeRunResult doPipe(Object input) throws PipeRunException {
	// initialize this request
	String from=defaultFrom;
	String subject=defaultSubject;
	Vector recipients=new Vector();
	StringBuffer sb=new StringBuffer();
	String message;
		
    Element emailElement;
    try {
        emailElement = XmlUtils.buildElement((String) input);

        subject = XmlUtils.getChildTagAsString(emailElement, "subject");
        message = XmlUtils.getChildTagAsString(emailElement, "message");
        if (null != XmlUtils.getChildTagAsString(emailElement, "from"))
            from = XmlUtils.getChildTagAsString(emailElement, "from");

        Element recipientsElement =
            XmlUtils.getFirstChildTag(emailElement, "recipients");
        Collection c = XmlUtils.getChildTags(recipientsElement, "recipient");
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            Element el = (Element) iter.next();
            recipients.add(XmlUtils.getStringValue(el));

        }

    } catch (DomBuilderException e) {
        throw new PipeRunException(this, "excption parsing ["+(String)input+"]",e);
    }

    if (recipients.size()==0) throw new PipeRunException(this, "["+getName()+"] no one to the message to: no recipients defined");
	try {
		// construct a message  
		MimeMessage msg = new MimeMessage(session);

		msg.setFrom(new InternetAddress(from));

		if (log.isDebugEnabled()){
			sb.append("Pipe ["+getName()+"] sent message ");
			sb.append("[smtpHost="+smtpHost);
			sb.append("[from="+from+"]");
			sb.append("[subject="+subject+"]");
			sb.append("[text="+message+"]");
				
		}

		
		Iterator recit=recipients.iterator();
		while (recit.hasNext()){
			String recipient=(String)recit.next();
			msg.addRecipient(Message.RecipientType.TO,new InternetAddress(recipient));
			if (log.isDebugEnabled()) {
				sb.append("[recipient="+recipient+"]");
			}
		}
		msg.setSubject(subject); 
		msg.setText(message);

		// send the message
		transport.sendMessage(msg,msg.getAllRecipients());
		
		log.debug(sb.toString());
		
	}catch(Exception e){
		log.error("Pipe ["+getName()+"] got error", e);
		throw new PipeRunException(this, "Pipe ["+getName()+"] got error", e);
	
	}
	return  new PipeRunResult(getForward(), message);
	
}
/**
 * name of the SMTP Host
 */
public String getSmtpHost() {
	return smtpHost;
}
/**
 * password for the SMTP Host<br/>
 * Creation date: (01-04-2003 12:43:21)
 * @return java.lang.String
 */
public String getSmtpPassword() {
	return smtpPassword;
}
/**
 * UserID for the SMTP Host 
 */
public String getSmtpUserid() {
	return smtpUserid;
}
/**
 * Set the default for From
 */
public void setDefaultFrom(String newFrom) {
	defaultFrom = newFrom;
}
/**
 * Set the default for Subject>
 */
public void setDefaultSubject(String newSubject) {
	defaultSubject = newSubject;
}
/**
 * Set the name of the SMTP Host 
 */
public void setSmtpHost(String newSmtpHost) {
	smtpHost = newSmtpHost;
}
/**
 * Set the password of the SMTP host
 */
public void setSmtpPassword(String newSmtpPassword) {
	smtpPassword = newSmtpPassword;
}
/**
 * Set the userid of the SMTP Host
 */
public void setSmtpUserid(java.lang.String newSmtpUserid) {
	smtpUserid = newSmtpUserid;
}
/**
 * Create a <code>Session</code> and <code>Transport</code> to the
 * smtp host.
  * @throws PipeStartException
 */
public void start() throws PipeStartException{
	try {
		session = Session.getInstance(properties,null);
		log.debug("Pipe ["+getName()+"] got session to ["+properties+"]");

		// connect to the transport 
		transport = session.getTransport("smtp");
		
		transport.connect(smtpHost,smtpUserid, smtpPassword);
		if (log.isDebugEnabled()){
			log.debug("Pipe ["+getName()+"] got transport to [smtpHost="+smtpHost+"]"+
				" [smtpUserid="+smtpUserid+
				" [smtpPassword="+smtpPassword+"]");
		}

	} catch (Exception e){
		log.error("["+getName()+"] error : "+e.getMessage());
		throw new PipeStartException(e);
	}

}
/**
 * Close the <code>transport</code> layer.
 */
public void stop(){
	try {
		transport.close();
	} catch (Exception e) {
		log.warn("Pipe ["+getName()+"] got error closing transport: ",e);
	}

}
}
