/*
 * $Log: Browse.java,v $
 * Revision 1.1  2005-09-29 14:57:11  europe\L190409
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
	public static final String version="$RCSfile: Browse.java,v $ $Revision: 1.1 $ $Date: 2005-09-29 14:57:11 $";

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
		if (null == action)
			action = mapping.getParameter();
        
		String adapterName = request.getParameter("adapterName");
		String receiverName = request.getParameter("receiverName");
		String messageId = request.getParameter("messageId");

		//commandIssuedBy containes information about the location the
		// command is sent from
		String commandIssuedBy= getCommandIssuedBy(request);
		log.debug("action ["+action+"] adapterName ["+adapterName+"] receiverName ["+receiverName+"] issued by "+commandIssuedBy);

		
		Adapter adapter = (Adapter) config.getRegisteredAdapter(adapterName);
		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);

		IMessageBrowser mb = receiver.getErrorStorage();
		

		performAction(adapter, receiver, action, mb, messageId);
		

		try {
			if ("showmessage".equalsIgnoreCase(action)) {
				Object rawmsg = mb.browseMessage(messageId);
				IListener listener = receiver.getListener();
				String msg = listener.getStringFromRawMessage(rawmsg,null);
				String type = request.getParameter("type");
				FileViewerServlet.showReaderContents(new StringReader(msg),type,response);
			} else {
				IMessageBrowsingIterator mbi=mb.getIterator();
				try {
					XmlBuilder messages=new XmlBuilder("messages");
					messages.addAttribute("adapterName",java.net.URLEncoder.encode(adapterName));
					messages.addAttribute("receiverName",java.net.URLEncoder.encode(receiverName));
					messages.addAttribute("title","Errorqueue of receiver ["+receiverName+"] of adapter ["+adapterName+"]");
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
