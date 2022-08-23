package nl.nn.adapterframework.management.bus;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.Message;
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

	public MessageDispatcher() {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ClassPathBeanDefinitionScanner scanner = scan();
		sopmt(scanner);
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

	private void sopmt(ClassPathBeanDefinitionScanner scanner) throws ClassNotFoundException, IntrospectionException {
		String[] names = scanner.getRegistry().getBeanDefinitionNames();
		for (String beanName : names) {
			log.info("scanning bean [{}] for ServiceActivators", beanName);
			BeanDefinition beanDef = scanner.getRegistry().getBeanDefinition(beanName);
			String className = beanDef.getBeanClassName();

			ClassLoader classLoader = applicationContext.getClassLoader();
			Class<?> beanClass = Class.forName(className, true, classLoader);
			BusAware busAware = AnnotationUtils.findAnnotation(beanClass, BusAware.class);
			if(busAware == null) {
				throw new IllegalStateException("found a bean that does not implement BusAware");
			}
			String busName = busAware.value();
			SubscribableChannel inputChannel = applicationContext.getBean(busName, SubscribableChannel.class);
			MessageChannel nullChannel = applicationContext.getBean("nullChannel", MessageChannel.class);

			BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
			MethodDescriptor[] methodDescriptors =  beanInfo.getMethodDescriptors();
			Object bean = SpringUtils.createBean(applicationContext, beanClass);
			for (MethodDescriptor methodDescriptor : methodDescriptors) {
				Method method = methodDescriptor.getMethod();

				HeaderSelector selector = AnnotationUtils.findAnnotation(method, HeaderSelector.class);
				if(selector != null) {
					BusTopic busAction = selector.value();
					MessageSelector msel = new MessageSelector() {

						@Override
						public boolean accept(Message<?> message) {
							String action = (String) message.getHeaders().get("action");

							return busAction.name().equalsIgnoreCase(action);
						}

					};
					MessageFilter filter = new MessageFilter(msel);
					filter.setDiscardChannel(nullChannel);
//					SpringUtils.autowireByName(applicationContext, filter);
					applicationContext.getAutowireCapableBeanFactory().initializeBean(filter, beanName);
					ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(bean, method);
					serviceActivator.setRequiresReply(method.getReturnType() != null);
//					SpringUtils.autowireByType(applicationContext, serviceActivator);
					applicationContext.getAutowireCapableBeanFactory().initializeBean(serviceActivator, serviceActivator.getClass().getCanonicalName());

					MessageHandlerChain chain = new MessageHandlerChain();
					List<MessageHandler> handlers = new ArrayList<>();
					handlers.add(filter);
					handlers.add(serviceActivator);
					chain.setHandlers(handlers);
//					SpringUtils.autowireByType(applicationContext, chain);
					applicationContext.getAutowireCapableBeanFactory().initializeBean(chain, chain.getClass().getCanonicalName());
					inputChannel.subscribe(chain);
				}
			}
		}
	}
}
