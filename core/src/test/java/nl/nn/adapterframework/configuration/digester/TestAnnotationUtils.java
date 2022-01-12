package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

import nl.nn.adapterframework.testutil.TestAssertions;

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
		assertTrue("should mark as deprecated", isClassMyDeprecated(DeprecatedClass.class));
		assertTrue("should mark as deprecated", isClassDeprecated(DeprecatedClass.class));
	}

	@Test
	public void isClassDeprecated_OnAbstractclass() {
		assertTrue("should mark as deprecated", isClassMyDeprecated(DeprecatedAbstractClass.class));
		assertTrue("should mark as deprecated", isClassDeprecated(DeprecatedAbstractClass.class));
	}

	@Test
	public void isClassDeprecated_OnInterface() {
		assertTrue("should mark as deprecated", isClassMyDeprecated(DeprecatedInterface.class));
		assertTrue("should mark as deprecated", isClassDeprecated(DeprecatedInterface.class));
	}

	@Test
	public void isClassDeprecated_OnSuperclass() {
		assertTrue("should mark as deprecated", isClassMyDeprecated(SubClass.class));
		assertTrue("should mark as deprecated", isClassDeprecated(SubClass.class));
	}

	@Test
	public void isClassDeprecated_OnSubSubclass() {
		assertTrue("should mark as deprecated", isClassMyDeprecated(SubSubClass.class));
		assertTrue("should mark as deprecated", isClassDeprecated(SubSubClass.class));
	}

	@Test
	public void isClassDeprecated_OnClassWithInterface() {
		assertTrue("should mark as deprecated", isClassMyDeprecated(ClassWithInterface.class));
		assertFalse("is not backwards compatible", isClassDeprecated(ClassWithInterface.class));
	}

	@Test
	public void isMethodDeprecated_DirectlyOnclass() throws Exception {
		assertTrue("should mark as deprecated", isMethodMyDeprecated(DeprecatedClass.class));
		assertTrue("should mark as deprecated", isMethodDeprecated(DeprecatedClass.class));
	}

	@Test
	public void isMethodDeprecated_OnAbstractclass() throws Exception {
		assertTrue("should mark as deprecated", isMethodMyDeprecated(DeprecatedAbstractClass.class));
		assertTrue("should mark as deprecated", isMethodDeprecated(DeprecatedAbstractClass.class));
	}

	@Test
	public void isMethodDeprecated_OnInterface() throws Exception {
		assertTrue("should mark as deprecated", isMethodMyDeprecated(DeprecatedInterface.class));
		assertTrue("should mark as deprecated", isMethodDeprecated(DeprecatedInterface.class));
	}

	@Test
	public void isMethodDeprecated_OnSuperclass() throws Exception {
		assertTrue("should mark as deprecated", isMethodMyDeprecated(SubClass.class));
		assertTrue("should mark as deprecated", isMethodDeprecated(SubClass.class));
	}

	@Test
	public void isMethodDeprecated_OnSubSubclass() throws Exception {
		assertTrue("should mark as deprecated", isMethodMyDeprecated(SubSubClass.class));
		assertTrue("should mark as deprecated", isMethodDeprecated(SubSubClass.class));
	}

	@Test
	public void isMethodDeprecated_OnClassWithInterface() throws Exception {
		assertTrue("should mark as deprecated", isMethodMyDeprecated(ClassWithInterface.class));
		assertFalse("is not backwards compatible", isMethodDeprecated(ClassWithInterface.class));
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
		assumeTrue(TestAssertions.isTestRunningOnCI());

		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter(new TypeFilter() {
			@Override //Find everything that has an interface
			public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
				ClassMetadata metadata = metadataReader.getClassMetadata();
				return metadata.getInterfaceNames().length > 0;
			}
		});

		BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator() {
			@Override
			protected String buildDefaultBeanName(BeanDefinition definition) {
				String beanClassName = definition.getBeanClassName();
				Assert.state(beanClassName != null, "No bean class name set");
				return beanClassName;
			}
		};
		scanner.setBeanNameGenerator(beanNameGenerator);

		String frankFrameworkPackage = "nl.nn.adapterframework";
		int numberOfBeans = scanner.scan(frankFrameworkPackage);
		assertTrue(numberOfBeans > 0);

		Set<Class<?>> interfazes = new HashSet<>();
		String[] names = scanner.getRegistry().getBeanDefinitionNames();
		for (String beanName : names) {
			if(beanName.contains(this.getClass().getCanonicalName())) continue; //Ignore this class

			List<Class<?>> interfaces = ClassUtils.getAllInterfaces(Class.forName(beanName));
			interfazes.addAll(interfaces);
		}

		for (Class<?> interfaze : interfazes) {
			if(interfaze.getCanonicalName().startsWith(frankFrameworkPackage)) {
				for(Method method : interfaze.getDeclaredMethods()) {
					for(Annotation annotation : method.getAnnotations()) {
						if(AnnotationFilter.PLAIN.matches(annotation) || AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
							fail("Found java annotation ["+annotation+"] on interface ["+interfaze.getTypeName()+"]");
						}
					}
				}
			}
		}
	}
}