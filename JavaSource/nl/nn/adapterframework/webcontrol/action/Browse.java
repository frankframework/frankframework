/*
 * $Log: Browse.java,v $
 * Revision 1.4  2007-09-24 13:05:02  europe\L190409
 * ability to download file, using correct filename
 *
 * Revision 1.3  2007/06/14 09:45:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * strip slash from context path
 *
 * Revision 1.2  2007/05/29 11:13:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add message logs
 *
 * Revision 1.1  2005/09/29 14:57:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * split transactional storage browser in browse-only and processing-options
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.FileViewerServlet;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Basic browser for transactional storage.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.4
 */
public class Browse extends ActionBase {
	public static final String version="$RCSfile: Browse.java,v $ $Revision: 1.4 $ $Date: 2007-09-24 13:05:02 $";

	protected void performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId) {
		// allow for extensions
	}

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);
		if (null == config) {
			return (mapping.findForward("noconfig"));
		}

		String action = request.getParameter("action");
		if (null == action) {
			action = mapping.getParameter();
		}
        
		String storageType  = request.getParameter("storageType");
		String adapterName  = request.getParameter("adapterName");
		String receiverName = request.getParameter("receiverName");
		String pipeName     = request.getParameter("pipeName");
		String messageId    = request.getParameter("messageId");

		//commandIssuedBy containes information about the location the
		// command is sent from
		String commandIssuedBy= getCommandIssuedBy(request);
		log.debug("storageType ["+storageType+"] action ["+action+"] adapterName ["+adapterName+"] receiverName ["+receiverName+"] pipeName ["+pipeName+"] issued by ["+commandIssuedBy+"]");

		
		Adapter adapter = (Adapter) config.getRegisteredAdapter(adapterName);

		IMessageBrowser mb;
		IListener listener=null;
		if ("messagelog".equals(storageType)) {
			MessageSendingPipe pipe=(MessageSendingPipe)adapter.getPipeLine().getPipe(pipeName);
			mb=pipe.getMessageLog();	
		} else {
			ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
			mb = receiver.getErrorStorage();
			performAction(adapter, receiver, action, mb, messageId);
			listener = receiver.getListener();
		}

		try {
			if ("showmessage".equalsIgnoreCase(action)) {
				Object rawmsg = mb.browseMessage(messageId);
				String msg=null;
				if (listener!=null) {
					msg = listener.getStringFromRawMessage(rawmsg,null);
				} else {
					msg=(String)rawmsg;
				}
				String type = request.getParameter("type");
				FileViewerServlet.showReaderContents(new StringReader(msg),"msg"+messageId,type,response, request.getContextPath().substring(1),"message ["+messageId+"]");
			} else {
				IMessageBrowsingIterator mbi=mb.getIterator();
				try {
					XmlBuilder messages=new XmlBuilder("messages");
					messages.addAttribute("storageType",storageType);
					messages.addAttribute("action",action);
					messages.addAttribute("adapterName",XmlUtils.encodeChars(adapterName));
					if ("messagelog".equals(storageType)) {
						messages.addAttribute("object","pipe ["+XmlUtils.encodeChars(pipeName)+"] of adapter ["+XmlUtils.encodeChars(adapterName)+"]");
						messages.addAttribute("pipeName",XmlUtils.encodeChars(pipeName));
 					} else {
						messages.addAttribute("object","receiver ["+XmlUtils.encodeChars(receiverName)+"] of adapter ["+XmlUtils.encodeChars(adapterName)+"]");
						messages.addAttribute("receiverName",XmlUtils.encodeChars(receiverName));
 					}
					int messageCount;
					for (messageCount=0; mbi.hasNext(); messageCount++) {
						Object iterItem = mbi.next();
						XmlBuilder message=new XmlBuilder("message");
						message.addAttribute("id",mb.getId(iterItem));
						message.addAttribute("pos",Integer.toString(messageCount+1));
						message.addAttribute("originalId",mb.getOriginalId(iterItem));
						message.addAttribute("correlationId",mb.getCorrelationId(iterItem));
						message.addAttribute("insertDate",DateUtils.format(mb.getInsertDate(iterItem), DateUtils.FORMAT_DATETIME_MILLISECONDS));
						message.addAttribute("comment",XmlUtils.encodeChars(mb.getCommentString(iterItem)));
						messages.addSubElement(message);
					}
					messages.addAttribute("messageCount",Integer.toString(messageCount));
					request.setAttribute("messages",messages.toXML());
				} finally {
					mbi.close();
				}
			}
		} catch (Throwable e) {
			log.error(e);
			throw new ServletException(e);
		}
		
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}		
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}

}
