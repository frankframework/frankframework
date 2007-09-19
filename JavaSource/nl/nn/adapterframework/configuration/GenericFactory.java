/*
 * Created on 19-sep-07
 *
 * $Id: GenericFactory.java,v 1.1.2.1 2007-09-19 14:19:41 europe\M00035F Exp $
 */
package nl.nn.adapterframework.configuration;

/**
 * Generic factory for instantiating beans from the Digester framework.
 * 
 * This factory always returns <code>null</code> for name of the bean,
 * so that beans can never be looked up from the Spring Factory by
 * name but always have to be looked up by className attribute.
 * 
 * This is useful for those kinds of rules in the digester-rules.xml where
 * the className was mandatory anyways.
 *
 * NB: It doesn't help to add a settable property for the bean-name, because
 * the Apache Digester cannot read a factory-create-rule from XML and supply
 * parameters to the factory created from the XML.
 * 
 * @author m00035f
 *
 */
public class GenericFactory extends AbstractSpringPoweredDigesterFactory {

    /**
     * Return <code>null</code> as bean-name so that beans are always
     * looked up by class (or auto-wired if not in factory), and never
     * looked up by name.
     * 
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#getBeanName()
     */
    public String getBeanName() {
        return null;
    }

}
