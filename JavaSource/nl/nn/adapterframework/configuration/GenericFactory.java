/*
 * $Log: GenericFactory.java,v $
 * Revision 1.4  2007-10-24 07:13:21  europe\M00035F
 * Rename abstract method 'getBeanName()' to 'getSuggestedBeanName()' since it better reflects the role of the method in the class.
 *
 * Revision 1.3  2007/10/22 14:56:21  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Make the 'Generic Factory' more generically useful by allowing beans to be created based of name of element in IBIS Configuration file.
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
 * <p>
 * This factory uses the name of the current element for name of the bean,
 * instead of hard-wiring a bean name. The name of current element is prefixed
 * with the string "proto-", to prevent unwanted auto-wiring cascaded of
 * other prototype beans defined in the Spring Factory which are supposed to
 * be defined in the IBIS Configuration File. 
 * </p>
 * <p>
 * If a className attribute is specified in the configuration file, then
 * this is used together with the bean-name to find the bean to be
 * instantiated from the Spring Factory (see the rules laid out in
 * {@link AbstractSpringPoweredDigesterFactory}.
 * In particular, the bean-name is ignored when the class-name is specified
 * and the Spring Factory contains exactly 1 bean-definition for that
 * class.
 * </p>
 * <p>
 * This is useful for those kinds of rules in the digester-rules.xml where
 * the className was always mandatory in older versions, but also for those rules
 * where className is never specified and only 1 possible implementation
 * exists.
 * </p>
 * <p>
 * NB: The Apache Digester cannot read a factory-create-rule from XML and supply
 * parameters to the factory created from the XML digester-rules, so there is
 * no way to configure a factory instance with a bean-name from the digester-rules.
 * </p>
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class GenericFactory extends AbstractSpringPoweredDigesterFactory {

    /**
     * Return name of current element prefixed with the string "proto-" as 
     * bean-name.
     * 
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#getBeanName()
     */
    public String getSuggestedBeanName() {
        return "proto-" + getDigester().getCurrentElementName();
    }

}
