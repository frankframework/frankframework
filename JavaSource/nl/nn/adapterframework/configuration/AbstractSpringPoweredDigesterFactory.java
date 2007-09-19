/*
 * Created on 11-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.configuration;

import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.ObjectCreationFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.xml.sax.Attributes;

/**
 * This is a factory for objects to be used with the 'factory-create-rule'
 * of the Apache Digester framework as a replacement for the 'object-create-rule'.
 * 
 * The intention is to have objects created by the Apache Digester be created
 * via the Spring Factory thus allowing for dependancy injection; and if not
 * possible then create them ourselves but inject at least a reference to the
 * Spring Factory when supported by the object. When the object is created
 * directly by this factory, the Spring Factory is used for auto-wiring
 * and initialization.
 * 
 * The factory is abstract; subclasses will need to implement method
 * 'getBeanName()' to return the name of the default Bean to load from
 * the Spring Context.
 * 
 * All Beans defined in the Spring Context that can be loaded via this
 * Factory must be defined as Prototype beans (scope="prototype") because
 * the expected behaviour from an ObjectCreationFactory is to create new
 * instances with each call. The Apache Digester will normally set extra 
 * properties for each instance. (See {@link #isPrototypesOnly()}.)
 * 
 * @author m00035f
 *
 */
public abstract class AbstractSpringPoweredDigesterFactory
    extends AbstractObjectCreationFactory
    implements ObjectCreationFactory {

    public static ListableBeanFactory factory;
    
    /**
     * 
     */
    public AbstractSpringPoweredDigesterFactory() {
        super();
    }
    
    /**
     * Get the name of the bean which should be retrieved from
     * the Spring BeanFactory.
     * 
     * If a className attribute has also been specified in the XML,
     * then that takes precedence over finding a bean with given
     * beanName.
     * 
     * If for the className multiple bean-definitions are found in
     * the factory, then a bean is selected from those with this
     * given bean-name. If no such bean exists, an error is thrown.
     * 
     * @return
     */
    abstract public String getBeanName();
    
    /**
     * Return <code>true</code> is only prototype beans from the
     * Spring Context will be returned, <code>false</code> is
     * a Spring singleton bean might be returned.
     * 
     * This is hard-coded to return <code>true</code> only in this
     * class. If a subclass wishes to allow using singleton-beans,
     * then this method should be overridden.
     * 
     * @return <code>true</code>
     */
    public boolean isPrototypesOnly() {
        return true;
    }
    
    /** 
     * Create object using, if possible, the Spring BeanFactory.
     * 
     * An object is created according to the following rules:
     * <ol>
     * <li>If <em>no</em> attribute 'className' is given in the configuration file,
     * then the bean named with method getBeanName() is created from the
     * Spring context.</li>
     * <li>If exactly 1 bean of type given by 'className' attribute can be
     * found in the Spring context, an instance of that bean is created
     * from the Spring factory.<br/>
     * The value returned by method getBeanName() is, in this case, 
     * not relevant.</li>
     * <li>If multiple beans of type given by 'className' attribute are
     * defined in the Spring context, then an instance is created whose 
     * bean-name is the same as that returned by the method getBeanName().</li>
     * <li>If the Spring context contains no beans of type 'className', then
     * a new instance of this class is created without accessing the
     * Spring factory.<br/>
     * If the created class implements interface 
     * {@link org.springframework.beans.factory.BeanFactoryAware} however,
     * the Spring factory will be passed as a property so that it can
     * be accessed directly from the bean.<br/>
     * (NB:Objects created by the Spring Factory will also have a pointer to
     * the creating BeanFactory when they implement this interface.)</li>
     * <li></li>
     * </ol>
     * 
     * @see org.apache.commons.digester.ObjectCreationFactory#createObject(org.xml.sax.Attributes)
     */
    public Object createObject(Attributes attrs) throws Exception {
        String className = attrs.getValue("className");
        String beanName;
        Class beanClass;
        
        // No explicit classname given; get bean from Spring Factory
        if (className == null) {
            beanName = getBeanName();
            beanClass = null;
        } else {
            // Get all beans matching the classname given
            beanClass = Class.forName(className);
            String[] matchingBeans = factory.getBeanNamesForType(beanClass);
            if (matchingBeans.length == 1) {
                // Only 1 bean of this type, so create it
                beanName = matchingBeans[0];
            } else if (matchingBeans.length > 1) {
                // multiple beans; find if there's one with the
                // same name as from 'getBeanName'.
                beanName = getBeanName();
            } else {
                // No beans matching the type.
                // Create instance, and if the instance implements
                // Spring's BeanFactoryAware interface, use it to
                // set BeanFactory attribute on this Bean.
                Object o = beanClass.newInstance();
                if (factory instanceof AutowireCapableBeanFactory) {
                    ((AutowireCapableBeanFactory)factory)
                        .autowireBeanProperties(
                            o, 
                            AutowireCapableBeanFactory.AUTOWIRE_BY_NAME,
                            true);
                    o = ((AutowireCapableBeanFactory)factory).initializeBean(o, getBeanName());
                } else if (o instanceof BeanFactoryAware) {
                    ((BeanFactoryAware)o).setBeanFactory(factory);
                }
                return o;
            }
        }
        
        // Only accept prototype-beans!
        if (isPrototypesOnly() && !factory.isPrototype(beanName)) {
            throw new Exception("Beans created from the BeanFactory must be prototype-beans, bean '"
                + beanName + "' of class '" + className + "' is not.");
        }
        
        return factory.getBean(beanName, beanClass);
    }

}
