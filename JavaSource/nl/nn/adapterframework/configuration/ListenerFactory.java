/*
 * $Log: ListenerFactory.java,v $
 * Revision 1.3  2007-10-22 14:42:55  europe\M00035F
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
    public String getBeanName() {
        return "proto-jmsListener";
    }
    
	/**
	 * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#createObject(org.xml.sax.Attributes)
	 */
	public Object createObject(Attributes attrs) throws Exception {
		String className = attrs.getValue("className");
		if (className != null && getDigester().peek() instanceof MessageSendingPipe && className.endsWith(".JmsListener")) {
			return createBeanFromClassName("nl.nn.adapterframework.jms.PullingJmsListener");
		} else {
            return createBeanFromClassName(className);
		}
	}

}
