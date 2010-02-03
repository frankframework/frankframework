/*
 * $Log: AbstractSpringPoweredDigesterFactory.java,v $
 * Revision 1.14  2010-02-03 14:20:26  L190409
 * solved NPE in attribute default checker
 *
 * Revision 1.13  2009/11/24 08:37:16  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * cosmetic change
 *
 * Revision 1.12  2009/11/24 08:32:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * excluded ${property.key} values from default value check
 *
 * Revision 1.11  2009/11/10 15:41:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added setMethod check on default value for Integers
 *
 * Revision 1.10  2009/11/06 08:06:41  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attributes with default value to configuration warnings
 *
 * Revision 1.9  2008/02/13 12:51:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.8  2007/12/28 08:53:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.7  2007/11/22 08:23:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.6  2007/10/24 08:04:23  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add logging for case when classname of Listener implementation is replaced
 *
 * Revision 1.5  2007/10/24 07:13:21  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename abstract method 'getBeanName()' to 'getSuggestedBeanName()' since it better reflects the role of the method in the class.
 *
 * Revision 1.4  2007/10/23 14:16:20  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add some logging for improved debugging
 *
 * Revision 1.3  2007/10/22 14:38:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Refactor to better allow subclasses to override the createObject() method
 *
 * Revision 1.2  2007/10/09 16:02:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 * Revision 1.1.2.5  2007/09/21 14:22:15  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Apply a number of fixes so that the framework starts again
 *
 * Revision 1.1.2.4  2007/09/21 09:20:33  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Remove UserTransaction from Adapter
 * * Remove InProcessStorage; refactor a lot of code in Receiver
 *
 */
