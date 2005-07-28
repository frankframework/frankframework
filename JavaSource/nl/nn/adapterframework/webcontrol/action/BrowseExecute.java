/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.2  2005-07-28 07:44:52  europe\L190409
 * rework using primitive types only for parameters
 *
 * Revision 1.1  2005/07/19 15:29:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of BrowseExcecute
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.FileViewerServlet;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class BrowseExecute extends ActionBase {
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.2 $ $Date: 2005-07-28 07:44:52 $";

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
		


		if ("deletemessage".equalsIgnoreCase(action)) {
			if (StringUtils.isNotEmpty(messageId)) {
				try {
					UserTransaction utx = adapter.getUserTransaction();
					utx.begin();
					mb.deleteMessage(messageId);
					utx.commit();
				} catch (Throwable e) {
					log.error(e);
					errors.add("",
						new ActionError(
							"errors.generic",
							"error occured deleting message:" + e.getMessage()));
				}
			}
		} 

		if ("resendmessage".equalsIgnoreCase(action)) {
			if (StringUtils.isNotEmpty(messageId)) {
				try {
					UserTransaction utx = adapter.getUserTransaction();
					utx.begin();
					Object msg = mb.getMessage(messageId);
					if (msg==null) {
						errors.add("",
							new ActionError(
								"errors.generic",
								"did not retrieve message" ));
						utx.rollback();
					} else {
						if (adapter.getRunState()!=RunStateEnum.STARTED) {
							errors.add("",
								new ActionError(
									"errors.generic",
									"message not processed: adapter not open" ));
							utx.rollback();
						} else {
							receiver.processRawMessage(receiver.getListener(),msg,null,-1);
						}
					}
				} catch (Throwable e) {
					log.error(e);
					errors.add("",
						new ActionError(
							"errors.generic",
							"error occured sending message:" + e.getMessage()));
				}
			}
		} 

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
		} catch (ListenerException e) {
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
