/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.management.bus;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

import lombok.Setter;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;

public class MessageDispatcher implements InitializingBean, ApplicationContextAware {
	private final Logger log = LogUtil.getLogger(this);
	private @Setter String packageName;
	private @Setter ApplicationContext applicationContext;
	private MessageChannel nullChannel;

	@Override
	public void afterPropertiesSet() throws Exception {
		nullChannel = applicationContext.getBean("nullChannel", MessageChannel.class); // Messages that do not match the TopicSelector will be discarded

		ClassPathBeanDefinitionScanner scanner = scan();
		BeanDefinitionRegistry registry = scanner.getRegistry();
		if (registry == null) {
			throw new IllegalStateException("registry is null");
		}

		String[] names = registry.getBeanDefinitionNames();
		for (String beanName : names) {
			log.debug("scanning bean [{}] for ServiceActivators", beanName);

			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			findServiceActivators(beanDef);
		}
	}

	private ClassPathBeanDefinitionScanner scan() {
		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(BusAware.class));

		BeanNameGenerator beanNameGenerator = new FullyQualifiedAnnotationBeanNameGenerator();
		scanner.setBeanNameGenerator(beanNameGenerator);

		int numberOfBeans = scanner.scan(packageName);
		log.info("found [{}] BusAware beans", numberOfBeans);
		if(numberOfBeans < 1) {
			throw new IllegalStateException("did not find any BusAware beans");
		}

		return scanner;
	}

	private void findServiceActivators(BeanDefinition beanDef) throws ClassNotFoundException, IntrospectionException {
		Class<?> beanClass = getBeanClass(beanDef);

		SubscribableChannel inputChannel = findChannel(beanClass); // Validate the channel exists before continuing

		BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
		MethodDescriptor[] methodDescriptors =  beanInfo.getMethodDescriptors();
		TopicSelector classTopicSelector = AnnotationUtils.findAnnotation(beanClass, TopicSelector.class);
		Object bean = SpringUtils.createBean(applicationContext, beanClass);

		for (MethodDescriptor methodDescriptor : methodDescriptors) {
			Method method = methodDescriptor.getMethod();

			TopicSelector methodTopicSelector = AnnotationUtils.findAnnotation(method, TopicSelector.class);
			if(methodTopicSelector != null) {
				registerServiceActivator(bean, method, inputChannel, methodTopicSelector.value());
			} else if(classTopicSelector != null) {
				ActionSelector action = AnnotationUtils.findAnnotation(method, ActionSelector.class);
				if(action != null) {
					registerServiceActivator(bean, method, inputChannel, classTopicSelector.value());
				}
			}
		}
	}

	private void registerServiceActivator(Object bean, Method method, SubscribableChannel channel, BusTopic topic) {
		String componentName = ClassUtils.classNameOf(bean)+"."+method.getName();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(bean, method);
//		serviceActivator.setRequiresReply(method.getReturnType() != void.class); //forces methods to return something, but this might not be required
		serviceActivator.setComponentName(componentName);
		serviceActivator.setManagedName("@"+componentName);
		initializeBean(serviceActivator);

		MessageSelectorChain selectors = new MessageSelectorChain();
		ActionSelector action = AnnotationUtils.findAnnotation(method, ActionSelector.class);
		if(action != null) {
			selectors.add(headerSelector(action.value(), BusAction.ACTION_HEADER_NAME));
		}
		selectors.add(headerSelector(topic, BusTopic.TOPIC_HEADER_NAME));
		selectors.add(activeSelector(applicationContext));

		MessageFilter filter = new MessageFilter(selectors);
		filter.setDiscardChannel(nullChannel);
		initializeBean(filter);

		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(filter);
		handlers.add(serviceActivator);

		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setHandlers(handlers);
		chain.setComponentName(componentName);
		initializeBean(chain);
		if(channel.subscribe(chain)) {
			log.info("registered new ServiceActivator [{}] on topic [{}] with action [{}] requires-reply [{}]", componentName, topic, (action != null?action.value():"*"), method.getReturnType() != void.class);
		} else {
			log.error("unable to register ServiceActivator [{}]", componentName);
		}
	}

	private MessageSelector activeSelector(ApplicationContext applicationContext) {
		return message -> ((AbstractApplicationContext) applicationContext).isActive();
	}

	public static <E extends Enum<E>> MessageSelector headerSelector(E enumType, String headerName) {
		return message -> {
			String headerValue = (String) message.getHeaders().get(headerName);
			return enumType.name().equalsIgnoreCase(headerValue);
		};
	}

	private Class<?> getBeanClass(BeanDefinition beanDef) throws ClassNotFoundException {
		String className = beanDef.getBeanClassName();

		ClassLoader classLoader = applicationContext.getClassLoader();
		return Class.forName(className, true, classLoader);
	}

	private SubscribableChannel findChannel(Class<?> beanClass) {
		BusAware busAware = AnnotationUtils.findAnnotation(beanClass, BusAware.class);
		if(busAware == null) {
			throw new IllegalStateException("found a bean that does not implement BusAware");
		}
		String busName = busAware.value();

		return applicationContext.getBean(busName, SubscribableChannel.class);
	}

	private void initializeBean(Object bean) {
		applicationContext.getAutowireCapableBeanFactory().initializeBean(bean, bean.getClass().getCanonicalName());
	}
}
