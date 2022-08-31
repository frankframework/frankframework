/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.bus;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

import lombok.Setter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;

public class MessageDispatcher implements InitializingBean, ApplicationContextAware {
	private Logger log = LogUtil.getLogger(this);
	private @Setter String packageName;
	private @Setter BeanFactory beanFactory;
	private @Setter ApplicationContext applicationContext;
	private MessageChannel nullChannel;
	private EnumMap<BusTopic, MessageFilter> messageTopicFilters = new EnumMap<>(BusTopic.class);

	@Override
	public void afterPropertiesSet() throws Exception {
		nullChannel = applicationContext.getBean("nullChannel", MessageChannel.class); //Messages that do not match the TopicSelector will be discarded

		ClassPathBeanDefinitionScanner scanner = scan();
		String[] names = scanner.getRegistry().getBeanDefinitionNames();
		for (String beanName : names) {
			log.info("scanning bean [{}] for ServiceActivators", beanName);
			BeanDefinition beanDef = scanner.getRegistry().getBeanDefinition(beanName);
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

		SubscribableChannel inputChannel = findChannel(beanClass); //Validate the channel exists before continuing

		BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
		MethodDescriptor[] methodDescriptors =  beanInfo.getMethodDescriptors();
		Object bean = SpringUtils.createBean(applicationContext, beanClass);

		for (MethodDescriptor methodDescriptor : methodDescriptors) {
			Method method = methodDescriptor.getMethod();

			TopicSelector selector = AnnotationUtils.findAnnotation(method, TopicSelector.class);
			if(selector != null) {
				registerServiceActivator(bean, method, inputChannel, selector.value());
			}
		}
	}

	private void registerServiceActivator(Object bean, Method method, SubscribableChannel channel, BusTopic topic) {
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(bean, method);
		serviceActivator.setRequiresReply(method.getReturnType() != void.class);
		initializeBean(serviceActivator);

		MessageHandlerChain chain = new MessageHandlerChain();
		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(getMessageTopicFilter(topic));
		handlers.add(serviceActivator);
		chain.setHandlers(handlers);
		initializeBean(chain);
		channel.subscribe(chain);
	}

	//Multiple methods can subscribe to the same BusTopic, filters are re-used
	private MessageFilter getMessageTopicFilter(BusTopic topic) {
		return messageTopicFilters.computeIfAbsent(topic, this::compute);
	}

	private MessageFilter compute(BusTopic topic) {
		MessageFilter filter = new MessageFilter(message -> {
			String action = (String) message.getHeaders().get(TopicSelector.TOPIC_HEADER_NAME);
			return topic.name().equalsIgnoreCase(action);
		});

		filter.setDiscardChannel(nullChannel);
		initializeBean(filter);
		return filter;
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

	private final void initializeBean(Object bean) {
		applicationContext.getAutowireCapableBeanFactory().initializeBean(bean, bean.getClass().getCanonicalName());
	}
}
