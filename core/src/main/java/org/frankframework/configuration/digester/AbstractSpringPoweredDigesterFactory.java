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
package org.frankframework.configuration.digester;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.digester3.AbstractObjectCreationFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.Attributes;

import lombok.Getter;
import lombok.Setter;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;

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
public abstract class AbstractSpringPoweredDigesterFactory extends AbstractObjectCreationFactory<Object> implements ApplicationContextAware, IDigesterRuleAware {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter @Setter ApplicationContext applicationContext;
	private DigesterRule rule = null;

	protected AbstractSpringPoweredDigesterFactory() {
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
	public abstract String getSuggestedBeanName();

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
	protected Object createObject(Map<String, String> attrs) throws ClassNotFoundException {
		String classname = attrs.get("classname");
		if(StringUtils.isNotEmpty(classname)) {
			throw new IllegalArgumentException("invalid attribute [classname]. Did you mean [className]?");
		}

		String className = attrs.get("className");
		if (log.isDebugEnabled()) {
			log.debug("CreateObject: Element=[{}], name=[{}], Configured ClassName=[{}], Suggested Spring Bean Name=[{}]",
					getDigester().getCurrentElementName(), attrs.get("name"), className, getSuggestedBeanName());
		}

		if(className == null) {
			className = rule.getBeanClass();
		}

		return createBeanFromClassName(className);
	}

	private Object createBeanFromClassName(String className) throws ClassNotFoundException {
		if(applicationContext == null) {
			throw new IllegalStateException("No ApplicationContext found, unable to initialize class [" + className + "]");
		}

		if (className == null) { //See if a bean class has been defined
			String beanName = getSuggestedBeanName();
			if (!isPrototypesOnly() && !applicationContext.isSingleton(beanName)) {
				throw new IllegalStateException("bean ["+beanName+"] must be of type singleton");
			}
			return applicationContext.getBean(beanName);
		}

		ClassLoader classLoader = applicationContext.getClassLoader();
		Class<?> beanClass = Class.forName(className, true, classLoader);
		return createBeanAndAutoWire(beanClass);
	}

	protected <T> T createBeanAndAutoWire(Class<T> beanClass) {
		if (log.isDebugEnabled())
			log.debug("instantiating bean class [{}] directly using Spring Factory [{}]", beanClass.getName(), applicationContext.getDisplayName());

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

	@Override
	public final void setDigesterRule(DigesterRule rule) {
		if(this.rule != null) {
			throw new IllegalStateException("cannot override factory rule ["+this.rule+"]");
		}
		this.rule = rule;
	}
}
