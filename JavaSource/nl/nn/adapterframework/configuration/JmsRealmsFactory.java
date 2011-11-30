/*
 * $Log: JmsRealmsFactory.java,v $
 * Revision 1.4  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2007/10/24 07:13:21  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename abstract method 'getBeanName()' to 'getSuggestedBeanName()' since it better reflects the role of the method in the class.
 *
 * Revision 1.1  2007/10/23 09:18:28  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add Digester-Factory for the JmsRealmFactory object to retrieve it from spring beans factory instead of old helper-bean-object.
 *
 * Created on 23-okt-07
 *
 */
package nl.nn.adapterframework.configuration;

/**
 * Factory for the JMS Realms instance, which holds all JMS Realm definitions
 * found in the Configuration File.
 * 
 * This bean is a singleton, not a prototype, which is why the GenericFactory
 * can not be used.
 * 
 * @author Tim van der Leeuw
 * @version Id
 *
 */
public class JmsRealmsFactory extends AbstractSpringPoweredDigesterFactory {

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#getBeanName()
     */
    public String getSuggestedBeanName() {
        return "jmsRealmsFactory";
    }
    
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#isPrototypesOnly()
     */
    public boolean isPrototypesOnly() {
        return false;
    }

}
