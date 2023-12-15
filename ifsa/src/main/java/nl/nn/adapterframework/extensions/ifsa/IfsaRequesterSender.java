/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.ifsa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAReportMessage;
import com.ing.ifsa.IFSATimeOutMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.extensions.ifsa.jms.IfsaFacade;
import nl.nn.adapterframework.extensions.ifsa.jms.IfsaListener;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateFormatUtils;
import nl.nn.adapterframework.util.JtaUtil;

/**
 * {@link ISender sender} that sends a message to an IFSA service and, in case the MessageProtocol is RR (Request-Reply)
 * it waits for an reply-message.
 *
 * @ff.parameter name
 * @ff.parameter serviceId When a parameter with name serviceId is present, it is used at runtime instead of the serviceId specified by the attribute. A lookup of the service for this serviceId will be done at runtime, while the service for the serviceId specified as an attribute will be done at configuration time. Hence, IFSA configuration problems will be detected at runtime for the serviceId specified as a param and at configuration time for the serviceId specified with an attribute
 * @ff.parameter occurrence The occurrence part of the serviceId (the part between the fourth and the fifth slash). The occurrence part of the specified serviceId (either specified by a parameter or an attribute) will be replaced with the value of this parameter at runtime. From "IFSA - Naming Standards.doc": IFSA://SERVICE/&lt;group&gt;/&lt;occurrence&gt;/&lt;service&gt;/&lt;version&gt;[?&lt;parameter=value&gt;]
 * @ff.parameters All other parameters are passed to the outgoing UDZ (User Defined Zone) object
 *
 * @author  Gerrit van Brakel
 * @since   4.2, switch class: 4.8
 */
public class IfsaRequesterSender extends IfsaFacade implements ISenderWithParameters, HasStatistics {

	private boolean throwExceptions=true;
	protected String bifNameSessionKey;

	protected ParameterList paramList = null;
	private StatisticsKeeper businessProcessTimes;

	public IfsaRequesterSender() {
		super(false); // instantiate IfsaFacade as a requester
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (paramList!=null) {
			paramList.configure();
		}
//		if (isSynchronous()) {
//			businessProcessTimes = new StatisticsKeeper(getName()+"/wait for bus and provider");
//		}
		log.info(getLogPrefix()+" configured sender on "+getPhysicalDestinationName());
	}

	@Override
	public void open() throws SenderException {
		try {
			openService();
		} catch (IfsaException e) {
			throw new SenderException(getLogPrefix()+"could not start Sender", e);
		}
	}

	/**
	 * Stop the sender and deallocate resources.
	 */
	@Override
	public void close() throws SenderException {
		try {
			closeService();
		} catch (Throwable e) {
			throw new SenderException(getLogPrefix() + "got error occurred stopping sender", e);
		}
	}

	/**
	 * returns true for Request/Reply configurations
	 */
	@Override
	public boolean isSynchronous() {
		return getMessageProtocolEnum() == IfsaMessageProtocolEnum.REQUEST_REPLY;
	}

