/*
 * $Log: IfsaRequesterSender.java,v $
 * Revision 1.11  2008-09-01 15:10:17  europe\L190409
 * forward BIFname when present
 *
 * Revision 1.10  2008/08/27 15:56:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added reset option to statisticsdump
 *
 * Revision 1.9  2008/05/22 07:23:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added some support for bif and btc
 *
 * Revision 1.8  2008/05/15 14:32:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * preparations for business process time statistics
 *
 * Revision 1.7  2008/04/17 12:58:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restored timeout handling
 *
 * Revision 1.6  2008/03/27 12:00:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set default timeout to 20s
 *
 * Revision 1.5  2008/02/13 12:55:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show detailed processing times
 *
 * Revision 1.4  2008/01/30 15:11:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * simplified transaction checking
 *
 * Revision 1.3  2008/01/11 14:50:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed transaction checking to using Spring
 *
 * Revision 1.2  2007/10/16 08:39:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved IfsaException and IfsaMessageProtocolEnum back to main package
 *
 * Revision 1.1  2007/10/16 08:15:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced switch class for jms and ejb
 *
 * Revision 1.28  2007/10/08 12:16:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.27  2007/09/05 15:48:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved XA determination capabilities to IfsaConnection
 *
 * Revision 1.26  2007/08/10 11:20:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed attribute 'transacted'
 * automatic determination of transaction state and capabilities
 *
 * Revision 1.25  2007/08/03 08:41:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.24  2007/06/26 06:52:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added warnings about incorrect setting of transacted
 *
 * Revision 1.23  2006/11/06 08:18:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modifications for dynamic serviceId and occurrence
 *
 * Revision 1.22  2006/10/13 08:13:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modify comments
 *
 * Revision 1.21  2006/02/23 11:39:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * correct handling of IfsaReport reply messages
 *
 * Revision 1.20  2006/01/05 13:55:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.19  2005/12/20 16:59:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.18  2005/10/26 08:48:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.17  2005/10/24 09:59:23  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.16  2005/10/18 07:04:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better handling of dynamic reply queues
 *
 * Revision 1.15  2005/09/13 15:56:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed acknowledge mode back to AutoAcknowledge
 * provided option to set serviceId dynamically from a parameter
 *
 * Revision 1.14  2005/09/01 11:19:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * no ack in case of timeout
 *
 * Revision 1.13  2005/08/31 16:33:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use same session for sending and receiving of reply
 *
 * Revision 1.12  2005/08/24 15:45:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * acknowledge receipt of reply-message
 *
 * Revision 1.11  2005/04/26 09:24:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * put closings in finally clause
 *
 * Revision 1.10  2005/04/20 14:21:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed rather useless warning
 *
 * Revision 1.9  2004/08/26 11:12:00  Johan Verrips <johan.verrips@ibissource.org>
 * Aangepast dat wanneer niet synchroon het messageid wordt teruggeven
 *
 * Revision 1.8  2004/07/22 11:03:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging of non-TextMessages
 *
 * Revision 1.7  2004/07/20 13:28:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented IFSA timeout mode
 *
 * Revision 1.6  2004/07/19 13:20:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes to logging
 *
 * Revision 1.5  2004/07/19 09:52:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made multi-threading, like JmsSender
 *
 * Revision 1.4  2004/07/15 07:43:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/07/08 12:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * logging refinements
 *
 * Revision 1.2  2004/07/07 13:58:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.1  2004/07/05 14:28:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Firs version, converted from IfsaClient
 *
 */
package nl.nn.adapterframework.extensions.ifsa.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.extensions.ifsa.IfsaMessageProtocolEnum;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.StatisticsKeeper;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAReportMessage;
import com.ing.ifsa.IFSATimeOutMessage;


/**
 * {@link ISender} that sends a message to an IFSA service and, in case the messageprotocol is RR (Request-Reply)
 * it waits for an reply-message.
 * <br>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>the ApplicationID, in the form of "IFSA://<i>AppId</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceId(String) serviceId}</td><td>the ServiceID of the service to be called, in the form of "IFSA://<i>ServiceID</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td><td>&nbsp;</td></td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receiver timeout, in milliseconds. To use the timeout defined as IFSA expiry, set this value to -1</td><td>20000 (20s)</td></tr>
 * </table>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>serviceId</td><td>string</td><td>When a parameter with name serviceId is present, it is used at runtime instead of the serviceId specified by the attribute. A lookup of the service for this serviceId will be done at runtime, while the service for the serviceId specified as an attribute will be done at configuration time. Hence, IFSA configuration problems will be detected at runtime for the serviceId specified as a param and at configuration time for the serviceId specified with an attribute</td></tr>
 * <tr><td>occurrence</td><td>string</td><td>The occurrence part of the serviceId (the part between the fourth and the fifth slash). The occurence part of the specified serviceId (either specified by a parameter or an attribute) will be replaced with the value of this parameter at runtime. From "IFSA - Naming Standards.doc": IFSA://SERVICE/&lt;group&gt;/&lt;occurrence&gt;/&lt;service&gt;/&lt;version&gt;[?&lt;parameter=value&gt;]</td></tr>
 * <tr><td>all other parameters</td><td>string</td><td>All parameters (names and values) (except serviceId and occurrence) are passed to the outgoing UDZ (User Defined Zone) object</td></tr>
 * </table>
 *
 * @author Johan Verrips / Gerrit van Brakel
 * @since  4.2
 */
