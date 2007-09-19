/*
 * Created on 19-sep-07
 *
 */
package nl.nn.adapterframework.configuration;

/**
 * @author m00035f
 *
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