	/**
	 * Retrieves a message with the specified correlationId from queue or other channel, but does no processing on it.
	 */
	private javax.jms.Message getRawReplyMessage(QueueSession session, IFSAQueue queue, TextMessage sentMessage) throws SenderException, TimeoutException {

		String selector=null;
		javax.jms.Message msg = null;
		QueueReceiver replyReceiver=null;
		try {
			replyReceiver = getReplyReceiver(session, sentMessage);
			selector=replyReceiver.getMessageSelector();

			long timeout = getExpiry(queue);
			log.debug(getLogPrefix()+"start waiting at most ["+timeout+"] ms for reply on message using selector ["+selector+"]");
			msg = replyReceiver.receive(timeout);
			if (msg==null) {
				log.info(getLogPrefix()+"received null reply");
			} else {
				log.info(getLogPrefix()+"received reply");
			}

		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "got exception retrieving reply", e);
		} finally {
			try {
				closeReplyReceiver(replyReceiver);
			} catch (IfsaException e) {
				log.error(getLogPrefix()+"error closing replyreceiver", e);
			}
		}
		if (msg == null) {
			throw new TimeoutException(getLogPrefix() + " timed out waiting for reply using selector [" + selector + "]");
		}
		if (msg instanceof IFSATimeOutMessage) {
			throw new TimeoutException(getLogPrefix()+"received IFSATimeOutMessage waiting for reply using selector ["+selector+"]");
		}
		return msg;
//		try {
//			TextMessage result = (TextMessage)msg;
//			return result;
//		} catch (Exception e) {
//			throw new SenderException(getLogPrefix()+"reply received for message using selector ["+selector+"] cannot be cast to TextMessage ["+msg.getClass().getName()+"]",e);
//		}
	}


	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {

		try {
			if (isSynchronous()) {
				if (JtaUtil.inTransaction()) {
					throw new SenderException("cannot send RR message from within a transaction");
				}
			} else {
				if (!JtaUtil.inTransaction()) {
					log.warn("FF messages should be sent from within a transaction");
				}
			}
		} catch (Exception e) { // N.B. do not move this catch clause down; this will catch TimeOutExceptions unwantedly
			throw new SenderException(e);
		}

		ParameterValueList paramValueList=null;
		if (paramList!=null) {
			try {
				paramValueList = paramList.getValues(message, session);
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix() + "caught ParameterException in sendMessage()", e);
			}
		}

		Map<String, String> params = new HashMap<>();
		if (paramValueList != null && paramList != null) {
			for(ParameterValue pv : paramValueList) {
				String key = pv.getName();
				String value = pv.asStringValue(null);
				params.put(key, value);
			}
		}
		//IFSAMessage originatingMessage = (IFSAMessage)prc.getSession().get(PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
		String BIF = (String)session.get(getBifNameSessionKey());
		if (StringUtils.isEmpty(BIF)) {
			BIF=(String)session.get(IfsaListener.THREAD_CONTEXT_BIFNAME_KEY);
		}
		try {
			return new SenderResult(sendMessage(message.asString(), params, BIF,null));
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(),e);
		}
	}

	public String sendMessage(String dummyCorrelationId, String message, Map<String, String> params) throws SenderException, TimeoutException {
		return sendMessage(message, params, null, null);
	}

	/**
	 * Execute a request to the IFSA service.
	 * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
	 */
	public String sendMessage(String message, Map<String, String> params, String bifName, byte btcData[]) throws SenderException, TimeoutException {
		String result = null;
		QueueSession session = null;
		QueueSender sender = null;
		Map udzMap = null;

		try {
			log.debug(getLogPrefix()+"creating session and sender");
			session = createSession();
			IFSAQueue queue;
			if (params != null && params.size() > 0) {
				// Use first param as serviceId
				String serviceId = params.get("serviceId");
				if (serviceId == null) {
					serviceId = getServiceId();
				}
				String occurrence = params.get("occurrence");
				if (occurrence != null) {
					int i = serviceId.indexOf('/', serviceId.indexOf('/', serviceId.indexOf('/', serviceId.indexOf('/') + 1) + 1) + 1);
					int j = serviceId.indexOf('/', i + 1);
					serviceId = serviceId.substring(0, i + 1) + occurrence + serviceId.substring(j);
				}
				queue = getMessagingSource().lookupService(getMessagingSource().polishServiceId(serviceId));
				if (queue==null) {
					throw new SenderException(getLogPrefix()+"got null as queue for serviceId ["+serviceId+"]");
				}
				if (log.isDebugEnabled()) {
					log.info(getLogPrefix()+ "got Queue to send messages on ["+queue.getQueueName()+"]");
				}
				// Use remaining params as outgoing UDZs
				udzMap = new HashMap();
				udzMap.putAll(params);
				udzMap.remove("serviceId");
				udzMap.remove("occurrence");
			} else {
				queue = getServiceQueue();
			}
			sender = createSender(session, queue);

			log.debug(getLogPrefix()+"sending message with bifName [" + bifName + "]");

			TextMessage sentMessage=sendMessage(session, sender, message, udzMap, bifName, btcData);
			log.debug(getLogPrefix()+"message sent");

			if (isSynchronous()){

				log.debug(getLogPrefix()+"waiting for reply");
				javax.jms.Message msg=getRawReplyMessage(session, queue, sentMessage);
				try {
					long tsReplyReceived = System.currentTimeMillis();
					long tsRequestSent = sentMessage.getJMSTimestamp();
					long tsReplySent   = msg.getJMSTimestamp();
//					long jmsTimestampRcvd = msg.getJMSTimestamp();
////						long businessProcFinishSent=0;
//					long businessProcStartRcvd=0;
//					long businessProcStartSent=0;
//					long businessProcFinishRcvd=0;
//					if (sentMessage instanceof IFSAMessage) {
//						businessProcStartSent=((IFSAMessage)sentMessage).getBusinessProcessingStartTime();
////							businessProcFinishSent=((IFSAMessage)sentMessage).getBusinessProcessingFinishTime();
//					}
//					if (msg instanceof IFSAMessage) {
//						businessProcStartRcvd=((IFSAMessage)msg).getBusinessProcessingStartTime();
//						businessProcFinishRcvd=((IFSAMessage)msg).getBusinessProcessingFinishTime();
//					}
					if (log.isInfoEnabled()) {
						log.info(getLogPrefix()+"A) RequestSent   ["+ DateFormatUtils.format(tsRequestSent)   +"]");
						log.info(getLogPrefix()+"B) ReplySent     ["+ DateFormatUtils.format(tsReplySent)     +"] diff (~queing + processing) ["+(tsReplySent-tsRequestSent)+"]");
						log.info(getLogPrefix()+"C) ReplyReceived ["+ DateFormatUtils.format(tsReplyReceived) +"] diff (transport of reply )["+(tsReplyReceived-tsReplySent)+"]");
//						log.info(getLogPrefix()+"C2) msgRcvd.businessProcStartRcvd  ["+DateUtils.format(businessProcStartRcvd) +"] ");
//						log.info(getLogPrefix()+"D)  msgRcvd.jmsTimestamp           ["+DateUtils.format(jmsTimestampRcvd)      +"] diff ["+(jmsTimestampRcvd-businessProcStartSent)+"]");
//						log.info(getLogPrefix()+"E)  msgRcvd.businessProcFinishRcvd ["+DateUtils.format(businessProcFinishRcvd)+"] diff ["+(businessProcFinishRcvd-jmsTimestampRcvd)+"] (=time spend on IFSA bus sending result?)");
//						log.info(getLogPrefix()+"F)  timestampAfterRcvd             ["+DateUtils.format(timestampAfterRcvd)    +"] diff ["+(timestampAfterRcvd-businessProcFinishRcvd)+"] ");
//						log.info(getLogPrefix()+"business processing time (E-C1) ["+(businessProcFinishRcvd-businessProcStartSent)+"] ");
					}
//					if (businessProcessTimes!=null) {
//						businessProcessTimes.addValue(businessProcFinishRcvd-businessProcStartSent);
//					}
				} catch (JMSException e) {
					log.warn(getLogPrefix()+"exception determining processing times",e);
				}
				if (msg instanceof TextMessage) {
					result = ((TextMessage)msg).getText();
				} else {
					if (msg.getClass().getName().endsWith("IFSAReportMessage")) {
						if (msg instanceof IFSAReportMessage) {
							IFSAReportMessage irm = (IFSAReportMessage)msg;
							if (isThrowExceptions()) {
								throw new SenderException(getLogPrefix()+"received IFSAReportMessage ["+ToStringBuilder.reflectionToString(irm)+"], NoReplyReason ["+irm.getNoReplyReason()+"]");
							}
							log.warn(getLogPrefix()+"received IFSAReportMessage ["+ToStringBuilder.reflectionToString(irm)+"], NoReplyReason ["+irm.getNoReplyReason()+"]");
							result = "<IFSAReport>"+
										"<NoReplyReason>"+irm.getNoReplyReason()+"</NoReplyReason>"+
									 "</IFSAReport>";

						 }
					} else {
						log.warn(getLogPrefix()+"received neither TextMessage nor IFSAReportMessage but ["+msg.getClass().getName()+"]");
						result = msg.toString();
					}
				}
				if (result==null) {
					log.info(getLogPrefix()+"received null reply");
				} else {
					if (log.isDebugEnabled()) {
						if (AppConstants.getInstance().getBoolean("log.logIntermediaryResults",false)) {
							log.debug(getLogPrefix()+"received reply ["+result+"]");
						} else {
							log.debug(getLogPrefix()+"received reply");
						}
					} else {
						log.info(getLogPrefix()+"received reply");
					}
				}
			} else {
				result = sentMessage.getJMSMessageID();
			}
		} catch (JMSException e) {
			throw new SenderException(getLogPrefix()+"caught JMSException in sendMessage()",e);
		} catch (IfsaException e) {
			throw new SenderException(getLogPrefix()+"caught IfsaException in sendMessage()",e);
		} finally {
			if (sender != null) {
				try {
					log.debug(getLogPrefix()+"closing sender");
					sender.close();
				} catch (JMSException e) {
					log.debug(getLogPrefix()+"Exception closing sender", e);
				}
			}
			closeSession(session);
		}
		if (isThrowExceptions() && result!=null && result.startsWith("<exception>")) {
			throw new SenderException("Retrieved exception message from IFSA bus: "+result);
		}
		return result;
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		if (businessProcessTimes!=null) {
			hski.handleStatisticsKeeper(data,businessProcessTimes);
			businessProcessTimes.performAction(action);
		}
	}

	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this);
		result += ts.toString();
		return result;
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	public void setThrowExceptions(boolean b) {
		throwExceptions = b;
	}
	public boolean isThrowExceptions() {
		return throwExceptions;
	}

	public void setBifNameSessionKey(String bifnameSessionKey) {
		this.bifNameSessionKey = bifnameSessionKey;
	}
	public String getBifNameSessionKey() {
		return bifNameSessionKey;
	}
}
