/*
 * $Log: Browse.java,v $
 * Revision 1.23  2011-03-16 16:37:32  L190409
 * use datetime parameters to improve browsing performance
 *
 * Revision 1.22  2010/01/04 15:05:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added label
 *
 * Revision 1.21  2009/12/23 17:10:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 * Revision 1.20  2009/10/26 13:53:09  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added MessageLog facility to receivers
 *
 * Revision 1.19  2009/08/04 11:46:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * work around IE 6 issue, that prevents exporting messages under HTTPS
 *
 * Revision 1.18  2009/04/28 11:36:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected dateClip handling
 *
 * Revision 1.17  2009/04/28 09:32:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added clip option to date filter
 *
 * Revision 1.16  2009/03/13 14:34:41  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added expiry date
 *
 * Revision 1.15  2009/01/02 10:27:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * export function restored (unjust removed in v4.9.3)
 *
 * Revision 1.14  2008/12/10 17:05:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in export selected messages; now a valid zip file is returned
 *
 * Revision 1.13  2008/11/06 10:23:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.12  2008/08/12 16:04:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added skipMessages
 *
 * Revision 1.11  2008/08/06 16:42:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to search in message body text
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
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
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
	public static final String version="$RCSfile: Browse.java,v $ $Revision: 1.23 $ $Date: 2011-03-16 16:37:32 $";

	private int maxMessages = AppConstants.getInstance().getInt("browse.messages.max",0); 
	private int skipMessages=0;
	
	
	// if performAction returns true, no forward should be returned
	protected boolean performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId, String selected[], HttpServletRequest request, HttpServletResponse response) {
		log.debug("performing action ["+action+"]");
		return false;
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
		String skipMessagesStr = getAndSetProperty(request,browseForm,"skipMessages","0");         
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
		String messageTextMask = getAndSetProperty(request,browseForm,"messageTextMask");
		String labelMask  = getAndSetProperty(request,browseForm,"labelMask");
		String startDateStr = getAndSetProperty(request,browseForm,"insertedAfter");
		String startDateClipStr = getAndSetProperty(request,browseForm,"insertedAfterClip");
		String endDateStr = request.getParameter("insertedBefore"); // not yet supported in actionForm
		String forceDescStr = request.getParameter("forceDescending"); // not yet supported in actionForm
		String viewAs = getAndSetProperty(request,browseForm,"viewAs", request.getParameter("type"));
		String selected[] = (String[])browseForm.get("selected");

		boolean startDateClip="on".equals(startDateClipStr);

		if (StringUtils.isNotEmpty(submit)) {
			action=submit;
		}
		
		Date startDate=null;
		Date endDate=null;
		String formattedStartDate=null;
		if (StringUtils.isNotEmpty(startDateStr)) {
			try {
				startDate=DateUtils.parseAnyDate(startDateStr);
				if (startDate!=null) {
					formattedStartDate=DateUtils.formatOptimal(startDate);
					log.debug("parsed start date to ["+formattedStartDate+"]");
					browseForm.set("insertedAfter",formattedStartDate);
					if (startDateClip) {
						endDate=DateUtils.nextHigherValue(startDate);
					}
				} else {
					warn("could not parse date from ["+startDateStr+"]");
				}
			} catch (CalendarParserException e) {
				warn("could not parse date from ["+startDateStr+"]", e);
			}
		}

		if (StringUtils.isNotEmpty(endDateStr)) {
			try {
				endDate=DateUtils.parseAnyDate(endDateStr);
				if (startDate==null) {
					warn("could not parse date from ["+endDateStr+"]");
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
		skipMessages=Integer.parseInt(skipMessagesStr);
		//commandIssuedBy containes information about the location the
		// command is sent from
		String commandIssuedBy= getCommandIssuedBy(request);
		log.debug("storageType ["+storageType+"] action ["+action+"] submit ["+submit+"] adapterName ["+adapterName+"] receiverName ["+receiverName+"] pipeName ["+pipeName+"] issued by ["+commandIssuedBy+"]");

		
		Adapter adapter = (Adapter) config.getRegisteredAdapter(adapterName);

		IMessageBrowser mb;
		IListener listener=null;
		if ("messagelog".equals(storageType)) {
			if (StringUtils.isNotEmpty(pipeName)) {
				MessageSendingPipe pipe=(MessageSendingPipe)adapter.getPipeLine().getPipe(pipeName);
				mb=pipe.getMessageLog();
			} else {
				ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
				mb = receiver.getMessageLog();
			}
			// actions 'deletemessage' and 'resendmessage' not allowed for messageLog	
			if ("export selected".equalsIgnoreCase(action)) {
				performAction(adapter, null, action, mb, messageId, selected, request, response);
			}
		} else {
			ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
			if (receiver==null) {
				error("cannot find Receiver ["+receiverName+"]", null);
				return null;
			}
			mb = receiver.getErrorStorage();
			if (performAction(adapter, receiver, action, mb, messageId, selected, request, response))
				return null;
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
				IMessageBrowsingIterator mbi=mb.getIterator(startDate,endDate,"true".equals(forceDescStr));
				try {
					XmlBuilder messages=new XmlBuilder("messages");
					messages.addAttribute("storageType",storageType);
					messages.addAttribute("action",action);
					messages.addAttribute("adapterName",XmlUtils.encodeChars(adapterName));
					if ("messagelog".equals(storageType) && StringUtils.isNotEmpty(pipeName)) {
						messages.addAttribute("object","pipe ["+XmlUtils.encodeChars(pipeName)+"] of adapter ["+XmlUtils.encodeChars(adapterName)+"]");
						messages.addAttribute("pipeName",XmlUtils.encodeChars(pipeName));
 					} else {
						messages.addAttribute("object","receiver ["+XmlUtils.encodeChars(receiverName)+"] of adapter ["+XmlUtils.encodeChars(adapterName)+"]");
						messages.addAttribute("receiverName",XmlUtils.encodeChars(receiverName));
 					}
					int messageCount;
					for (messageCount=0; mbi.hasNext(); ) {
						IMessageBrowsingIteratorItem iterItem = mbi.next();
						try {
							String cType=iterItem.getType();
							String cHost=iterItem.getHost();
							String cId=iterItem.getId();
							String cMessageId=iterItem.getOriginalId();
							String cCorrelationId=iterItem.getCorrelationId();
							String comment=iterItem.getCommentString();
							Date insertDate=iterItem.getInsertDate();
							String cLabel=iterItem.getLabel();
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
							if (startDate!=null && insertDate!=null) {
								if (insertDate.before(startDate)) {
									continue;
								}
								if (startDateClip) {
									String formattedInsertDate=DateUtils.formatOptimal(insertDate);
									if (!formattedInsertDate.startsWith(formattedStartDate)) {
										continue;
									}
								}
							}
							if (StringUtils.isNotEmpty(commentMask) && (StringUtils.isEmpty(comment) || comment.indexOf(commentMask)<0)) {
								continue;
							}
							if (StringUtils.isNotEmpty(messageTextMask)) {
								Object rawmsg = mb.browseMessage(cId);
								String msg=null;
								if (listener!=null) {
									msg = listener.getStringFromRawMessage(rawmsg,new HashMap());
								} else {
									msg=(String)rawmsg;
								}
								if (msg==null || msg.indexOf(messageTextMask)<0) {
									continue;
								}
							}
							if (StringUtils.isNotEmpty(labelMask) && (StringUtils.isEmpty(cLabel) || !cLabel.startsWith(labelMask))) {
								continue;
							}
							messageCount++;
							if (messageCount>skipMessages) { 
								XmlBuilder message=new XmlBuilder("message");
								message.addAttribute("id",cId);
								message.addAttribute("pos",Integer.toString(messageCount));
								message.addAttribute("originalId",cMessageId);
								message.addAttribute("correlationId",cCorrelationId);
								message.addAttribute("type",cType);
								message.addAttribute("host",cHost);
								message.addAttribute("insertDate",DateUtils.format(insertDate, DateUtils.FORMAT_FULL_GENERIC));
								if (iterItem.getExpiryDate()!=null) {
									message.addAttribute("expiryDate",DateUtils.format(iterItem.getExpiryDate(), DateUtils.FORMAT_FULL_GENERIC));
								}
								message.addAttribute("comment",XmlUtils.encodeChars(iterItem.getCommentString()));
								message.addAttribute("label",cLabel);
								messages.addSubElement(message);
							}

							if (getMaxMessages()>0 && messageCount>=(getMaxMessages()+skipMessages)) {
								log.warn("stopped iterating messages after ["+messageCount+"]: limit reached");
								break;
							}
						} finally {
							iterItem.release();
						}
					}
					messages.addAttribute("messageCount",Integer.toString(messageCount-skipMessages));
					request.setAttribute("messages",messages.toXML());
				} finally {
					mbi.close();
				}
			}
		} catch (Throwable e) {
			error("Caught Exception", e);
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
