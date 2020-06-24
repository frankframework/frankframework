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
package nl.nn.adapterframework.extensions.ifsa.ejb;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Session;

import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.api.ServiceURI;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.stream.Message;

/**
 *
 * @author Tim van der Leeuw
 * @since 4.8
 */
public class IfsaProviderListener extends IfsaEjbBase implements IPortConnectedListener {
    
    private IMessageHandler handler;
    private IbisExceptionListener exceptionListener;
    private IReceiver receiver;
    private IListenerConnector listenerPortConnector;
    
    @Override
   public void setHandler(IMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void setExceptionListener(IbisExceptionListener listener) {
        this.exceptionListener = listener;
    }

    @Override
    public void configure() throws ConfigurationException {
        super.configure();
        listenerPortConnector.configureEndpointConnection(this, null, null, null, getExceptionListener(),
                null, Session.AUTO_ACKNOWLEDGE, false, null, timeOut, -1);
    }

    @Override
    public void open() throws ListenerException {
        listenerPortConnector.start();
    }

    @Override
    public void close() throws ListenerException {
        listenerPortConnector.stop();
    }

    @Override
    public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
        ServiceRequest request = (ServiceRequest) rawMessage;
        return request.getUniqueId();
    }

    @Override
    public Message extractMessage(Object rawMessage, Map threadContext) throws ListenerException {
        ServiceRequest request = (ServiceRequest) rawMessage;
        return new Message(request.getBusinessMessage().getText());
    }

    @Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
        // Nothing to do here
        return;
    }

    @Override
    public IbisExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    @Override
    public IMessageHandler getHandler() {
        return handler;
    }

    @Override
    public IReceiver getReceiver() {
        return receiver;
    }

    @Override
    public void setReceiver(IReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public String getListenerPort() {
        String appIdName = getApplicationId().replaceFirst("IFSA://", "");
        return "IFSA_" + appIdName + "_" + getMessageProtocol() + "_Listener";
    }

    @Override
    public IListenerConnector getListenerPortConnector() {
        return listenerPortConnector;
    }

    public void setListenerPortConnector(IListenerConnector listenerPortConnector) {
        this.listenerPortConnector = listenerPortConnector;
    }


    public void populateThreadContext(Object rawMessage, Map threadContext, Session session) throws ListenerException {
        ServiceRequest request = (ServiceRequest) rawMessage;
        
        // Get variables from the IFSA Service Request, in as good manner
        // as possible to emulate the way that the JMS IfsaProviderListener works
        String mode = getMessageProtocol().equals("RR")? "NON_PERSISTENT" : "PERSISTENT";
        String id = request.getUniqueId();
        String cid = id;
        if (log.isDebugEnabled()) {
            log.debug("Setting correlation ID to MessageId");
        }
        Date dTimeStamp = new Date();
        Message messageText = extractMessage(rawMessage, threadContext);
        
        String fullIfsaServiceName = null;
        ServiceURI requestedService = request.getServiceURI();
        String ifsaServiceName=null, ifsaGroup=null, ifsaOccurrence=null, ifsaVersion=null;
        
        ifsaServiceName = requestedService.getService();
        ifsaGroup = requestedService.getGroup();
        ifsaOccurrence = requestedService.getOccurrence();
        ifsaVersion = requestedService.getVersion();
        
        if (log.isDebugEnabled()) {
                log.debug(getLogPrefix()+ "got message for [" + fullIfsaServiceName
                                + "] with JMSDeliveryMode=[" + mode
                                + "] \n  JMSMessageID=[" + id
                                + "] \n  JMSCorrelationID=["+ cid
                                + "] \n  ifsaServiceName=["+ ifsaServiceName
                                + "] \n  ifsaGroup=["+ ifsaGroup
                                + "] \n  ifsaOccurrence=["+ ifsaOccurrence
                                + "] \n  ifsaVersion=["+ ifsaVersion
                                + "] \n  Timestamp=[" + dTimeStamp.toString()
                                + "] \n  ReplyTo=[none"
                                + "] \n  MessageHeaders=[<unknown>"
                                + "] \n  Message=[" + messageText.toString()+"\n]");

        }
        threadContext.put("id", id);
        threadContext.put(IPipeLineSession.technicalCorrelationIdKey, cid);
        threadContext.put("timestamp", dTimeStamp);
        threadContext.put("replyTo", "none");
        threadContext.put("messageText", messageText);
        threadContext.put("fullIfsaServiceName", fullIfsaServiceName);
        threadContext.put("ifsaServiceName", ifsaServiceName);
        threadContext.put("ifsaGroup", ifsaGroup);
        threadContext.put("ifsaOccurrence", ifsaOccurrence);
        threadContext.put("ifsaVersion", ifsaVersion);

        Map udz = request.getAllUserDefinedZones();
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
    }
}
