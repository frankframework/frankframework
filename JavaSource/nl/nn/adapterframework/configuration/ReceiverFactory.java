/*
 * $Log: ReceiverFactory.java,v $
 * Revision 1.2  2007-10-09 15:29:43  europe\L190409
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
public class ReceiverFactory extends AbstractSpringPoweredDigesterFactory {
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#getBeanName()
     */
    public String getBeanName() {
        return "proto-receiver";
    }

}
