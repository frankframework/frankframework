/*
 * $Log: Browse.java,v $
 * Revision 1.8.2.2  2008-06-24 08:26:28  europe\L190409
 * sync from HEAD
 *
 * Revision 1.10  2008/06/24 08:00:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * prepare for export of messages in zipfile
 *
 * Revision 1.9  2008/06/03 15:59:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved parsing of start date
 *
 * Revision 1.8  2008/01/11 14:55:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * filter on host and type, too
 *
 * Revision 1.7  2007/12/10 10:25:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.6  2007/11/15 12:36:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * configurable order for jdbc transactional storage browsing
 * + max number of messages
 *
 * Revision 1.5  2007/10/08 12:26:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected date formatting
 *
 * Revision 1.4  2007/09/24 13:05:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CalendarParserException;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.FileViewerServlet;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Basic browser for transactional storage.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.4
 */
public class Browse extends ActionBase {
	public static final String version="$RCSfile: Browse.java,v $ $Revision: 1.8.2.2 $ $Date: 2008-06-24 08:26:28 $";

	public int maxMessages = AppConstants.getInstance().getInt("browse.messages.max",0); 

	protected void performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId, String selected[], HttpServletResponse response) {
		log.debug("performing action ["+action+"]");
	}

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);
		if (null == config) {
			return (mapping.findForward("noconfig"));
		}

		DynaActionForm browseForm = getPersistentForm(mapping, form, request);

		String submit=request.getParameter("submit");
		log.debug("submit param ["+submit+"]");

		String maxMessagesStr = getAndSetProperty(request,browseForm,"maxMessages",getMaxMessages()+"");         
		String action 		= getAndSetProperty(request,browseForm,"action");
		String storageType  = getAndSetProperty(request,browseForm,"storageType");
		String adapterName  = getAndSetProperty(request,browseForm,"adapterName");
		String receiverName = getAndSetProperty(request,browseForm,"receiverName");
		String pipeName     = getAndSetProperty(request,browseForm,"pipeName");
		String messageId    = getAndSetProperty(request,browseForm,"messageId");
		String typeMask    = getAndSetProperty(request,browseForm,"typeMask");
		String hostMask    = getAndSetProperty(request,browseForm,"hostMask");
		String currentIdMask    = getAndSetProperty(request,browseForm,"currentIdMask");
		String messageIdMask    = getAndSetProperty(request,browseForm,"messageIdMask");
		String correlationIdMask    = getAndSetProperty(request,browseForm,"correlationIdMask");
		String commentMask  = getAndSetProperty(request,browseForm,"commentMask");
		String startDateStr = getAndSetProperty(request,browseForm,"insertedAfter");
		String viewAs = getAndSetProperty(request,browseForm,"viewAs", request.getParameter("type"));
		String selected[] = (String[])browseForm.get("selected");

		if (StringUtils.isNotEmpty(submit)) {
			action=submit;
		}
		
		Date startDate=null;
		if (StringUtils.isNotEmpty(startDateStr)) {
			try {
				startDate=DateUtils.parseAnyDate(startDateStr);
				if (startDate!=null) {
					String formattedStartDate=DateUtils.formatOptimal(startDate);
					log.debug("parsed start date to ["+formattedStartDate+"]");
					browseForm.set("insertedAfter",formattedStartDate);
				} else {
					warn("could not parse date from ["+startDateStr+"]");
				}
			} catch (CalendarParserException e) {
				warn("could not parse date from ["+startDateStr+"]", e);
			}
		}
		
		ArrayList viewAsList=new ArrayList();
		viewAsList.add("html");
		viewAsList.add("text");
		browseForm.set("viewAsList", viewAsList);

		log.debug("selected ["+browseForm.get("selected")+"]");
