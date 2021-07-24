package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertNotNull;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;

import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.util.LogUtil;

public class MapPropertyDescriptorsTest {
	private  Logger log = LogUtil.getLogger(this);

	@Test
	public void testPropertyDescriptorsBeingRegistered() throws ClassNotFoundException, IntrospectionException {
		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(IConfigurable.class));

		BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator() {
			@Override
			protected String buildDefaultBeanName(BeanDefinition definition) {
				String beanClassName = definition.getBeanClassName();
				Assert.state(beanClassName != null, "No bean class name set");
				return beanClassName;
			}
		};
		scanner.setBeanNameGenerator(beanNameGenerator);

		int numberOfBeans = scanner.scan("nl.nn.adapterframework", "nl.nn.ibistesttool");
		log.debug("Found "+numberOfBeans+" beans registered!");

		String[] names = scanner.getRegistry().getBeanDefinitionNames();
		for (String beanName : names) {
			BeanInfo beanInfo = Introspector.getBeanInfo(Class.forName(beanName));
			// get methods 
			MethodDescriptor[] methodDescriptors =  beanInfo.getMethodDescriptors();
			for (MethodDescriptor methodDescriptor : methodDescriptors) {
				String methodName = methodDescriptor.getName();
				if(methodName.startsWith("set")) {
					String propertyName = methodName.substring(3);
					String getterName = "get" + propertyName;
					String getterStartsWithIs = "is" + propertyName;

					propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);

					boolean getterMatches = Arrays.stream(methodDescriptors)
							.anyMatch(name -> name.getName().equals(getterName) || name.getName().equals(getterStartsWithIs));

					if(getterMatches) {
						PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
						PropertyDescriptor pd = Arrays.stream(pds)
							.filter(name -> name.getWriteMethod() != null && methodName.equals(name.getWriteMethod().getName()))
							.findAny()
							.orElse(null);
							assertNotNull("Make sure that the attribute ["+propertyName+"] has proper getter and setters in class ["+beanName+"].", pd);
					}
				}
			}
		}
	}
}