package nl.nn.adapterframework.configuration;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.ObjectCreationFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * This is a factory for objects to be used with the 'factory-create-rule'
 * of the Apache Digester framework as a replacement for the 'object-create-rule'.
 * 
 * The intention is to have objects created by the Apache Digester be created
 * via the Spring Factory thus allowing for dependency injection; and if not
 * possible then create them ourselves but inject at least a reference to the
 * Spring Factory when supported by the object. When the object is created
 * directly by this factory, the Spring Factory is used for auto-wiring
 * and initialization.
 * 
 * The factory is abstract; subclasses will need to implement method
 * 'getSuggestedBeanName()' to return the name of the default Bean to load from
 * the Spring Context.
 * 
 * All Beans defined in the Spring Context that can be loaded via this
 * Factory must be defined as Prototype beans (scope="prototype") because
 * the expected behaviour from an ObjectCreationFactory is to create new
 * instances with each call. The Apache Digester will normally set extra 
 * properties for each instance. (See {@link #isPrototypesOnly()}.)
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 * 
 */
public abstract class AbstractSpringPoweredDigesterFactory extends AbstractObjectCreationFactory implements ObjectCreationFactory {
	protected Logger log = LogUtil.getLogger(this);

    public static ListableBeanFactory factory;
	private ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
    
    public AbstractSpringPoweredDigesterFactory() {
        super();
    }
    
    /**
     * Suggest the name of the bean which should be retrieved from
     * the Spring BeanFactory.
     * 
     * If a className attribute has also been specified in the XML,
     * then that takes precedence over finding a bean with given
     * suggestedBeanName.
     * 
     * If for the className multiple bean-definitions are found in
     * the factory, then a bean is selected from those with this
     * given suggestedBeanName. If no such bean exists, an error is thrown
     * because the factory can not select between multiple beans.
     * 
     * @return
     */
    abstract public String getSuggestedBeanName();
    
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
     * then the bean named with method getSuggestedBeanName() is created from the
     * Spring context.</li>
     * <li>If exactly 1 bean of type given by 'className' attribute can be
     * found in the Spring context, an instance of that bean is created
     * from the Spring factory.<br/>
     * The value returned by method getSuggestedBeanName() is, in this case, 
     * not relevant.</li>
     * <li>If multiple beans of type given by 'className' attribute are
     * defined in the Spring context, then an instance is created whose 
     * bean-name is the same as that returned by the method getSuggestedBeanName().</li>
     * <li>If the Spring context contains no beans of type 'className', then
     * a new instance of this class is created without accessing the
     * Spring factory.<br/>
     * The Spring BeanFactory will then be invoked to attempt auto-wiring
     * beans by name and initialization via any BeanFactory - callback methods.
     * If the created class implements interface 
     * {@link org.springframework.beans.factory.BeanFactoryAware},
     * the Spring factory will be made available as a property so that it can
     * be accessed directly from the bean.<br/>
     * (NB:Objects created by the Spring Factory will also have a pointer to
     * the creating BeanFactory when they implement this interface.)</li>
     * <li></li>
     * </ol>
     * 
     * @see org.apache.commons.digester.ObjectCreationFactory#createObject(org.xml.sax.Attributes)
     */
    public Object createObject(Attributes attrs) throws Exception {
    	Map attrMap = copyAttrsToMap(attrs);
        return createObject(attrMap);
    }
    
    /**
     * Create Object from Spring factory, but using the attributes
     * from the XML converted to a Map. This is so that sub-classes
     * can override this method and change attributes in the map
     * before creating the object from the Spring factory.
     * 
     * @param attrs
     * @return
     * @throws Exception
     */
    protected Object createObject(Map attrs) throws Exception {
        String className = (String) attrs.get("className");
        if (log.isDebugEnabled()) {
            log.debug("CreateObject: Element=[" + getDigester().getCurrentElementName()
                    + "], name=[" + attrs.get("name")
                    + "], Configured ClassName=[" + className
                    + "], Suggested Spring Bean Name=[" + getSuggestedBeanName() + "]");
        }

		Object currObj = createBeanFromClassName(className);

		for (Iterator it = attrs.keySet().iterator(); it.hasNext();) {
			String attributeName = (String)it.next();
			String value = (String)attrs.get(attributeName);
			PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(currObj, attributeName);
			String name = (String)attrs.get("name");
			if (pd!=null) {
				Method rm = PropertyUtils.getReadMethod(pd);
				if (rm!=null) {
					try {
						Object dv = rm.invoke(currObj, null);
						if (dv!=null) {
							if (dv instanceof String) {
								if (value.equals(dv)) {
									addSetToDefaultConfigWarning(currObj, name, attributeName, value);
								}
							} else {
								if (dv instanceof Boolean) {
									if (Boolean.valueOf(value).equals(dv)) {
										addSetToDefaultConfigWarning(currObj, name, attributeName, value);
									}
								} else {
									if (dv instanceof Integer) {
										try {
											if (Integer.valueOf(value).equals(dv)) {
												addSetToDefaultConfigWarning(currObj, name, attributeName, value);
											}
										} catch (NumberFormatException e) {
											addConfigWarning(currObj, name, "attribute ["+ attributeName+"] String ["+value+"] cannot be converted to Integer: "+e.getMessage());
										}
									} else {
										log.warn("Unknown returning type [" + rm.getReturnType() + "]" + "for getter method [" + rm.getName() + "], object [" + getObjectName(currObj, name) + "]");
									}
								}
							}
						}
					} catch (Throwable t) {
						log.warn("Error on getting default for object [" + getObjectName(currObj, name) + "] with method [" + rm.getName() + "]", t);
					}
				}
			}
		}

		return currObj;
    }

	private String getObjectName(Object o, String name) {
		String result=o.getClass().getName();
		if (name!=null) {
			result+=" ["+name+"]";
		}
		return result;
	}

	private void addSetToDefaultConfigWarning(Object currObj, String name, String key, String value) {
		String mergedKey = getDigester().getCurrentElementName() + "/" + (name==null?"":name) + "/" + key;
		if (!configWarnings.containsDefaultValueExceptions(mergedKey)) {
			addConfigWarning(currObj, name, "attribute ["+key+"] already has a default value ["+value+"]");
		}
	}

	private void addConfigWarning(Object currObj, String name, String message) {
		Locator loc = digester.getDocumentLocator();
		String msg ="line "+loc.getLineNumber()+", col "+loc.getColumnNumber()+": "+getObjectName(currObj, name)+": "+message;
		configWarnings.add(log, msg);
	}

    /**
     * Given a class-name, create a bean. The classname-parameter can be
     * <code>null</code>, in which case the bean is created using the
     * bean-name returned by <code>getSuggestedBeanName()</code>, that is often
     * implemented by prefixing the element name with 'proto-'
     * 
     * @param ClassName
     * @return
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ConfigurationException
     */
    protected Object createBeanFromClassName(String className) 
    	throws ClassNotFoundException, InstantiationException, IllegalAccessException, ConfigurationException {
        
        String beanName;
        Class beanClass;
        
        // No explicit classname given; get bean from Spring Factory
        if (className == null) {
            beanName = getSuggestedBeanName();
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
                beanName = getSuggestedBeanName();
            } else {
                // No beans matching the type.
                // Create instance, and if the instance implements
                // Spring's BeanFactoryAware interface, use it to
                // set BeanFactory attribute on this Bean.
                return createBeanAndAutoWire(beanClass);
            }
        }
        
        // Only accept prototype-beans!
        if (isPrototypesOnly() && !factory.isPrototype(beanName)) {
            throw new ConfigurationException("Beans created from the BeanFactory must be prototype-beans, bean ["
                + beanName + "] of class [" + className + "] is not.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Creating bean with actual bean-name [" + beanName + "], bean-class [" + (beanClass != null ? beanClass.getName() : "null") + "] from Spring Bean Factory.");
        }
        return factory.getBean(beanName, beanClass);
    }

    protected Object createBeanAndAutoWire(Class beanClass) throws InstantiationException, IllegalAccessException {
        if (log.isDebugEnabled()) {
            log.debug("Bean class [" + beanClass.getName() + "] not found in Spring Bean Factory, instantiating directly and using Spring Factory for auto-wiring support.");
        }
        Object o = beanClass.newInstance();
        if (factory instanceof AutowireCapableBeanFactory) {
            ((AutowireCapableBeanFactory)factory).autowireBeanProperties(o,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME,false);
            o = ((AutowireCapableBeanFactory)factory).initializeBean(o, getSuggestedBeanName());
        } else if (o instanceof BeanFactoryAware) {
            ((BeanFactoryAware)o).setBeanFactory(factory);
        }
        return o;
    }
    
    protected Map copyAttrsToMap(Attributes attrs) {
    	Map map = new HashMap(attrs.getLength());
    	for (int i=0;i<attrs.getLength();++i) {
    		map.put(attrs.getQName(i), attrs.getValue(i));
    	}
    	return map;
    }

}
