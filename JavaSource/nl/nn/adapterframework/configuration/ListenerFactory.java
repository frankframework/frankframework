/*
 * Created on 18-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.configuration;

/**
 * @author m00035f
 *
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
