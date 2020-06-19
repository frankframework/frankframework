/*
   Copyright 2013, 2016 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.http.HttpUtils;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CalendarParserException;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
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
 * @author  Gerrit van Brakel
 * @since   4.4
 */
public class Browse extends ActionBase {

	private int maxMessages = AppConstants.getInstance().getInt("browse.messages.max",0); 
	private int skipMessages=0;
	
	
	// if performAction returns true, no forward should be returned
	protected boolean performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId, String selected[], HttpServletRequest request, HttpServletResponse response) {
		log.debug("performing action ["+action+"]");
		return false;
	}

	public ActionForward executeSub(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);

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
				if (endDate==null) {
					warn("could not parse date from ["+endDateStr+"]");
				}
			} catch (CalendarParserException e) {
				warn("could not parse date from ["+endDateStr+"]", e);
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
		String commandIssuedBy= HttpUtils.getCommandIssuedBy(request);
		log.debug("storageType ["+storageType+"] action ["+action+"] submit ["+submit+"] adapterName ["+adapterName+"] receiverName ["+receiverName+"] pipeName ["+pipeName+"] issued by ["+commandIssuedBy+"]");

		
		Adapter adapter = (Adapter)ibisManager.getRegisteredAdapter(adapterName);

		IMessageBrowser mb;
		IListener listener=null;
		String logCount;
		if ("messagelog".equals(storageType)) {
			if (StringUtils.isNotEmpty(pipeName)) {
				MessageSendingPipe pipe=(MessageSendingPipe)adapter.getPipeLine().getPipe(pipeName);
				mb=pipe.getMessageLog();
			} else {
				ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
				mb = receiver.getMessageLogBrowser();
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
			mb = receiver.getErrorStorageBrowser();
			if (performAction(adapter, receiver, action, mb, messageId, selected, request, response))
				return null;
			listener = receiver.getListener();
		}
		try {
			logCount = "(" + ((ITransactionalStorage) mb).getMessageCount() + ")";
		} catch (Exception e) {
			log.warn(e);
			logCount = "(?)";
		}

		try {
			if ("showmessage".equalsIgnoreCase(action)) {
				Object rawmsg = mb.browseMessage(messageId);
				String msg=null;
				if(rawmsg instanceof MessageWrapper) {
					MessageWrapper msgsgs = (MessageWrapper) rawmsg;
					msg = msgsgs.getMessage().asString();
				} else if (listener!=null) {
					msg = listener.extractMessage(rawmsg, null).asString();
				} else {
					msg = Message.asString(rawmsg);
				}
				if (StringUtils.isEmpty(msg)) {
					msg="<no message found>";
				} else {
					msg=Misc.cleanseMessage(msg, mb.getHideRegex(), mb.getHideMethod());
				}
				String type=request.getParameter("type");
				if (StringUtils.isEmpty(type)) {
					type=viewAs;
				}
				FileViewerServlet.showReaderContents(new StringReader(msg),"msg"+messageId,type,response,"message ["+messageId+"]");
				return null;
			} else {
				IMessageBrowsingIterator mbi=mb.getIterator(startDate, endDate, SortOrder.NONE);
				try {
					XmlBuilder messages=new XmlBuilder("messages");
					messages.addAttribute("storageType",storageType);
					messages.addAttribute("action",action);
					messages.addAttribute("adapterName",XmlUtils.encodeChars(adapterName));
					if ("messagelog".equals(storageType) && StringUtils.isNotEmpty(pipeName)) {
						messages.addAttribute("object","pipe ["+XmlUtils.encodeChars(pipeName)+"] of adapter ["+XmlUtils.encodeChars(adapterName)+"] "+logCount);
						messages.addAttribute("pipeName",XmlUtils.encodeChars(pipeName));
 					} else {
						messages.addAttribute("object","receiver ["+XmlUtils.encodeChars(receiverName)+"] of adapter ["+XmlUtils.encodeChars(adapterName)+"] "+logCount);
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
									msg = listener.extractMessage(rawmsg,new HashMap()).asString();
								} else {
									msg = Message.asString(rawmsg);
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
