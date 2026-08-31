package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.Description;

public class ControllerDescriptionAnnotationsTest {

	private static final String CONTROLLERS_PACKAGE = "org.frankframework.console.controllers";

	@Test
	public void allRequestMappedMethodsShouldHaveDescriptionAnnotation() throws ClassNotFoundException {
		List<String> missingDescriptions = new ArrayList<>();

		ClassPathBeanDefinitionScanner scanner = scanControllers();
		BeanDefinitionRegistry registry = scanner.getRegistry();
		int productionRestControllerCount = 0;
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			String beanClassName = beanDefinition.getBeanClassName();
			if (beanClassName == null) {
				continue;
			}

			Class<?> controllerClass = Class.forName(beanClassName);
			if (isTestClass(controllerClass)) {
				continue;
			}
			if (!CONTROLLERS_PACKAGE.equals(controllerClass.getPackageName())) {
				continue;
			}

			if (inspectControllerClass(controllerClass, missingDescriptions)) {
				productionRestControllerCount++;
			}
		}

		assertTrue(productionRestControllerCount > 0, "No production rest controllers were discovered for annotation validation");
		missingDescriptions.sort(Comparator.naturalOrder());
		assertTrue(missingDescriptions.isEmpty(), () -> "Request-mapped methods missing @Description: " + missingDescriptions);
	}

	private ClassPathBeanDefinitionScanner scanControllers() {
		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
			String className = metadataReader.getClassMetadata().getClassName();
			String packageName = className.substring(0, className.lastIndexOf('.'));
			return CONTROLLERS_PACKAGE.equals(packageName)
				&& metadataReader.getAnnotationMetadata().hasAnnotation(RestController.class.getName());
		});
		scanner.setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());

		int numberOfBeans = scanner.scan(CONTROLLERS_PACKAGE);
		assertTrue(numberOfBeans > 0, "No rest controllers were found during scanning");
		return scanner;
	}

	private boolean isTestClass(Class<?> clazz) {
		if (clazz.getProtectionDomain() == null || clazz.getProtectionDomain().getCodeSource() == null) {
			return false;
		}
		URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
		return location != null && location.toExternalForm().contains("test-classes");
	}

	private boolean inspectControllerClass(Class<?> controllerClass, List<String> missingDescriptions) {
		boolean hasRequestMappedMethod = false;
		for (Method method : controllerClass.getDeclaredMethods()) {
			if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
				hasRequestMappedMethod = true;
				if (!AnnotatedElementUtils.hasAnnotation(method, Description.class)) {
					missingDescriptions.add(controllerClass.getSimpleName() + "#" + method.toGenericString());
				}
			}
		}
		return hasRequestMappedMethod;
	}
}
