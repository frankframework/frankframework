package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.INamedObject;

class ClassUtilsTest {

	@Test
	void testConvertToType() {
		assertAll(
				() -> assertEquals(7, ClassUtils.convertToType(int.class, "7")),
				() -> assertEquals(7, ClassUtils.convertToType(Integer.class, "7")),
				() -> assertEquals(7L, ClassUtils.convertToType(long.class, "7")),
				() -> assertEquals(7L, ClassUtils.convertToType(Long.class, "7")),
				() -> assertEquals("7", ClassUtils.convertToType(String.class, "7")),
				() -> assertTrue(ClassUtils.convertToType(boolean.class, "true")),
				() -> assertTrue(ClassUtils.convertToType(Boolean.class, "true")),
				() -> assertFalse(ClassUtils.convertToType(Boolean.class, "niet true")),
				() -> assertEquals(TestEnum.ONE, ClassUtils.convertToType(TestEnum.class, "one")),

				() -> assertThrows(IllegalArgumentException.class, () -> ClassUtils.convertToType(Object.class, "dummy")),
				() -> assertThrows(IllegalArgumentException.class, () -> ClassUtils.convertToType(Long.class, "dummy")),
				() -> assertThrows(IllegalArgumentException.class, () -> ClassUtils.convertToType(int.class, "")) // Empty string
		);
	}

	@Test
	void testInvokeStringSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		Method method = clazz.getClass().getDeclaredMethod("setField", String.class);
		ClassUtils.invokeSetter(clazz, method, "value");
		assertEquals("value", clazz.getField());
	}

	@Test
	void testInvokeTestClassSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		TestClass objectToSet = new TestClass();
		ClassUtils.invokeSetter(clazz, "setTestClass", objectToSet);
		assertEquals(objectToSet, clazz.getTestClass());
	}

	@Test
	void testInvokeSuperTestClassSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		TestClass objectToSet = new SuperTestClass();
		ClassUtils.invokeSetter(clazz, "setTestClass", objectToSet);
		assertEquals(objectToSet, clazz.getTestClass());
	}

	@Test
	void testInvokeWithNullSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		Method method = clazz.getClass().getDeclaredMethod("setEnumVarArgs", TestEnum[].class);
		ClassUtils.invokeSetter(clazz, method, null);
		assertNull(clazz.getField());
	}

	@Test
	void testInvokeEnumVarArgsSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		Method method = clazz.getClass().getDeclaredMethod("setEnumVarArgs", TestEnum[].class);
		ClassUtils.invokeSetter(clazz, method, "ONE, TWO");
		assertEquals(TestEnum.ONE, clazz.getTestEnums()[0]);
		assertEquals(TestEnum.TWO, clazz.getTestEnums()[1]);
	}

	@Test
	void testInvokeStringVarArgsSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		Method method = clazz.getClass().getDeclaredMethod("setTestStrings", String[].class);

		ClassUtils.invokeSetter(clazz, method, "AAA, BBB");
		assertEquals("AAA", clazz.getTestStrings()[0]);
		assertEquals("BBB", clazz.getTestStrings()[1]);
	}

	/** see CredentialProvider ClassUtilsTest to test results without Spring present */
	@Test
	void testNameOf() {
		assertEquals("String", ClassUtils.nameOf("test"));
		assertEquals("ClassUtilsTest", ClassUtils.nameOf(this));
		assertEquals("ClassUtilsTest", ClassUtils.nameOf(this.getClass()));
		assertEquals("org.frankframework.util.ClassUtilsTest$1", ClassUtils.nameOf(new INamedObject() {
			private @Getter @Setter String name;
		}));
	}

	@Test
	public void getDeclaredFieldValueReturnsCorrectValue() throws NoSuchFieldException {
		TestClass testClass = new TestClass();
		testClass.setField("testValue");
		Object result = ClassUtils.getDeclaredFieldValue(testClass, "field");
		assertEquals("testValue", result);
	}

	@Test
	public void getDeclaredFieldValueThrowsExceptionForNonExistentField() {
		TestClass testClass = new TestClass();
		assertThrows(NoSuchFieldException.class, () -> ClassUtils.getDeclaredFieldValue(testClass, "nonExistentField"));
	}

	@Test
	public void getDeclaredFieldValueThrowsExceptionForNullObject() {
		assertThrows(NullPointerException.class, () -> ClassUtils.getDeclaredFieldValue(null, "field"));
	}

	@Test
	public void getDeclaredFieldValueThrowsExceptionForNullFieldName() {
		TestClass testClass = new TestClass();
		assertThrows(NullPointerException.class, () -> ClassUtils.getDeclaredFieldValue(testClass, null));
	}

	private enum TestEnum {ONE, TWO}

	@Setter
	@SuppressWarnings("unused")
	private static class TestClass {
		private String field;
	}

	private static class SuperTestClass extends TestClass {}

	private static class DummyClassWithSetter {
		private @Getter @Setter String field;
		private @Getter TestEnum[] testEnums;
		private @Getter String[] testStrings;
		private @Getter @Setter TestClass testClass;

		@SuppressWarnings("unused") // not unused, but used with reflection!
		public void setEnumVarArgs(TestEnum... testEnum) {
			this.testEnums = testEnum;
		}

		@SuppressWarnings("unused") // not unused, but used with reflection!
		public void setTestStrings(String... testStrings) {
			this.testStrings = testStrings;
		}
	}
}
