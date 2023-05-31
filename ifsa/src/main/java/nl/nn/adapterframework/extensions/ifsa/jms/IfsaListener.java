/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package nl.nn.adapterframework.extensions.ifsa.jms;

import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ing.ifsa.IFSAHeader;
import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAPoisonMessage;
import com.ing.ifsa.IFSAServiceName;

import nl.nn.adapterframework.core.IKnowsDeliveryCount;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlEncodingUtils;

public abstract class IfsaListener extends IfsaFacade implements IListener<IFSAMessage>, IKnowsDeliveryCount<IFSAMessage> {
	public static final String THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY = "originalRawMessage";
	public static final String THREAD_CONTEXT_BIFNAME_KEY="IfsaBif";

	protected IfsaListener() {
		super(true); // Instantiate as a provider
	}


	protected String displayHeaders(IFSAMessage message) {
		StringBuilder result = new StringBuilder();
		try {
			for (Enumeration<String> enumeration = message.getPropertyNames(); enumeration.hasMoreElements(); ) {
				String tagName = enumeration.nextElement();
				Object value = message.getObjectProperty(tagName);
				result.append("\n").append(tagName).append(": ");
				if (value == null) {
					result.append("null");
				} else {
					result.append("(").append(ClassUtils.nameOf(value)).append(") [").append(value).append("]");
					if (tagName.startsWith("ifsa") &&
							!tagName.equals("ifsa_unique_id") &&
							!tagName.startsWith("ifsa_epz_") &&
							!tagName.startsWith("ifsa_udz_")) {
						result.append(" * copied when sending reply");
						if (!(value instanceof String)) {
							result.append(" THIS CAN CAUSE A PROBLEM AS ").append(ClassUtils.nameOf(value)).append(" IS NOT String!");
						}
					}
				}
			}
		} catch (Throwable t) {
			log.warn("exception parsing headers", t);
		}
		return result.toString();
	}

	protected String getIdFromWrapper(RawMessageWrapper<IFSAMessage> wrapper, Map<String,Object> threadContext)  {
		for (Iterator<String> it = wrapper.getContext().keySet().iterator(); it.hasNext();) {
			String key = it.next();
			Object value = wrapper.getContext().get(key);
			log.debug(getLogPrefix()+"setting variable ["+key+"] to ["+value+"]");
			threadContext.put(key, value);
		}
		return wrapper.getId();
	}

	/**
	 * Extracts ID-string from message obtained from raw message}.
	 * Puts also the following parameters  in the threadContext:
	 * <ul>
	 *   <li>id</li>
	 *   <li>cid</li>
	 *   <li>timestamp</li>
	 *   <li>replyTo</li>
	 *   <li>messageText</li>
	 *   <li>fullIfsaServiceName</li>
	 *   <li>ifsaServiceName</li>
	 *   <li>ifsaGroup</li>
	 *   <li>ifsaOccurrence</li>
	 *   <li>ifsaVersion</li>
	 *   <li>ifsaBifName</li>
	 *   <li>ifsaBtcData</li>
	 * </ul>
	 * @return ID-string of message for adapter.
	 */
	public String getIdFromRawMessageWrapper(RawMessageWrapper<IFSAMessage> rawMessage, Map<String,Object> threadContext) {

		if (rawMessage instanceof MessageWrapper) {
			return getIdFromWrapper(rawMessage,threadContext);
		}

		IFSAMessage message;
		try {
			message = rawMessage.getRawMessage();
		} catch (ClassCastException e) {
			log.error("{} message received was not of type IFSAMessage, but [{}]",  getLogPrefix(), rawMessage.getRawMessage().getClass().getName(), e);
			return null;
		}
		return getIdFromRawMessage(message, threadContext);
	}

