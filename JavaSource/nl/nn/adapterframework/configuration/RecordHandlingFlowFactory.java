/*
 * $Log: RecordHandlingFlowFactory.java,v $
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
public class RecordHandlingFlowFactory
    extends AbstractSpringPoweredDigesterFactory {

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#getBeanName()
     */
    public String getBeanName() {
        return "proto-RecordHandlingFlow";
    }

}
