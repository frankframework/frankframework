/*
   Copyright 2024 WeAreFrank!

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
import java.util.Optional;
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

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.xml.FullXmlFilter;

@Log4j2
public class Digester extends FullXmlFilter implements InitializingBean, ApplicationContextAware {
	private final Set<DigesterRule> parsedPatterns = new HashSet<>();
	private Deque<String> elementNames = new ArrayDeque<>(); // XML element names
	private Deque<BeanRuleWrapper> elementBeans = new ArrayDeque<>(); // Beans conform found element names, TODO turn into IConfigurable
	private Deque<ApplicationContext> applicationContext = new ArrayDeque<>(); // Context Stack
	private Map<DigesterRule, AbstractSpringPoweredDigesterFactory> beanFactories = new ConcurrentHashMap<>();
	private ValidateAttributeRule handleAttributeRule;
	private Locator locator;

	record BeanRuleWrapper (DigesterRule rule, Object bean) {}

	public void setApplicationContext(ApplicationContext ac) {
		applicationContext.push(ac); // Should be the first item on the stack, stack should never be empty.
		elementBeans.push(new BeanRuleWrapper(null, ac));
	}

	private ApplicationContext getCurrentApplicationContext() {
		if (applicationContext.isEmpty()) {
			throw new IllegalStateException("applicationContext stack should never be empty!");
		}
		return applicationContext.peek();
	}

	public void setParsedPatterns(Map<String, DigesterRule> parsedPatterns) {
		this.parsedPatterns.addAll(parsedPatterns.values());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		handleAttributeRule = SpringUtils.createBean(getCurrentApplicationContext(), ValidateAttributeRule.class);
		handleAttributeRule.setDigester(this);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		elementNames.push(localName);
		DigesterRule rule = findRuleForLocalName().orElseThrow(() -> new SAXException("found element [%s] with no matching pattern".formatted(localName)));
		if (StringUtils.isNotEmpty(rule.getRegisterTextMethod())) {
			return; // Handle in characters
		}

		try {
			AbstractSpringPoweredDigesterFactory factory = getFactoryForRule(rule);
			if (factory != null) { // Only the Configuration element doesn't have a factory
				factory.setApplicationContext(getCurrentApplicationContext());
				Object bean = factory.createObject(atts);
				if (bean instanceof ApplicationContext a) {
					applicationContext.push(a);
				}
				elementBeans.push(new BeanRuleWrapper(rule, bean));
			}

			handleAttributeRule.begin(uri, localName, atts);
		} catch (Exception e) {
			throw new SAXException("unable to create bean", e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// TODO handle getRegisterTextMethod
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			handleAttributeRule.end(uri, localName);

			String poppedElementNamed = elementNames.pop();
			if (!localName.equals(poppedElementNamed)) {
				throw new SAXException("whoopsie, this is bad");
			}

			BeanRuleWrapper beanWrapper = elementBeans.pop();
			DigesterRule rule = beanWrapper.rule();

			if (rule != null && StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				Object parent = elementBeans.peek().bean();
				ClassUtils.invokeSetter(parent, rule.getRegisterMethod(), beanWrapper.bean());
			}

			if (beanWrapper.bean() instanceof ApplicationContext) {
				applicationContext.pop();
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new SAXException(e);
		}
	}

	private AbstractSpringPoweredDigesterFactory getFactoryForRule(DigesterRule rule) {
		return beanFactories.computeIfAbsent(rule, r -> {
			AbstractSpringPoweredDigesterFactory factory = createFactory(r.getFactory());
			if (factory != null) {
				factory.setDigesterRule(rule);
				factory.setDigester(this);
			}
			return factory;
		});
	}

	@Nonnull
	private Optional<DigesterRule> findRuleForLocalName() {
		return parsedPatterns.stream().filter(this::matchesRule).findFirst();
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

	public String getCurrentElementName() {
		return elementNames.peek();
	}

	public Locator getDocumentLocator() {
		return locator;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	public Object peek() {
		return elementBeans.peek().bean();
	}
}
