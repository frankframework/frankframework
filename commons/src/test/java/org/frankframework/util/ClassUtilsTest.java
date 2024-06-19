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

	private enum TestEnum {ONE,TWO}

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

			() -> assertThrows(IllegalArgumentException.class, ()->ClassUtils.convertToType(Object.class, "dummy")),
			() -> assertThrows(IllegalArgumentException.class, ()->ClassUtils.convertToType(Long.class, "dummy")),
			() -> assertThrows(IllegalArgumentException.class, ()->ClassUtils.convertToType(int.class, "")) //Empty string
		);
	}

	private class DummyClassWithSetter {
		private @Getter @Setter String field;
		private @Getter TestEnum[] testEnums;
		private @Getter String[] testStrings;
		public void setEnumVarArgs(TestEnum... testEnum) {
			this.testEnums = testEnum;
		}
		public void setTestStrings(String... testStrings) {
			this.testStrings = testStrings;
		}
	}

	@Test
	void testInvokeStringSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		Method method = clazz.getClass().getDeclaredMethod("setField", String.class);
		ClassUtils.invokeSetter(clazz, method, "value");
		assertEquals("value", clazz.getField());
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
}
