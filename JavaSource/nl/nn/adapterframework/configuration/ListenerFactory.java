/*
 * $Log: ListenerFactory.java,v $
 * Revision 1.6  2007-11-22 08:36:31  europe\L190409
 * improved logging
 *
 * Revision 1.2.2.4  2007/11/09 12:32:05  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Conditionalize logging for performance
 *
 * Revision 1.2.2.3  2007/11/09 12:05:56  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Improve logging of actions
 *
 * Revision 1.2.2.2  2007/11/09 11:59:46  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Reformat
 *
 * Revision 1.2.2.1  2007/10/24 09:39:48  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge changes from HEAD
 *
 * Revision 1.5  2007/10/24 08:04:23  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add logging for case when classname of Listener implementation is replaced
 *
 * Revision 1.4  2007/10/24 07:13:21  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename abstract method 'getBeanName()' to 'getSuggestedBeanName()' since it better reflects the role of the method in the class.
 *
 * Revision 1.3  2007/10/22 14:42:55  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Override createObject so that a JMS PullingJmsListener is created instead of a 'default' JmsListener when the parent is instance of MessageSendingPipe;

 * this is for compatibility with the MessageSendingPipe using an instance of ICorrelatedPullingListener (which the PushingJmsListener can not provide).

 * 

 * This solution is a workaround to be used until we decide how to refactor the MessageSendingPipe, or if the functionality of correlated listener should be

 * added to PushingJmsListener (which would contradict it's design, and currently have the unwanted side-effect ofcreating a JMS Container for the queue).
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.configuration;

import java.util.Map;

import nl.nn.adapterframework.pipes.MessageSendingPipe;

import org.xml.sax.Attributes;

/**
 * 
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class ListenerFactory extends AbstractSpringPoweredDigesterFactory {
    public static final String JMS_LISTENER_CLASSNAME_SUFFIX = ".JmsListener";
    protected static final String CORRELATED_LISTENER_CLASSNAME = "nl.nn.adapterframework.jms.PullingJmsListener";

    /**
     * Default bean to create from the Spring factory is 'proto-jmsListener',
     * a JMS Listener bean.
     * 
     * This is different from the old situation, where the default class was
     * in fact an interface and therefore className could not be left out
     * of the configuration of a Listener.
     * 
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#getBeanName()
     */
    public String getSuggestedBeanName() {
        return "proto-jmsListener";
    }
    
    /**
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#createObject(org.xml.sax.Attributes)
     */
    public Object createObject(Attributes attrs) throws Exception {
        String className = attrs.getValue("className");
        if (className != null && getDigester().peek() instanceof MessageSendingPipe && className.endsWith(JMS_LISTENER_CLASSNAME_SUFFIX)) {
            if (log.isDebugEnabled()) {
                log.debug("JmsListener is created as part of a MessageSendingPipe; replace classname with '" + CORRELATED_LISTENER_CLASSNAME + "' to ensure compatibility");
            }
            return createBeanFromClassName(CORRELATED_LISTENER_CLASSNAME);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Creating Listener class '" + className + "'");
            }
            return createBeanFromClassName(className);
        }
    }

}