//		ArrayList selected=(ArrayList)browseForm.get("selected");
//		for (int i=0; i<selected.size(); i++) {
//			log.debug("selected "+i+" = ["+selected.get(i));
//		}
		
		

		maxMessages=Integer.parseInt(maxMessagesStr);
		//commandIssuedBy containes information about the location the
		// command is sent from
		String commandIssuedBy= getCommandIssuedBy(request);
		log.debug("storageType ["+storageType+"] action ["+action+"] submit ["+submit+"] adapterName ["+adapterName+"] receiverName ["+receiverName+"] pipeName ["+pipeName+"] issued by ["+commandIssuedBy+"]");

		
		Adapter adapter = (Adapter) config.getRegisteredAdapter(adapterName);

		IMessageBrowser mb;
		IListener listener=null;
		if ("messagelog".equals(storageType)) {
			MessageSendingPipe pipe=(MessageSendingPipe)adapter.getPipeLine().getPipe(pipeName);
			mb=pipe.getMessageLog();	
		} else {
			ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
			mb = receiver.getErrorStorage();
			performAction(adapter, receiver, action, mb, messageId, selected, response);
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
				if (StringUtils.isEmpty(msg)) {
					msg="<no message found>";
				}
				String type=request.getParameter("type");
				if (StringUtils.isEmpty(type)) {
					type=viewAs;
				}
				FileViewerServlet.showReaderContents(new StringReader(msg),"msg"+messageId,type,response, request.getContextPath().substring(1),"message ["+messageId+"]");
				return null;
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
					for (messageCount=0; mbi.hasNext(); ) {
						Object iterItem = mbi.next();
						String cType=null;
						String cHost=null;
						if (mb instanceof ITransactionalStorage) {
							ITransactionalStorage ts =(ITransactionalStorage)mb;
							cType=ts.getTypeString(iterItem);
							cHost=ts.getHostString(iterItem);
						}
						String cId=mb.getId(iterItem);
						String cMessageId=mb.getOriginalId(iterItem);
						String cCorrelationId=mb.getCorrelationId(iterItem);
						String comment=mb.getCommentString(iterItem);
						Date insertDate=mb.getInsertDate(iterItem);
						if (StringUtils.isNotEmpty(typeMask) && !cType.startsWith(typeMask)) {
							continue;
						}
						if (StringUtils.isNotEmpty(hostMask) && !cHost.startsWith(hostMask)) {
							continue;
						}
						if (StringUtils.isNotEmpty(currentIdMask) && !cId.startsWith(currentIdMask)) {
							continue;
						}
						if (StringUtils.isNotEmpty(messageIdMask) && !cMessageId.startsWith(messageIdMask)) {
							continue;
						}
						if (StringUtils.isNotEmpty(correlationIdMask) && !cCorrelationId.startsWith(correlationIdMask)) {
							continue;
						}
						if (startDate!=null && insertDate!=null && insertDate.before(startDate)) {
							continue;
						}
						if (StringUtils.isNotEmpty(commentMask) && (StringUtils.isEmpty(comment) || comment.indexOf(commentMask)<0)) {
							continue;
						}
						XmlBuilder message=new XmlBuilder("message");
						message.addAttribute("id",mb.getId(iterItem));
						message.addAttribute("pos",Integer.toString(messageCount+1));
						message.addAttribute("originalId",mb.getOriginalId(iterItem));
						message.addAttribute("correlationId",mb.getCorrelationId(iterItem));
						if (mb instanceof ITransactionalStorage) {
							ITransactionalStorage ts = (ITransactionalStorage)mb;
							message.addAttribute("type",ts.getTypeString(iterItem));
							message.addAttribute("host",ts.getHostString(iterItem));
						}
						message.addAttribute("insertDate",DateUtils.format(mb.getInsertDate(iterItem), DateUtils.FORMAT_FULL_GENERIC));
						message.addAttribute("comment",XmlUtils.encodeChars(mb.getCommentString(iterItem)));
						messages.addSubElement(message);
						messageCount++;
						if (getMaxMessages()>0 && messageCount>=getMaxMessages()) {
							log.warn("stopped iterating messages after ["+messageCount+"]: limit reached");
							break;
						}
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

	public void setMaxMessages(int i) {
		maxMessages = i;
	}
	public int getMaxMessages() {
		return maxMessages;
	}

}
