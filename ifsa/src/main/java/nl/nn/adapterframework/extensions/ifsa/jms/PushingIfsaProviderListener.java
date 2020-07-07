/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.ing.ifsa.IFSAHeader;
import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAPoisonMessage;
import com.ing.ifsa.IFSAServiceName;
import com.ing.ifsa.IFSAServicesProvided;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IKnowsDeliveryCount;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionRequirements;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.extensions.ifsa.IfsaMessageProtocolEnum;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Implementation of {@link IPortConnectedListener} that acts as an IFSA-service.
 * 
 * There is no need or possibility to set the ServiceId as the Provider will receive all messages
 * for this Application on the same serviceQueue.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaProviderListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>the ApplicationID, in the form of "IFSA://<i>AppId</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCacheMode(String) cacheMode}</td><td>controls caching of JMS objects. Must be one of CACHE_NONE, CACHE_CONNECTION, CACHE_SESSION, CACHE_CONSUMER</td><td>effectively: <ul>
 *   <li>in transacted receivers: CACHE_NONE</li>
 *   <li>in non transacted receivers: CACHE_CONSUMER</li>
 * </ul></td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receive timeout, in milliseconds</td><td>3000</td></tr>
 * </table>
 * The following session keys are set for each message:
 * <ul>
 *   <li>id (the message id)</li>
 *   <li>cid (the correlation id)</li>
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
 * N.B. 
 * Starting from IFSA-jms version 2.2.10.055(beta) a feature was created to have separate service-queues for Request/Reply
 * and for Fire & Forget services. This allows applications to provide both types of services, each in its own transaction
 * mode. This options is not compatible with earlier versions of IFSA-jms. If an earlier version of IFSA-jms is deployed on 
 * the server, this behaviour must be disabled by the following setting in DeploymentSpecifics.properties:
 * 
 * <code>ifsa.provider.useSelectors=false</code>
 * 
 * <p>
 * For Fire&Forget providers, the message log might get cluttered with messages like:
 * <code><pre>
   [1-10-08 17:10:34:382 CEST] 209d4317 ConnectionMan W J2CA0075W: An active transaction should be present while processing method allocateMCWrapper.
   [1-10-08 17:10:34:382 CEST] 209d4317 ConnectionMan W J2CA0075W: An active transaction should be present while processing method initializeForUOW
 * </pre></code> 
 * This is due to a IFSA requirement, that sessions be created using a parameter transacted=true, indicating
 * JMS transacted sessions. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public class PushingIfsaProviderListener extends IfsaFacade implements IPortConnectedListener<IFSAMessage>, IThreadCountControllable, IKnowsDeliveryCount, ITransactionRequirements {

	public final static String THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY = "originalRawMessage";
	public final static String THREAD_CONTEXT_BIFNAME_KEY="IfsaBif";


	private String listenerPort;
	private String cacheMode; // default is set in spring container

	private IListenerConnector jmsConnector;
	private IMessageHandler<IFSAMessage> handler;
	private IReceiver receiver;
	private IbisExceptionListener exceptionListener;

	public PushingIfsaProviderListener() {
		super(true); //instantiate as a provider
		setTimeOut(3000); // set default timeout, to be able to stop adapter!
	}

	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (jmsConnector==null) {
			throw new ConfigurationException(getLogPrefix()+" has no jmsConnector. It should be configured via springContext.xml");
		}
		if (StringUtils.isNotEmpty(getCacheMode())) {
			if (!getCacheMode().equals("CACHE_NONE") && 
				!getCacheMode().equals("CACHE_CONNECTION") && 
				!getCacheMode().equals("CACHE_SESSION") && 
				!getCacheMode().equals("CACHE_CONSUMER")) {
					throw new ConfigurationException(getLogPrefix()+"cacheMode ["+getCacheMode()+"] must be one of CACHE_NONE, CACHE_CONNECTION, CACHE_SESSION or CACHE_CONSUMER");
				}
		}
		Destination destination=null;
		try {
			destination=getServiceQueue();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"could not get Destination",e);
		}
		try {
			jmsConnector.configureEndpointConnection(this, getMessagingSource().getConnectionFactory(), null,
					destination, getExceptionListener(), getCacheMode(), getAckMode(), isJmsTransacted(),
					getProviderSelector(), getTimeOut(), -1);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}



	@Override
	public void open() throws ListenerException {
		try {
			openService();
			
			IFSAServicesProvided services = getServiceQueue().getIFSAServicesProvided();

			for (int i = 0; i < services.getNumberOfServices(); i++) {
				IFSAServiceName service = services.getService(i);
				
				String protocol=(service.IsFireAndForgetService() ? "Fire and Forget" : "Request/Reply");
				log.info(getLogPrefix()+"providing ServiceName ["+service.getServiceName()+"] ServiceGroup [" + service.getServiceGroup()+"] protocol [" + protocol+"] ServiceVersion [" + service.getServiceVersion()+"]");				
			}
			jmsConnector.start();
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}
	

	@Override
	public void close() throws ListenerException {
		try {
			jmsConnector.stop();
			closeService();
		} catch (IfsaException e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}
	
	@Override
	public boolean transactionalRequired() {
		return this.getMessageProtocolEnum()==IfsaMessageProtocolEnum.FIRE_AND_FORGET;
	}

	@Override
	public boolean transactionalAllowed() {
		return true;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult plr, Object rawMessageOrWrapper, Map<String,Object> threadContext) throws ListenerException {	
		QueueSession session= (QueueSession) threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);
			    		    
	    // on request-reply send the reply.
	    if (getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY)) {
			javax.jms.Message originalRawMessage;
			if (rawMessageOrWrapper instanceof javax.jms.Message) { 
				originalRawMessage = (javax.jms.Message)rawMessageOrWrapper;
			} else {
				originalRawMessage = (javax.jms.Message)threadContext.get(THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
			}
			if (originalRawMessage==null) {
				String cid = (String) threadContext.get(IPipeLineSession.businessCorrelationIdKey);
				log.warn(getLogPrefix()+"no original raw message found for correlationId ["+cid+"], cannot send result");
			} else {
				if (session==null) {
					throw new ListenerException(getLogPrefix()+"no session found in context, cannot send result");
				}
				try {
					String result="<exception>no result</exception>";
					if (plr!=null && plr.getResult()!=null) {
						result=plr.getResult().asString();
					}
					sendReply(session, originalRawMessage, result);
				} catch (IfsaException | IOException e) {
					try {
						sendReply(session, originalRawMessage, "<exception>"+e.getMessage()+"</exception>");
					} catch (IfsaException e2) {
						log.warn(getLogPrefix()+"exception sending errormessage as reply",e2);
					}
					throw new ListenerException(getLogPrefix()+"Exception on sending result", e);
				}
			}
	    }
	}
	

	protected String getIdFromWrapper(IMessageWrapper wrapper, Map<String,Object> threadContext)  {
		for (Iterator<String> it=wrapper.getContext().keySet().iterator(); it.hasNext();) {
			String key = it.next();
			Object value = wrapper.getContext().get(key);
			log.debug(getLogPrefix()+"setting variable ["+key+"] to ["+value+"]");
			threadContext.put(key, value);
		}
		return wrapper.getId();
	}
	protected Message getMessageFromWrapper(IMessageWrapper wrapper, Map<String,Object> threadContext)  {
		return wrapper.getMessage();
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
	@Override
	public String getIdFromRawMessage(IFSAMessage rawMessage, Map<String,Object> threadContext) throws ListenerException {
	
		IFSAMessage message = null;
	 
	 	if (rawMessage instanceof IMessageWrapper) {
	 		return getIdFromWrapper((IMessageWrapper)rawMessage,threadContext);
	 	}
	 
	    try {
	        message = (IFSAMessage) rawMessage;
	    } catch (ClassCastException e) {
	        log.error(getLogPrefix()+
	            "message received was not of type IFSAMessage, but [" + rawMessage.getClass().getName() + "]", e);
	        return null;
	    }
	    String mode = "unknown";
	    String id = "unset";
	    String cid = "unset";
	    Date tsSent = null;
	    Destination replyTo = null;
	    String messageText = null;
		String fullIfsaServiceName = null;
	    IFSAServiceName requestedService = null;
	    String ifsaServiceName=null, ifsaGroup=null, ifsaOccurrence=null, ifsaVersion=null;
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
	        messageText = ((TextMessage)message).getText();
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
	
		PipeLineSessionBase.setListenerParameters(threadContext, id, BIFname, null, tsSent);
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

		Map udz = (Map)message.getIncomingUDZObject();
		if (udz!=null) {
			String contextDump = "ifsaUDZ:";
			for (Iterator it = udz.keySet().iterator(); it.hasNext();) {
				String key = (String)it.next();
				String value = (String)udz.get(key);
				contextDump = contextDump + "\n " + key + "=[" + value + "]";
				threadContext.put(key, value);
			}
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix()+ contextDump);
			}
		}

	    return BIFname;
	}
	
	private String displayHeaders(IFSAMessage message) {
		StringBuffer result= new StringBuffer();
		try { 
			for(Enumeration enumeration = message.getPropertyNames(); enumeration.hasMoreElements();) {
				String tagName = (String)enumeration.nextElement();
				Object value = message.getObjectProperty(tagName);
				result.append("\n").append(tagName).append(": ");
				if (value==null) {
					result.append("null");
				} else {
					result.append("(").append(ClassUtils.nameOf(value)).append(") [").append(value).append("]");
					if (tagName.startsWith("ifsa") && 
						!tagName.equals("ifsa_unique_id") && 
						!tagName.startsWith("ifsa_epz_") && 
						!tagName.startsWith("ifsa_udz_")) {
							result.append(" * copied when sending reply");
							if (!(value instanceof String)) {
								result.append(" THIS CAN CAUSE A PROBLEM AS "+ClassUtils.nameOf(value)+" IS NOT String!");
							}
						}
				}
			}
		} catch(Throwable t) {
			log.warn("exception parsing headers",t);
		}
		return result.toString();
	}
	
	
	/**
	 * Extracts message string from raw message. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return input message for adapter.
	 */
	@Override
	public Message extractMessage(IFSAMessage rawMessage, Map<String,Object> threadContext) throws ListenerException {
		if (rawMessage instanceof IMessageWrapper) {
			return getMessageFromWrapper((IMessageWrapper)rawMessage,threadContext);
		}
		if (rawMessage instanceof IFSAPoisonMessage) {
			IFSAPoisonMessage pm = (IFSAPoisonMessage)rawMessage;
			IFSAHeader header = pm.getIFSAHeader();
			String source;
			try {
				source = header.getIFSA_Source();
			} catch (Exception e) {
				source = "unknown due to exeption:"+e.getMessage();
			}
			return  new Message("<poisonmessage>"+
					"  <source>"+source+"</source>"+
					"  <contents>"+XmlUtils.encodeChars(ToStringBuilder.reflectionToString(pm))+"</contents>"+
					"</poisonmessage>");
		}

	    TextMessage message = null;
	    try {
	        message = (TextMessage) rawMessage;
	    } catch (ClassCastException e) {
	        log.warn(getLogPrefix()+ "message received was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
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


    
	public void setJmsConnector(IListenerConnector configurator) {
		jmsConnector = configurator;
	}
	public IListenerConnector getJmsConnector() {
		return jmsConnector;
	}
	@Override
	public IListenerConnector getListenerPortConnector() {
		return jmsConnector;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		this.exceptionListener = listener;
	}
	@Override
	public IbisExceptionListener getExceptionListener() {
		return exceptionListener;
	}

	@Override
	public void setHandler(IMessageHandler<IFSAMessage> handler) {
		this.handler = handler;
	}
	@Override
	public IMessageHandler<IFSAMessage> getHandler() {
		return handler;
	}




	/**
	 * Name of the WebSphere listener port that this JMS Listener binds to. Optional.
	 * 
	 * This property is only used in EJB Deployment mode and has no effect otherwise. 
	 * If it is not set in EJB Deployment Mode, then the listener port name is
	 * constructed by the EjbListenerPortConnector from
	 * the Listener name, Adapter name and the Receiver name.
	 * 
	 * @param listenerPort Name of the listener port, as configured in the application
	 * server.
	 */
	public void setListenerPort(String listenerPort) {
		this.listenerPort = listenerPort;
	}
	/**
	 * Name of the WebSphere listener port that this JMS Listener binds to. Optional.
	 * 
	 * This property is only used in EJB Deployment mode and has no effect otherwise. 
	 * If it is not set in EJB Deployment Mode, then the listener port name is
	 * constructed by the EjbListenerPortConnector from
	 * the Listener name, Adapter name and the Receiver name.
	 * 
	 * @return The name of the WebSphere Listener Port, as configured in the
	 * application server.
	 */
	public String getListenerPort() {
		return listenerPort;
	}

    
	public void setReceiver(IReceiver receiver) {
		this.receiver = receiver;
	}
	public IReceiver getReceiver() {
		return receiver;
	}


	public void setCacheMode(String string) {
		cacheMode = string;
	}
	public String getCacheMode() {
		return cacheMode;
	}

	public boolean isThreadCountReadable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;
			
			return tcc.isThreadCountReadable();
		}
		return false;
	}

	public boolean isThreadCountControllable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;
			
			return tcc.isThreadCountControllable();
		}
		return false;
	}

	public int getCurrentThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;
			
			return tcc.getCurrentThreadCount();
		}
		return -1;
	}

	public int getMaxThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;
			
			return tcc.getMaxThreadCount();
		}
		return -1;
	}

	public void increaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;
			
			tcc.increaseThreadCount();
		}
	}

	public void decreaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;
			
			tcc.decreaseThreadCount();
		}
	}

	public int getDeliveryCount(Object rawMessage) {
		try {
			javax.jms.Message message=(javax.jms.Message)rawMessage;
			int value = message.getIntProperty("JMSXDeliveryCount");
			if (log.isDebugEnabled()) log.debug("determined delivery count ["+value+"]");
			return value;
		} catch (Exception e) {
			log.error(getLogPrefix()+"exception in determination of DeliveryCount",e);
			return -1;
		}
	}

}
