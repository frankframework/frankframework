/*
 * $Log: ListenerFactory.java,v $
 * Revision 1.1.2.2  2007-10-10 14:30:41  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.configuration;

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

}
