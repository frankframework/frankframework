/*
   Copyright 2024-2025 WeAreFrank!

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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.xml.FullXmlFilter;

@Log4j2
public class Digester extends FullXmlFilter implements InitializingBean, ApplicationContextAware {
	private final Set<DigesterRule> parsedPatterns = new HashSet<>();

	private final Deque<String> elementNames = new ArrayDeque<>(); // XML element names
	private final Deque<BeanRuleWrapper> elementBeans = new ArrayDeque<>(); // Beans conform found element names
	private final Deque<ApplicationContext> applicationContext = new ArrayDeque<>(); // Context Stack
	private final Map<DigesterRule, IDigesterFactory> beanFactories = new ConcurrentHashMap<>();

	private ValidateAttributeRule handleAttributeRule;
	private @Getter @Setter Locator documentLocator;

	record BeanRuleWrapper (DigesterRule rule, @Nonnull Object bean) {}

	/**
	 * This Class is created via the Configuration context. This is also the first element to populate.
	 */
	@Override
	public void setApplicationContext(ApplicationContext ac) {
		applicationContext.push(ac); // Should be the first item on the stack, stack should never be empty.
		elementBeans.push(new BeanRuleWrapper(null, ac));
	}

	/**
	 * Allows beans to be created via the `top` level context.
	 * <p>
	 * Eg. pipes are created via the AdapterContext,
	 * monitoring, jmsRealms and jobs are created via the ConfigurationContext
	 * </p>
	 */
	private ApplicationContext getCurrentApplicationContext() {
		if (applicationContext.isEmpty()) {
			throw new IllegalStateException("ApplicationContext stack should never be empty!");
		}
		return applicationContext.peek();
	}

	/**
	 * List of DigesterRules that apply to elements created through this {@link Digester}.
	 */
	public void setParsedPatterns(Map<String, DigesterRule> parsedPatterns) {
		this.parsedPatterns.addAll(parsedPatterns.values());
	}

	@Nullable
	public Object peek() {
		if (!elementBeans.isEmpty()) {
			return elementBeans.peek().bean();
		}

		// Though this should technically never happen. Allow NULL to be returned.
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		handleAttributeRule = SpringUtils.createBean(getCurrentApplicationContext());
		handleAttributeRule.setDigester(this);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		log.debug("startElement: found element [{}]", localName);
		elementNames.push(localName);

		if (elementNames.size() != 1 && "configuration".equals(localName)) {
			log.debug("skipping erroneous nested configuration element");
			// This is a bit of a strange scenario but we allow Configuration to be a root tag.
			// Due to this, it's possible to include files with the Configuration root tag in another
			// Configuration file. This does not directly do anything, as there is not factory, so ignore the element.
		} else {
			DigesterRule rule = findRuleForLocalName(localName);
			if (StringUtils.isNotEmpty(rule.getRegisterTextMethod())) {
				// Capture the character data first.
				elementBeans.push(new BeanRuleWrapper(rule, new StringBuilder()));
			} else {
				createBeanThroughFactory(rule, localName, atts);
			}
		}

		super.startElement(uri, localName, qName, atts);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (peek() instanceof StringBuilder buffer) {
			buffer.append(ch, start, length);
			log.debug("characters: appending character data [{}]", buffer);
		}

		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		log.debug("endElement: register bean [{}]", localName);

		String poppedElementNamed = elementNames.pop();
		if (!localName.equals(poppedElementNamed)) {
			throw new SAXException("local element [%s] do not match stack element [%s]".formatted(localName, poppedElementNamed));
		}

		if (elementNames.size() != 1 && "configuration".equals(localName)) {
			// See startElement where we ignore additional configuration elements
			log.debug("skipping erroneous nested configuration element");
		} else {
			handleAttributeRule.end(localName);

			BeanRuleWrapper beanWrapper = elementBeans.pop();
			registerDigestedBeanOnParent(beanWrapper);
			if (beanWrapper.bean() instanceof ApplicationContext) {
				applicationContext.pop();
			}
		}

		super.endElement(uri, localName, qName);
	}

	/**
	 * Using the appropriate {@link DigesterRule} it attempts to create the bean using the top-level ApplicationContext.
	 * This can either be a {@link Configuration} or {@link Adapter}.
	 * Once created it uses the {@link ValidateAttributeRule} to call all setters and populate the bean properties.
	 * <p>
	 * The newly created bean is not yet added to the parent, see {@link #endElement(String, String, String)}.
	 */
	private void createBeanThroughFactory(DigesterRule rule, String localName, Attributes atts) throws SAXException {
		try {
			IDigesterFactory factory = getFactoryForRule(rule);
			if (factory != null) { // Only the Configuration element doesn't have a factory
				Object bean = factory.createBean(getCurrentApplicationContext(), atts);
				if (bean instanceof ApplicationContext a) {
					applicationContext.push(a);
				}
				elementBeans.push(new BeanRuleWrapper(rule, bean));
			}
		} catch (Exception e) {
			throw new SAXException("unable to create bean for element [%s] using DigesterRule [%s]".formatted(localName, rule), e);
		}

		try {
			handleAttributeRule.begin(localName, atts);
		} catch (Exception e) {
			throw new SAXException("unable to populate bean attributes for element [%s]".formatted(localName), e);
		}
	}

	/**
	 * Verify if the target bean has a rule assigned to it, and if so register it with it's parent.
	 */
	private void registerDigestedBeanOnParent(BeanRuleWrapper beanWrapper) throws SAXException {
		DigesterRule rule = beanWrapper.rule();
		if (rule == null) {
			return; // Technically this is possible for the first rule...
		}

		final Object parent = elementBeans.peek().bean();
		try {
			if (StringUtils.isNotEmpty(rule.getRegisterTextMethod())) {
				if (beanWrapper.bean() instanceof StringBuilder buffer) {
					ClassUtils.invokeSetter(parent, rule.getRegisterTextMethod(), buffer.toString());
				}
			} else if (StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				final Object bean = beanWrapper.bean();
				ClassUtils.invokeSetter(parent, rule.getRegisterMethod(), bean);
			}
		} catch (Exception e) {
			throw new SAXException("unable to register text or bean on parent [%s]".formatted(parent), e);
		}
	}

	/**
	 * Keep a local cache of all factories, so they can be reused.
	 */
	private IDigesterFactory getFactoryForRule(DigesterRule rule) {
		return beanFactories.computeIfAbsent(rule, r -> {
			IDigesterFactory factory = createFactory(r.getFactory());
			if (factory != null) {
				factory.setDigesterRule(rule);
				factory.setDigester(this);
			}
			return factory;
		});
	}

	@Nonnull
	private DigesterRule findRuleForLocalName(String localName) throws SAXException {
		return parsedPatterns.stream()
				.filter(this::matchesRule)
				.findFirst()
				.orElseThrow(() -> new SAXException("found element [%s] with no matching pattern".formatted(localName)));
	}

	private boolean matchesRule(DigesterRule rule) {
		List<String> split = StringUtil.split(rule.getPattern(), "/");
		Collections.reverse(split);
		Deque<String> localQ = new ArrayDeque<>(elementNames);
		for (String element : split) {
			if (element.equals("*")) continue;
			if (localQ.isEmpty() || !element.equals(localQ.pop())) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	private AbstractSpringPoweredDigesterFactory createFactory(String factory) {
		if ("null".equals(factory)) { // Check against a null-string when you don't want to use the default factory
			log.trace("NULL factory specified, skip factory registration");
			return null;
		} else if (StringUtils.isNotEmpty(factory)) {
			Object object;
			try {
				log.trace("attempting to create new factory of class [{}]", factory);
				Class<?> clazz = ClassUtils.loadClass(factory);
				object = SpringUtils.createBean(applicationContext.peekLast(), clazz); // Wire the factory through Spring
			} catch (Exception e) {
				throw new IllegalArgumentException("factory [" + factory + "] not found", e);
			}
			if (object instanceof AbstractSpringPoweredDigesterFactory creationFactory) {
				return creationFactory;
			}
			throw new IllegalArgumentException("factory type must implement ObjectCreationFactory");
		}
		log.trace("no factory specified, returing default [{}]", GenericFactory.class::getCanonicalName);
		return SpringUtils.createBean(applicationContext.peekLast(), GenericFactory.class); // Wire the factory through Spring
	}
}
