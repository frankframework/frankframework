/*
   Copyright 2022-2026 WeAreFrank!

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
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.support.GenericApplicationContext;
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
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;

import lombok.Setter;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;

/**
 * Scans the classpath for beans annotated with {@link BusAware} and registers their methods annotated with {@link TopicSelector} as service activators on the specified bus channel.
 * We cannot use AnnotationConfigApplicationContext because we cannot configure the scanner to only scan for beans annotated with {@link BusAware}.
 */
public class MessageDispatcher extends GenericApplicationContext implements InitializingBean, ApplicationContextAware {

	private final ClassPathBeanDefinitionScanner scanner;

	private final Logger log = LogUtil.getLogger(this);
	private @Setter String packageName;
	private @Setter ApplicationContext applicationContext;
	private MessageChannel nullChannel;

	MessageDispatcher() {
		this.scanner = createScanner();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setParent(applicationContext);

		// Equivalent of proxy-target-class="true": auto-proxying using CGLIB.
		AopConfigUtils.registerAutoProxyCreatorIfNecessary(this);
		AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(this);

		// Register the JSR-250 AuthorizationMethodInterceptor to enable @RolesAllowed annotations on service activator methods.
		registerBean("jsr250AuthorizationMethodInterceptor",
				AuthorizationManagerBeforeMethodInterceptor.class,
				(Supplier<AuthorizationManagerBeforeMethodInterceptor>) AuthorizationManagerBeforeMethodInterceptor::jsr250,
				bd -> bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));

		// Find all beans annotated with @BusAware.
		scan();

		// Initialize the context so the AOP config is loaded.
		refresh();

		nullChannel = getBean("nullChannel", MessageChannel.class); // Messages that do not match the TopicSelector will be discarded

		// Register @BusAware beans as service activators.
		String[] names = getBeanDefinitionNames();
		for (String beanName : names) {
			log.debug("scanning bean [{}] for ServiceActivators", beanName);

			BeanDefinition beanDef = getBeanDefinition(beanName);
			Class<?> beanClass = getBeanClass(beanDef);

			findServiceActivators(beanClass);
		}
	}

	private void scan() {
		int numberOfBeans = scanner.scan(packageName);
		log.debug("found [{}] BusAware beans", numberOfBeans);
		if(numberOfBeans < 1) {
			throw new IllegalStateException("did not find any BusAware beans");
		}
	}

	private ClassPathBeanDefinitionScanner createScanner() {
		ClassPathBeanDefinitionScanner cpDefScanner = new ClassPathBeanDefinitionScanner(this);
		cpDefScanner.setIncludeAnnotationConfig(false);
		cpDefScanner.addIncludeFilter(new AnnotationTypeFilter(BusAware.class));

		BeanNameGenerator beanNameGenerator = new FullyQualifiedAnnotationBeanNameGenerator();
		cpDefScanner.setBeanNameGenerator(beanNameGenerator);

		return cpDefScanner;
	}

	private void findServiceActivators(Class<?> beanClass) throws IntrospectionException {
		SubscribableChannel inputChannel = findChannel(beanClass); // Validate the channel exists before continuing
		if (inputChannel == null) {
			return;
		}

		BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
		MethodDescriptor[] methodDescriptors =  beanInfo.getMethodDescriptors();
		TopicSelector classTopicSelector = AnnotationUtils.findAnnotation(beanClass, TopicSelector.class);
		Object bean = SpringUtils.createBean(this, beanClass);

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
//		serviceActivator.setRequiresReply(method.getReturnType() != void.class); // forces methods to return something, but this might not be required
		serviceActivator.setComponentName(componentName);
		serviceActivator.setManagedName("@"+componentName);
		initializeBean(serviceActivator, componentName);

		MessageSelectorChain selectors = new MessageSelectorChain();
		ActionSelector action = AnnotationUtils.findAnnotation(method, ActionSelector.class);
		if(action != null) {
			selectors.add(headerSelector(action.value(), BusAction.ACTION_HEADER_NAME));
		}
		selectors.add(headerSelector(topic, BusTopic.TOPIC_HEADER_NAME));
		selectors.add(activeSelector());

		MessageFilter filter = new MessageFilter(selectors);
		filter.setDiscardChannel(nullChannel);
		initializeBean(filter, componentName+".filter");

		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(filter);
		handlers.add(serviceActivator);

		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setHandlers(handlers);
		initializeBean(chain, componentName+".chain");
		if(channel.subscribe(chain)) {
			log.debug("registered new ServiceActivator [{}] on topic [{}] with action [{}] requires-reply [{}]", componentName, topic, (action != null?action.value():"*"), method.getReturnType() != void.class);
		} else {
			log.error("unable to register ServiceActivator [{}]", componentName);
		}
	}

	private MessageSelector activeSelector() {
		return message -> this.isActive();
	}

	public static <E extends Enum<E>> MessageSelector headerSelector(E enumType, String headerName) {
		return message -> {
			String headerValue = (String) message.getHeaders().get(headerName);
			return enumType.name().equalsIgnoreCase(headerValue);
		};
	}

	private Class<?> getBeanClass(BeanDefinition beanDef) throws ClassNotFoundException {
		String className = beanDef.getBeanClassName();

		ClassLoader classLoader = Objects.requireNonNull(getClassLoader(), "no classloader found");
		return Class.forName(className, true, classLoader);
	}

	private @Nullable SubscribableChannel findChannel(Class<?> beanClass) {
		BusAware busAware = AnnotationUtils.findAnnotation(beanClass, BusAware.class);
		if(busAware == null) {
			return null;
		}

		String busName = busAware.value();
		return getBean(busName, SubscribableChannel.class);
	}

	private <T> void initializeBean(T bean, String componentName) {
		getAutowireCapableBeanFactory().initializeBean(bean, componentName);

		// And register the bean definition so it will partake in the lifecycle of the application context.
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(() -> bean);
		beanDefinition.setBeanClass(bean.getClass());
		beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
		registerBeanDefinition(componentName, beanDefinition);
	}
}
