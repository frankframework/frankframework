/*
 * $Log: SendJmsMessageExecute.java,v $
 * Revision 1.9  2009-08-31 09:48:27  m168309
 * added context facility for the JMS correlationId (in xml processing instructions)
 *
 * Revision 1.8  2009/08/26 15:50:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch TimeOutException
 *
 * Revision 1.7  2008/12/16 13:37:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * read messages in the right encoding
 *
 * Revision 1.6  2008/05/22 07:41:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use inherited error() method
 *
 * Revision 1.5  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.4  2004/03/26 10:42:58  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 16:58:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now uses unique ID for sending test-messages
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;


/**
 * Executes the sending of a message with JMS.
 * <p>
 * For setting the JMS correlationId a processing instruction with the name <code>ibiscontext</code> and key <code>tcid</code> has to be used<br/><br/>
 * example:<br/><code><pre>
 * &lt;?ibiscontext tcid=1234567890/&gt;
 * &lt;message&gt;This is a Message&lt;/message&gt;
 * </pre></code><br/>
 * 
 * @version Id
 * @author  Johan Verrips
 */
public final class SendJmsMessageExecute extends ActionBase {
	public static final String version = "$RCSfile: SendJmsMessageExecute.java,v $ $Revision: 1.9 $ $Date: 2009-08-31 09:48:27 $";
	
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	    
	
	    // Was this transaction cancelled?
	    // -------------------------------
	    if (isCancelled(request)) {
	        log.debug("sendJmsMessage was cancelled");
	        removeFormBean(mapping, request);
	        return (mapping.findForward("success"));
	    }
	
	    // Retrieve form content
	    // ---------------------
	    DynaActionForm sendJmsMessageForm = (DynaActionForm) form;
	    String form_jmsRealm=(String) sendJmsMessageForm.get("jmsRealm");
	    String form_destinationName = (String) sendJmsMessageForm.get("destinationName");
	    String form_destinationType = (String) sendJmsMessageForm.get("destinationType");
	    boolean form_persistent=false;
	    if (null!=sendJmsMessageForm.get("persistent")) {
	    	form_persistent = ((Boolean) sendJmsMessageForm.get("persistent")).booleanValue();
	    }
	    String form_message = (String) sendJmsMessageForm.get("message");
	    FormFile form_file=(FormFile) sendJmsMessageForm.get("file");
	    String form_replyToName=(String) sendJmsMessageForm.get("replyToName");
	
	
		if ((form_file!=null) && (form_file.getFileSize()>0)){
			form_message=XmlUtils.readXml(form_file.getFileData(),request.getCharacterEncoding(),false);
			log.debug("Upload of file ["+form_file.getFileName()+"] ContentType["+form_file.getContentType()+"]");
	
		} else {
			form_message=new String(form_message.getBytes(),Misc.DEFAULT_INPUT_STREAM_ENCODING);
		}
	    // initiate MessageSender
	    JmsSender qms = new JmsSender();
	    qms.setName("SendJmsMessageAction");
	    qms.setJmsRealm(form_jmsRealm);
	    qms.setDestinationName(form_destinationName);
	    qms.setPersistent(form_persistent);
	    qms.setDestinationType(form_destinationType);
	    if ((form_replyToName!=null) && (form_replyToName.length()>0))
		    qms.setReplyToName(form_replyToName);
	
		processMessage(qms, "testmsg_"+Misc.createUUID(), form_message);
	
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
		    StoreFormData(sendJmsMessageForm);
		    saveErrors(request, errors);
	        return (new ActionForward(mapping.getInput()));
	    }
	
		//Successfull: store cookie
	          String cookieValue = "";
	          cookieValue += "jmsRealm=\"" + form_jmsRealm + "\"";
	          cookieValue += " "; //separator
	          cookieValue += "destinationName=\"" + form_destinationName + "\"";
	          cookieValue += " "; //separator          
	          cookieValue += "destinationType=\"" + form_destinationType + "\"";
	          Cookie sendJmsCookie = new Cookie(AppConstants.getInstance().getProperty("WEB_JMSCOOKIE_NAME"), cookieValue);
	          sendJmsCookie.setMaxAge(Integer.MAX_VALUE);
	          log.debug("Store cookie for " + request.getServletPath()+
	          " cookieName[" + AppConstants.getInstance().getProperty("WEB_JMSCOOKIE_NAME")+"] "+
	          " cookieValue[" + new StringTagger(cookieValue).toString()+"]");
	          try {
		          response.addCookie(sendJmsCookie);
	          } catch (Throwable e) {
		          log.warn("unable to add cookie to request. cookie value ["+sendJmsCookie.getValue()+"]");
	          }
	    
	   
		
	    
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}

	private void processMessage(JmsSender qms, String messageId, String message) {
		//PipeLineSession pls=new PipeLineSession();
		Map ibisContexts = XmlUtils.getIbisContext(message);
		String technicalCorrelationId = messageId;
		if (ibisContexts!=null) {
			String contextDump = "ibisContext:";
			for (Iterator it = ibisContexts.keySet().iterator(); it.hasNext();) {
				String key = (String)it.next();
				String value = (String)ibisContexts.get(key);
				if (log.isDebugEnabled()) {
					contextDump = contextDump + "\n " + key + "=[" + value + "]";
				}
				if (key.equals("tcid")) {
					technicalCorrelationId = value;
				}
			}
			if (log.isDebugEnabled()) {
				log.debug(contextDump);
			}
		}

		try {
			qms.open();
			qms.sendMessage(technicalCorrelationId,message);
		} catch (SenderException e) {
			error("error occured sending message",e);
		} catch (TimeOutException e) {
			error("error occured sending message",e);
		}
		try {
			qms.close();
		} catch (Exception e) {
			error("error occured on closing connection",e);
		}
	}
	
	public void StoreFormData(DynaActionForm form){
	    List jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
	    if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
	    form.set("jmsRealms", jmsRealms);

	}
}
