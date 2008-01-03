/*
 * $Log: IfsaProviderListener.java,v $
 * Revision 1.3  2008-01-03 15:45:28  europe\L190409
 * rework port connected listener interfaces
 *
 * Revision 1.2  2007/11/22 08:48:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update from ejb-branch
 *
 * Revision 1.1.2.12  2007/11/14 09:11:50  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix unimplemented-method error (no implementation required, add no-op implementation)
 *
 * Revision 1.1.2.11  2007/11/06 14:03:10  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix method to get name of WebSphere Listener Port
 *
 * Revision 1.1.2.10  2007/11/06 13:34:52  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Remove unused imports
 *
 * Revision 1.1.2.9  2007/11/06 13:15:10  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Move code putting properties into threadContext from 'getIdFromRawMessage' to 'populateThreadContext'
 *
 * Revision 1.1.2.8  2007/11/06 12:49:33  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add methods 'populateThreadContext' and 'destroyThreadContext' to interface IPortConnectedListener
 *
 * Revision 1.1.2.7  2007/11/06 12:33:07  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Implement more closely some of the details of original code
 *
 * Revision 1.1.2.6  2007/11/06 10:40:24  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Make IfsaProviderListener follow state of it's ListenerPort, like with JmsListener
 *
 * Revision 1.1.2.5  2007/11/06 10:36:49  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Make IfsaProviderListener follow state of it's ListenerPort, like with JmsListener
 *
 * Revision 1.1.2.4  2007/11/05 13:51:37  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add 'version' string to new IFSA classes
 *
 * Revision 1.1.2.3  2007/10/29 12:25:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * Revision 1.1.2.2  2007/10/29 09:33:00  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Refactor: pullup a number of methods to abstract base class so they can be shared between IFSA parts
 *
 * Revision 1.1.2.1  2007/10/25 15:03:44  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Begin work on implementing IFSA-EJB
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Session;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.api.ServiceURI;

/**
 *
 * @author Tim van der Leeuw
 * @since 4.8
 * @version Id
 */
public class IfsaProviderListener extends IfsaEjbBase implements IPortConnectedListener {
    public static final String version = "$RCSfile: IfsaProviderListener.java,v $ $Revision: 1.3 $ $Date: 2008-01-03 15:45:28 $";
    
    private IMessageHandler handler;
    private IbisExceptionListener exceptionListener;
    private IReceiver receiver;
    private IListenerConnector listenerPortConnector;
    
    public void setHandler(IMessageHandler handler) {
        this.handler = handler;
    }

    public void setExceptionListener(IbisExceptionListener listener) {
        this.exceptionListener = listener;
    }

    public void configure() throws ConfigurationException {
        super.configure();
        listenerPortConnector.configureEndpointConnection(this, null, null, getExceptionListener(), null, false, null);
    }

    public void open() throws ListenerException {
        listenerPortConnector.start();
    }

    public void close() throws ListenerException {
        listenerPortConnector.stop();
    }

    public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
        ServiceRequest request = (ServiceRequest) rawMessage;
        return request.getUniqueId();
    }

    public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
        ServiceRequest request = (ServiceRequest) rawMessage;
        return request.getBusinessMessage().getText();
    }

    public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
        // Nothing to do here
        return;
    }

    public IbisExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    public IMessageHandler getHandler() {
        return handler;
    }

    public IReceiver getReceiver() {
        return receiver;
    }

    public void setReceiver(IReceiver receiver) {
        this.receiver = receiver;
    }

    public String getListenerPort() {
        String appIdName = getApplicationId().replaceFirst("IFSA://", "");
        return "IFSA_" + appIdName + "_" + getMessageProtocol() + "_Listener";
    }

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
        String messageText = getStringFromRawMessage(rawMessage, threadContext);
        
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
                                + "] \n  Message=[" + messageText+"\n]");

        }
        threadContext.put("id", id);
        threadContext.put("cid", cid);
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