	public String getIdFromRawMessage(IFSAMessage message, Map<String, Object> threadContext) {
		String mode = "unknown";
		String id = "unset";
		String cid = "unset";
		Date tsSent = null;
		Destination replyTo = null;
		String messageText = null;
		String fullIfsaServiceName = null;
		IFSAServiceName requestedService;
		String ifsaServiceName=null;
		String ifsaGroup=null;
		String ifsaOccurrence=null;
		String ifsaVersion=null;
		try {
			if (message.getJMSDeliveryMode() == DeliveryMode.NON_PERSISTENT) {
				mode = "NON_PERSISTENT";
			} else
				if (message.getJMSDeliveryMode() == DeliveryMode.PERSISTENT) {
					mode = "PERSISTENT";
				}
		} catch (JMSException ignore) {
		}
		// --------------------------
		// retrieve MessageID
		// --------------------------
		try {
			id = message.getJMSMessageID();
		} catch (JMSException ignore) {
		}
		// --------------------------
		// retrieve CorrelationID
		// --------------------------
		try {
			cid = message.getJMSCorrelationID();
		} catch (JMSException ignore) {
		}
		// --------------------------
		// retrieve TimeStamp
		// --------------------------
		try {
			long lTimeStamp = message.getJMSTimestamp();
			tsSent = new Date(lTimeStamp);

		} catch (JMSException ignore) {
		}
		// --------------------------
		// retrieve ReplyTo address
		// --------------------------
		try {
			replyTo = message.getJMSReplyTo();

		} catch (JMSException ignore) {
		}
		// --------------------------
		// retrieve message text
		// --------------------------
		try {
			messageText = ((TextMessage) message).getText();
		} catch (Throwable ignore) {
		}
		// --------------------------
		// retrieve ifsaServiceDestination
		// --------------------------
		try {
			fullIfsaServiceName = message.getServiceString();
			requestedService = message.getService();

			ifsaServiceName = requestedService.getServiceName();
			ifsaGroup = requestedService.getServiceGroup();
			ifsaOccurrence = requestedService.getServiceOccurance();
			ifsaVersion = requestedService.getServiceVersion();

		} catch (JMSException e) {
			log.error(getLogPrefix() + "got error getting serviceparameter", e);
		}

		String BIFname=null;
		try {
			BIFname= message.getBifName();
			if (StringUtils.isNotEmpty(BIFname)) {
				threadContext.put(THREAD_CONTEXT_BIFNAME_KEY,BIFname);
			}
		} catch (JMSException e) {
			log.error(getLogPrefix() + "got error getting BIFname", e);
		}
		byte btcData[]=null;
		try {
			btcData= message.getBtcData();
		} catch (JMSException e) {
			log.error(getLogPrefix() + "got error getting btcData", e);
		}

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+ "got message for [" + fullIfsaServiceName
					+ "] with JMSDeliveryMode=[" + mode
					+ "] \n  JMSMessageID=[" + id
					+ "] \n  JMSCorrelationID=["+ cid
					+ "] \n  BIFname=["+ BIFname
					+ "] \n  ifsaServiceName=["+ ifsaServiceName
					+ "] \n  ifsaGroup=["+ ifsaGroup
					+ "] \n  ifsaOccurrence=["+ ifsaOccurrence
					+ "] \n  ifsaVersion=["+ ifsaVersion
					+ "] \n  Timestamp Sent=[" + DateUtils.format(tsSent)
					+ "] \n  ReplyTo=[" + ((replyTo == null) ? "none" : replyTo.toString())
					+ "] \n  MessageHeaders=["+displayHeaders(message)+"\n"
//					+ "] \n  btcData=["+ btcData
					+ "] \n  Message=[" + message.toString()+"\n]");

		}
//		if (cid == null) {
//			if (StringUtils.isNotEmpty(BIFname)) {
//				cid = BIFname;
//				if (log.isDebugEnabled()) log.debug("Setting correlation ID to BIFname ["+cid+"]");
//			} else {
//				cid = id;
//				if (log.isDebugEnabled()) log.debug("Setting correlation ID to MessageId ["+cid+"]");
//			}
//		}

		PipeLineSession.updateListenerParameters(threadContext, id, BIFname, null, tsSent);
		threadContext.put("timestamp", tsSent);
		threadContext.put("replyTo", ((replyTo == null) ? "none" : replyTo.toString()));
		threadContext.put("messageText", messageText);
		threadContext.put("fullIfsaServiceName", fullIfsaServiceName);
		threadContext.put("ifsaServiceName", ifsaServiceName);
		threadContext.put("ifsaGroup", ifsaGroup);
		threadContext.put("ifsaOccurrence", ifsaOccurrence);
		threadContext.put("ifsaVersion", ifsaVersion);
		threadContext.put("ifsaBifName", BIFname);
		threadContext.put("ifsaBtcData", btcData);

		Map udz = message.getIncomingUDZObject();
		if (udz!=null) {
			StringBuilder contextDump = new StringBuilder("ifsaUDZ:");
			for (Iterator it = udz.keySet().iterator(); it.hasNext();) {
				String key = (String)it.next();
				String value = (String)udz.get(key);
				contextDump.append("\n ").append(key).append("=[").append(value).append("]");
				threadContext.put(key, value);
			}
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix()+ contextDump);
			}
		}

		return BIFname;
	}

	/**
	 * Extracts message string from raw message. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return input message for adapter.
	 */
	@Override
	public Message extractMessage(RawMessageWrapper<IFSAMessage> rawMessage, Map<String,Object> threadContext) throws ListenerException {
		if (rawMessage instanceof MessageWrapper) {
			return rawMessage.getMessage();
		}
		IFSAMessage ifsaMessage = rawMessage.getRawMessage();
		if (ifsaMessage instanceof IFSAPoisonMessage) {
			IFSAPoisonMessage pm = (IFSAPoisonMessage)ifsaMessage;
			IFSAHeader header = pm.getIFSAHeader();
			String source;
			try {
				source = header.getIFSA_Source();
			} catch (Exception e) {
				source = "unknown due to exeption:"+e.getMessage();
			}
			return  new Message("<poisonmessage>"+
					"  <source>"+source+"</source>"+
					"  <contents>"+ XmlEncodingUtils.encodeChars(ToStringBuilder.reflectionToString(pm))+"</contents>"+
					"</poisonmessage>");
		}

		TextMessage message = null;
		try {
			message = (TextMessage) ifsaMessage;
		} catch (ClassCastException e) {
			log.warn(getLogPrefix()+ "message received was not of type TextMessage, but ["+ifsaMessage.getClass().getName()+"]", e);
			return null;
		}
		try {
			String result=message.getText();
			threadContext.put(THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY, message);
			return new Message(result);
		} catch (JMSException e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}

	@Override
	public int getDeliveryCount(RawMessageWrapper<IFSAMessage> rawMessage) {
		try {
			javax.jms.Message message = rawMessage.getRawMessage();
			int value = message.getIntProperty("JMSXDeliveryCount");
			if (log.isDebugEnabled()) log.debug("determined delivery count ["+value+"]");
			return value;
		} catch (Exception e) {
			log.error(getLogPrefix()+"exception in determination of DeliveryCount",e);
			return -1;
		}
	}
}
