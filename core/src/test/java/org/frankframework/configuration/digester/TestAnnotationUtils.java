package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.ClassMetadata;
import org.springframework.util.Assert;

import org.frankframework.configuration.IbisManager;

/**
 * We're using AnnotationUtils.findAnnotation(method, Deprecated.class);
 * This class has been rewritten in Spring 5 breaking inherited-native java-annotation lookups on interfaces.
 *
 * Explicitly test all classes for occurrences of inherited-native java-annotations on interfaces.
 *
 */
public class TestAnnotationUtils {

	@Test
	public void isClassDeprecated_DirectlyOnclass() {
		assertTrue(isClassMyDeprecated(DeprecatedClass.class), "should mark as deprecated");
		assertTrue(isClassDeprecated(DeprecatedClass.class), "should mark as deprecated");
	}

	@Test
	public void isClassDeprecated_OnAbstractclass() {
		assertTrue(isClassMyDeprecated(DeprecatedAbstractClass.class), "should mark as deprecated");
		assertTrue(isClassDeprecated(DeprecatedAbstractClass.class), "should mark as deprecated");
	}

	@Test
	public void isClassDeprecated_OnInterface() {
		assertTrue(isClassMyDeprecated(DeprecatedInterface.class), "should mark as deprecated");
		assertTrue(isClassDeprecated(DeprecatedInterface.class), "should mark as deprecated");
	}

	@Test
	public void isClassDeprecated_OnSuperclass() {
		assertTrue(isClassMyDeprecated(SubClass.class), "should mark as deprecated");
		assertTrue(isClassDeprecated(SubClass.class), "should mark as deprecated");
	}

	@Test
	public void isClassDeprecated_OnSubSubclass() {
		assertTrue(isClassMyDeprecated(SubSubClass.class), "should mark as deprecated");
		assertTrue(isClassDeprecated(SubSubClass.class), "should mark as deprecated");
	}

	@Test
	public void isClassDeprecated_OnClassWithInterface() {
		assertTrue(isClassMyDeprecated(ClassWithInterface.class), "should mark as deprecated");
		assertFalse(isClassDeprecated(ClassWithInterface.class), "is not backwards compatible");
	}

	@Test
	public void isMethodDeprecated_DirectlyOnclass() throws Exception {
		assertTrue(isMethodMyDeprecated(DeprecatedClass.class), "should mark as deprecated");
		assertTrue(isMethodDeprecated(DeprecatedClass.class), "should mark as deprecated");
	}

	@Test
	public void isMethodDeprecated_OnAbstractclass() throws Exception {
		assertTrue(isMethodMyDeprecated(DeprecatedAbstractClass.class), "should mark as deprecated");
		assertTrue(isMethodDeprecated(DeprecatedAbstractClass.class), "should mark as deprecated");
	}

	@Test
	public void isMethodDeprecated_OnInterface() throws Exception {
		assertTrue(isMethodMyDeprecated(DeprecatedInterface.class), "should mark as deprecated");
		assertTrue(isMethodDeprecated(DeprecatedInterface.class), "should mark as deprecated");
	}

	@Test
	public void isMethodDeprecated_OnSuperclass() throws Exception {
		assertTrue(isMethodMyDeprecated(SubClass.class), "should mark as deprecated");
		assertTrue(isMethodDeprecated(SubClass.class), "should mark as deprecated");
	}

	@Test
	public void isMethodDeprecated_OnSubSubclass() throws Exception {
		assertTrue(isMethodMyDeprecated(SubSubClass.class), "should mark as deprecated");
		assertTrue(isMethodDeprecated(SubSubClass.class), "should mark as deprecated");
	}

	@Test
	public void isMethodDeprecated_OnClassWithInterface() throws Exception {
		assertTrue(isMethodMyDeprecated(ClassWithInterface.class), "should mark as deprecated");
		assertFalse(isMethodDeprecated(ClassWithInterface.class), "is not backwards compatible");
	}

	private static boolean isClassMyDeprecated(Class<?> clazz) {
		return null != AnnotationUtils.findAnnotation(clazz, MyDeprecated.class);
	}

