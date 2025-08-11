package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.Servlet;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.DestinationType;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IConfigurable;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.LogUtil;

public class MapPropertyDescriptorsTest {
	private final  Logger log = LogUtil.getLogger(this);

	private Iterable<String> getClassesThatImplementIConfigurable() {
		return getClassesThatImplement(IConfigurable.class);
	}

	private Iterable<String> getClassesThatImplement(Class<?> type) {
		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(type));

		// Disable servlets from being found
		scanner.addExcludeFilter(new AssignableTypeFilter(Servlet.class));

		// Disable Spring annotations
		scanner.addExcludeFilter(new AnnotationTypeFilter(IbisInitializer.class));
		scanner.addExcludeFilter(new AnnotationTypeFilter(Configuration.class));

		BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator() {
			@Override
			protected String buildDefaultBeanName(BeanDefinition definition) {
				String beanClassName = definition.getBeanClassName();
				Assert.state(beanClassName != null, "No bean class name set");
				return beanClassName;
			}
		};
		scanner.setBeanNameGenerator(beanNameGenerator);

		int numberOfBeans = scanner.scan("org.frankframework", "nl.nn.ibistesttool");
		log.debug("Found {} beans registered!", numberOfBeans);

		String[] bdn = scanner.getRegistry().getBeanDefinitionNames();
		assertEquals(numberOfBeans, bdn.length); // ensure we got all beans

		return Arrays.asList(bdn);
	}

	@Test
	public void testPropertyDescriptorsBeingRegistered() throws ClassNotFoundException, IntrospectionException {
		for (String beanName : getClassesThatImplementIConfigurable()) {
			if (beanName.endsWith(".UnloadableClass")) continue;
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
							assertNotNull(pd, "Make sure that the attribute ["+propertyName+"] has proper getter and setters in class ["+beanName+"].");
					}
				}
			}
		}
	}

	@Test
	public void testIfAllConfigurationWarningsAreDeprecated() throws ClassNotFoundException {
		for (String beanName : getClassesThatImplementIConfigurable()) {
			if (beanName.endsWith(".UnloadableClass")) continue;

			Class<?> beanClass = Class.forName(beanName);

			if (beanClass.isAnnotationPresent(ConfigurationWarning.class)) {
				assertTrue(beanClass.isAnnotationPresent(Deprecated.class), "Class " + beanName + " has ConfigurationWarning, but no Deprecation annotation");
			}
			for (Method method : beanClass.getMethods()) {
				if (method.isAnnotationPresent(ConfigurationWarning.class)) {
					assertTrue(method.isAnnotationPresent(Deprecated.class), "Method " + method.getName() + " in bean " + beanName + " has ConfigurationWarning, but no Deprecated annotation");
				}
			}
			for (Field field : beanClass.getFields()) {
				if (field.isAnnotationPresent(ConfigurationWarning.class)) {
					assertTrue(field.isAnnotationPresent(Deprecated.class), "Field " + field.getName() + " in bean " + beanName + " has ConfigurationWarning, but no Deprecated annotation");
				}
			}

		}
	}

	@Test
	public void verifyThatPhysicalDestinationHaveDestinationTypeAnnotation() throws ClassNotFoundException {
		List<Executable> executables = new ArrayList<>();
		for (String beanName : getClassesThatImplement(HasPhysicalDestination.class)) {
			Class<?> beanClass = Class.forName(beanName);
			executables.add(() -> assertNotNull(AnnotationUtils.findAnnotation(beanClass, DestinationType.class), "Class [%s] is missing the DestinationType annotation!".formatted(beanName)));
		}
		assertAll(executables);
	}
}
