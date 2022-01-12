/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.configuration.digester;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.digester3.AbstractObjectCreationFactory;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.Attributes;

import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;

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
 *
 */
public abstract class AbstractSpringPoweredDigesterFactory extends AbstractObjectCreationFactory<Object> implements ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);
	private @Setter ApplicationContext applicationContext;

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
	@Override
	public Object createObject(Attributes attrs) throws Exception {
		Map<String, String> attrMap = copyAttrsToMap(attrs);
		return createObject(attrMap);
	}

	/**
	 * Create Object from Spring factory, but using the attributes
	 * from the XML converted to a Map. This is so that sub-classes
	 * can override this method and change attributes in the map
	 * before creating the object from the Spring factory.
	 */
	protected Object createObject(Map<String, String> attrs) throws Exception {
		String className = attrs.get("className");
		if (log.isDebugEnabled()) {
			log.debug("CreateObject: Element=[" + getDigester().getCurrentElementName()
					+ "], name=[" + attrs.get("name")
					+ "], Configured ClassName=[" + className
					+ "], Suggested Spring Bean Name=[" + getSuggestedBeanName() + "]");
		}

		return createBeanFromClassName(className);
	}

	/**
	 * Given a class-name, create a bean. The classname-parameter can be
	 * <code>null</code>, in which case the bean is created using the
	 * bean-name returned by <code>getSuggestedBeanName()</code>, that is often
	 * implemented by prefixing the element name with 'proto-'
	 */
	protected Object createBeanFromClassName(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ConfigurationException {
		if(applicationContext == null) {
			throw new IllegalStateException("No ApplicationContext found, unable to initialize class [" + className + "]");
		}

        String beanName;
        Class<?> beanClass;

        // No explicit className given; get bean from Spring Factory
        if (className == null) {
            if (log.isDebugEnabled()) {
                log.debug("createBeanFromClassName(): className is null");
            }
            beanName = getSuggestedBeanName();
            beanClass = null;
        } else {
            // Get all beans matching the className given
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            beanClass = Class.forName(className, true, classLoader);

            String[] matchingBeans = applicationContext.getBeanNamesForType(beanClass);
            if (matchingBeans.length == 1) {
                // Only 1 bean of this type, so create it
                beanName = matchingBeans[0];
                if (log.isDebugEnabled()) {
                    log.debug("createBeanFromClassName(): only bean ["+beanName+"] matches class ["+beanClass.getName()+"]");
                }
            } else if (matchingBeans.length > 1) {
                // multiple beans; find if there's one with the
                // same name as from 'getBeanName'.
                beanName = getSuggestedBeanName();
                if (log.isDebugEnabled()) {
                    log.debug("createBeanFromClassName(): multiple beans match class ["+beanClass.getName()+"], using suggested ["+beanName+"]");
                }
            } else {
                // No beans matching the type.
                // Create instance, and if the instance implements
                // Spring's BeanFactoryAware interface, use it to
                // set BeanFactory attribute on this Bean.
                if (log.isDebugEnabled()) {
                    log.debug("createBeanFromClassName(): no beans match class ["+beanClass.getName()+"]");
                }
//               	try {
//	               	Object o=ibisContext.getBean(getSuggestedBeanName(), beanClass);
//	               	if (o!=null) {
//	                   	if (DEBUG && log.isDebugEnabled()) log.debug("createBeanFromClassName(): found bean ["+o+"] by suggested name ["+getSuggestedBeanName()+"] match class ["+beanClass.getName()+"]");
//	               		return o;
//	               	}
//               	} catch (NoSuchBeanDefinitionException e) {
//                  	if (DEBUG && log.isDebugEnabled()) log.debug("createBeanFromClassName(): no bean ["+getSuggestedBeanName()+"] found for class ["+beanClass.getName()+"]: "+e.getMessage());
//               	} catch (BeanNotOfRequiredTypeException e) {
//                  	if (DEBUG && log.isDebugEnabled()) log.debug("createBeanFromClassName(): bean ["+getSuggestedBeanName()+"] found for class ["+beanClass.getName()+"]: "+e.getMessage());
//               	}
                return createBeanAndAutoWire(beanClass);
            }
        }

        // Only accept prototype-beans!
        if (isPrototypesOnly() && !applicationContext.isPrototype(beanName)) {
            throw new ConfigurationException("Beans created from the BeanFactory must be prototype-beans, bean ["
                + beanName + "] of class [" + className + "] is not.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Creating bean with actual bean-name [" + beanName + "], bean-class [" + (beanClass != null ? beanClass.getName() : "null") + "] from Spring Bean Factory.");
        }
        return applicationContext.getBean(beanName, beanClass);
    }

	protected <T> T createBeanAndAutoWire(Class<T> beanClass) throws InstantiationException, IllegalAccessException {
		if (log.isDebugEnabled()) {
			log.debug("Bean class [" + beanClass.getName() + "], autowire bean name [" + getSuggestedBeanName() + "] not found in Spring Bean Factory, instantiating directly and using Spring Factory ["+applicationContext.getDisplayName()+"] for auto-wiring support.");
		}

		return SpringUtils.createBean(applicationContext, beanClass); //Autowire and initialize the bean through Spring
	}

	protected Map<String, String> copyAttrsToMap(Attributes attrs) {
		Map<String, String> map = new HashMap<>(attrs.getLength());
		for (int i = 0; i < attrs.getLength(); ++i) {
			String value = attrs.getValue(i);
			map.put(attrs.getQName(i), value);
		}
		return map;
	}

}