	private static boolean isMethodMyDeprecated(Class<?> clazz) throws Exception {
		Method method = getMethodFromClass(clazz);
		return null != AnnotationUtils.findAnnotation(method, MyDeprecated.class);
	}

	private static boolean isClassDeprecated(Class<?> clazz) {
		return null != AnnotationUtils.findAnnotation(clazz, Deprecated.class);
	}

	private static boolean isMethodDeprecated(Class<?> clazz) throws Exception {
		Method method = getMethodFromClass(clazz);
		return null != AnnotationUtils.findAnnotation(method, Deprecated.class);
	}

	private static Method getMethodFromClass(Class<?> clazz) throws Exception {
		try {
			return clazz.getMethod("testMethod", (Class<?>[]) null);
		} catch (Exception e) {
			return clazz.getSuperclass().getMethod("testMethod", (Class<?>[]) null);
		}
	}

	@MyDeprecated
	@Deprecated
	static interface DeprecatedInterface {

		@MyDeprecated
		@Deprecated
		void testMethod();
	}

	@MyDeprecated
	@Deprecated
	static class DeprecatedClass {

		@MyDeprecated
		@Deprecated
		public void testMethod() {};
	}

	@MyDeprecated
	@Deprecated
	abstract static class DeprecatedAbstractClass {

		@MyDeprecated
		@Deprecated
		public void testMethod() {}
	}

	static class SubClass extends SuperClass {
	}

	static class SubSubClass extends SubClass {
	}

	static class ClassWithInterface implements DeprecatedInterface {

		@Override
		public void testMethod() {}
	}

	@MyDeprecated
	@Deprecated
	static class SuperClass {

		@MyDeprecated
		@Deprecated
		public void testMethod() {}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface MyDeprecated {
	}

	@Test
	public void findInterfacesWithAnnotations() throws Exception {
		//assumeTrue(TestAssertions.isTestRunningOnCI());

		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		//Find everything that has an interface
		scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
			ClassMetadata metadata = metadataReader.getClassMetadata();
			return metadata.getInterfaceNames().length > 0;
		});

		BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator() {
			@Override
			protected String buildDefaultBeanName(@Nonnull BeanDefinition definition) {
				String beanClassName = definition.getBeanClassName();
				Assert.state(beanClassName != null, "No bean class name set");
				return beanClassName;
			}
		};
		scanner.setBeanNameGenerator(beanNameGenerator);

		String frankFrameworkPackage = "org.frankframework";
		int numberOfBeans = scanner.scan(frankFrameworkPackage);
		assertTrue(numberOfBeans > 0);

		Set<Class<?>> interfazes = new HashSet<>();
		String[] names = scanner.getRegistry().getBeanDefinitionNames();
		for (String beanName : names) {
			if(beanName.contains(this.getClass().getCanonicalName())
					|| beanName.startsWith("org.frankframework.credentialprovider")
					|| beanName.endsWith(".UnloadableClass")
			) continue; //Ignore this class, the "unloadable" test-class, and also credential provider classes because they use optional dependencies not on our classpath.

			List<Class<?>> interfaces = ClassUtils.getAllInterfaces(Class.forName(beanName));
			interfazes.addAll(interfaces);
		}

		Set<String> interfacesToSkip = new HashSet<>();
		interfacesToSkip.add(IbisManager.class.getCanonicalName());

		for (Class<?> interfaze : interfazes) {
			if(interfaze.getCanonicalName().startsWith(frankFrameworkPackage)
					&& !interfacesToSkip.contains(interfaze.getCanonicalName())) {
				for(Method method : interfaze.getDeclaredMethods()) {
					for(Annotation annotation : method.getAnnotations()) {
						if(AnnotationFilter.PLAIN.matches(annotation) || AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
							fail("Found java annotation ["+annotation+"] on interface ["+interfaze.getTypeName()+"], is not seen by digester because it uses Spring AnnotationUtils");
						}
					}
				}
			}
		}
	}
}
