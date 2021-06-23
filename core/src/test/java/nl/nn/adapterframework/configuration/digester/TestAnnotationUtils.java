package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * We're using AnnotationUtils.findAnnotation(method, Deprecated.class);
 * This class has been rewritten in Spring 5 breaking inherited-native java-annotation lookups.
 * @author Niels
 *
 */
public class TestAnnotationUtils {

	@Test
	public void isClassDeprecated_DirectlyOnclass() {
		assertTrue("should mark as deprecated", isClassDeprecated(DeprecatedClass.class));
		assertTrue("should mark as deprecated", isClassMyDeprecated(DeprecatedClass.class));
	}

	@Test
	public void isClassDeprecated_OnAbstractclass() {
		assertTrue("should mark as deprecated", isClassDeprecated(DeprecatedAbstractClass.class));
		assertTrue("should mark as deprecated", isClassMyDeprecated(DeprecatedAbstractClass.class));
	}

	@Test
	public void isClassDeprecated_OnInterface() {
		assertTrue("should mark as deprecated", isClassDeprecated(DeprecatedInterface.class));
		assertTrue("should mark as deprecated", isClassMyDeprecated(DeprecatedInterface.class));
	}

	@Test
	public void isClassDeprecated_OnSuperclass() {
		assertTrue("should mark as deprecated", isClassDeprecated(SubClass.class));
		assertTrue("should mark as deprecated", isClassMyDeprecated(SubClass.class));
	}

	@Test
	public void isClassDeprecated_OnClassWithInterface() {
		assertTrue("should mark as deprecated", isClassDeprecated(ClassWithInterface.class));
		assertTrue("should mark as deprecated", isClassMyDeprecated(ClassWithInterface.class));
	}

	@Test
	public void isMethodDeprecated_DirectlyOnclass() throws Exception {
		assertTrue("should mark as deprecated", isMethodDeprecated(DeprecatedClass.class));
		assertTrue("should mark as deprecated", isMethodMyDeprecated(DeprecatedClass.class));
	}

	@Test
	public void isMethodDeprecated_OnAbstractclass() throws Exception {
		assertTrue("should mark as deprecated", isMethodDeprecated(DeprecatedAbstractClass.class));
		assertTrue("should mark as deprecated", isMethodMyDeprecated(DeprecatedAbstractClass.class));
	}

	@Test
	public void isMethodDeprecated_OnInterface() throws Exception {
		assertTrue("should mark as deprecated", isMethodDeprecated(DeprecatedInterface.class));
		assertTrue("should mark as deprecated", isMethodMyDeprecated(DeprecatedInterface.class));
	}

	@Test
	public void isMethodDeprecated_OnSuperclass() throws Exception {
		assertTrue("should mark as deprecated", isMethodDeprecated(SubClass.class));
		assertTrue("should mark as deprecated", isMethodMyDeprecated(SubClass.class));
	}

	@Test
	public void isMethodDeprecated_OnClassWithInterface() throws Exception {
		assertTrue("should mark as deprecated", isMethodDeprecated(ClassWithInterface.class));
		assertTrue("should mark as deprecated", isMethodMyDeprecated(ClassWithInterface.class));
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
}