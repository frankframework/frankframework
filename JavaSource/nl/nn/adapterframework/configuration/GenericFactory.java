/*
 * $Log: GenericFactory.java,v $
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
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
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