public class IfsaRequesterSender extends IfsaFacade implements ISenderWithParameters, HasStatistics {
	public static final String version="$RCSfile: IfsaRequesterSender.java,v $ $Revision: 1.11 $ $Date: 2008-09-01 15:10:17 $";
	
	protected ParameterList paramList = null;
	private StatisticsKeeper businessProcessTimes;

	public IfsaRequesterSender() {
  		super(false); // instantiate IfsaFacade as a requestor	
	}

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
	public void close() throws SenderException {
	    try {
	        closeService();
	    } catch (Throwable e) {
	        throw new SenderException(getLogPrefix() + "got error occured stopping sender", e);
	    }
	}

	/**
	 * returns true for Request/Reply configurations
	 */
	public boolean isSynchronous() {
		return getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY);
	}
	
	/**
	 * Retrieves a message with the specified correlationId from queue or other channel, but does no processing on it.
	 */
	private Message getRawReplyMessage(QueueSession session, IFSAQueue queue, TextMessage sentMessage) throws SenderException, TimeOutException {
	
		String selector=null;
	    Message msg = null;
		QueueReceiver replyReceiver=null;
		try {
		    replyReceiver = getReplyReceiver(session, sentMessage);
			selector=replyReceiver.getMessageSelector();
			
			long timeout = getExpiry(queue);
			log.debug(getLogPrefix()+"start waiting at most ["+timeout+"] ms for reply on message using selector ["+selector+"]");
		    msg = replyReceiver.receive(timeout);
		    if (msg!=null) {
		    	log.debug(getLogPrefix()+"received reply");
		    }

	    } catch (Exception e) {
	        throw new SenderException(getLogPrefix()+"got exception retrieving reply", e);
	    } finally {
			try {
				closeReplyReceiver(replyReceiver);
			} catch (IfsaException e) {
				log.error(getLogPrefix()+"error closing replyreceiver", e);
	        } 
		}
	    if (msg == null) {
	        throw new TimeOutException(getLogPrefix()+" timed out waiting for reply using selector ["+selector+"]");
	    }
		if (msg instanceof IFSATimeOutMessage) {
			throw new TimeOutException(getLogPrefix()+"received IFSATimeOutMessage waiting for reply using selector ["+selector+"]");
		}
		return msg;
//		try {
//			TextMessage result = (TextMessage)msg;
//			return result;
//		} catch (Exception e) {
//			throw new SenderException(getLogPrefix()+"reply received for message using selector ["+selector+"] cannot be cast to TextMessage ["+msg.getClass().getName()+"]",e);
//		}
	}

	public String sendMessage(String message) throws SenderException, TimeOutException {
		return sendMessage(null, message, (Map)null);
	}

	public String sendMessage(String dummyCorrelationId, String message) throws SenderException, TimeOutException {
		return sendMessage(dummyCorrelationId, message, (Map)null);
	}

	public String sendMessage(String dummyCorrelationId, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		
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

		ParameterValueList paramValueList;
		try {
			paramValueList = prc.getValues(paramList);
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"caught ParameterException in sendMessage() determining serviceId",e);
		}
		Map params = new HashMap();
		if (paramValueList != null && paramList != null) {
			for (int i = 0; i < paramList.size(); i++) {
				String key = paramList.getParameter(i).getName();
				String value = paramValueList.getParameterValue(i).asStringValue(null);
				params.put(key, value);
			}
		}
		//IFSAMessage originatingMessage = (IFSAMessage)prc.getSession().get(PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
		String BIF=(String)prc.getSession().get(PushingIfsaProviderListener.THREAD_CONTEXT_BIFNAME_KEY);
		return sendMessage(dummyCorrelationId, message, params,BIF,null);
	}

	public String sendMessage(String dummyCorrelationId, String message, Map params) throws SenderException, TimeOutException {
		return sendMessage(dummyCorrelationId, message,params, null, null);
	}

	/**
	 * Execute a request to the IFSA service.
	 * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
	 */
	public String sendMessage(String dummyCorrelationId, String message, Map params, String bifName, byte btcData[]) throws SenderException, TimeOutException {
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
				String serviceId = (String)params.get("serviceId");
				if (serviceId == null) {
					serviceId = getServiceId();
				}
				String occurrence = (String)params.get("occurrence");
				if (occurrence != null) {
					int i = serviceId.indexOf('/', serviceId.indexOf('/', serviceId.indexOf('/', serviceId.indexOf('/') + 1) + 1) + 1);
					int j = serviceId.indexOf('/', i + 1);
					serviceId = serviceId.substring(0, i + 1) + occurrence + serviceId.substring(j);
				}
				queue = getConnection().lookupService(getConnection().polishServiceId(serviceId));
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

			log.debug(getLogPrefix()+"sending message");

			long timestampBeforeSend = System.currentTimeMillis();
		    TextMessage sentMessage=sendMessage(session, sender, message, udzMap, bifName, btcData);
			log.debug(getLogPrefix()+"message sent");

			if (isSynchronous()){
		
				log.debug(getLogPrefix()+"waiting for reply");
				Message msg=getRawReplyMessage(session, queue, sentMessage);
				try {
					long timestampAfterRcvd = System.currentTimeMillis();
					long jmsTimestampSent = sentMessage.getJMSTimestamp();
					long jmsTimestampRcvd = msg.getJMSTimestamp();
//						long businessProcFinishSent=0;
					long businessProcStartRcvd=0;
					long businessProcStartSent=0;
					long businessProcFinishRcvd=0;
					if (sentMessage instanceof IFSAMessage) {
						businessProcStartSent=((IFSAMessage)sentMessage).getBusinessProcessingStartTime();
//							businessProcFinishSent=((IFSAMessage)sentMessage).getBusinessProcessingFinishTime();
					}
					if (msg instanceof IFSAMessage) {
						businessProcStartRcvd=((IFSAMessage)msg).getBusinessProcessingStartTime();
						businessProcFinishRcvd=((IFSAMessage)msg).getBusinessProcessingFinishTime();
					}
					if (log.isInfoEnabled()) {
						log.info(getLogPrefix()+"A)  timestampBeforeSend            ["+DateUtils.format(timestampBeforeSend)+"]");
						log.info(getLogPrefix()+"B)  msgSent.jmsTimestamp       	["+DateUtils.format(jmsTimestampSent)      +"] diff ["+(jmsTimestampSent-timestampBeforeSend)+"]");
						log.info(getLogPrefix()+"C1) msgSent.businessProcStartSent  ["+DateUtils.format(businessProcStartSent) +"] diff ["+(businessProcStartSent-jmsTimestampSent)+"]");
						log.info(getLogPrefix()+"C2) msgRcvd.businessProcStartRcvd  ["+DateUtils.format(businessProcStartRcvd) +"] ");
						log.info(getLogPrefix()+"D)  msgRcvd.jmsTimestamp           ["+DateUtils.format(jmsTimestampRcvd)      +"] diff ["+(jmsTimestampRcvd-businessProcStartSent)+"]");
						log.info(getLogPrefix()+"E)  msgRcvd.businessProcFinishRcvd ["+DateUtils.format(businessProcFinishRcvd)+"] diff ["+(businessProcFinishRcvd-jmsTimestampRcvd)+"] (=time spend on IFSA bus sending result?)");
						log.info(getLogPrefix()+"F)  timestampAfterRcvd             ["+DateUtils.format(timestampAfterRcvd)    +"] diff ["+(timestampAfterRcvd-businessProcFinishRcvd)+"] ");
						log.info(getLogPrefix()+"business processing time (E-C1) ["+(businessProcFinishRcvd-businessProcStartSent)+"] ");
					}	
					if (businessProcessTimes!=null) {						
						businessProcessTimes.addValue(businessProcFinishRcvd-businessProcStartSent);
					}
				} catch (JMSException e) {
					log.warn(getLogPrefix()+"exception determining processing times",e);
				}
				if (msg instanceof TextMessage) {
					result = ((TextMessage)msg).getText();
				} else {
					if (msg.getClass().getName().endsWith("IFSAReportMessage")) {
						if (msg instanceof IFSAReportMessage) {
							IFSAReportMessage irm = (IFSAReportMessage)msg;
							log.warn(getLogPrefix()+"received IFSAReportMessage ["+irm.toString()+"]");
							result = "<IFSAReport>"+
										"<NoReplyReason>"+irm.getNoReplyReason()+"</NoReplyReason>"+
									 "</IFSAReport>";
						 }
					} else {
						log.warn(getLogPrefix()+"received neither TextMessage nor IFSAReportMessage but ["+msg.getClass().getName()+"]");
						result = msg.toString();
					}
				}
				log.debug(getLogPrefix()+"reply received");
					
		    } else{
		    	 result=sentMessage.getJMSMessageID(); 
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
	    return result;	
	}

	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, boolean reset) {
		if (businessProcessTimes!=null) {
			hski.handleStatisticsKeeper(data,businessProcessTimes);
			if (reset) {
				businessProcessTimes.clear();
			}
		}
	}
	


	
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        result += ts.toString();
        return result;

	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

}
