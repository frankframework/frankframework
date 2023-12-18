package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.frankframework.util.ClassUtils;
import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.INamedObject;

public class ClassUtilsTest {

	private static enum TestEnum {ONE,TWO};

	@Test
	public void testConvertToType() {
		assertAll(
			() -> assertEquals(7, ClassUtils.convertToType(int.class, "7")),
			() -> assertEquals(7, ClassUtils.convertToType(Integer.class, "7")),
			() -> assertEquals(7L, ClassUtils.convertToType(long.class, "7")),
			() -> assertEquals(7L, ClassUtils.convertToType(Long.class, "7")),
			() -> assertEquals("7", ClassUtils.convertToType(String.class, "7")),
			() -> assertEquals(true, ClassUtils.convertToType(boolean.class, "true")),
			() -> assertEquals(true, ClassUtils.convertToType(Boolean.class, "true")),
			() -> assertEquals(false, ClassUtils.convertToType(Boolean.class, "niet true")),
			() -> assertEquals(TestEnum.ONE, ClassUtils.convertToType(TestEnum.class, "one")),

			() -> assertThrows(IllegalArgumentException.class, ()->ClassUtils.convertToType(Object.class, "dummy")),
			() -> assertThrows(IllegalArgumentException.class, ()->ClassUtils.convertToType(Long.class, "dummy")),
			() -> assertThrows(IllegalArgumentException.class, ()->ClassUtils.convertToType(int.class, "")) //Empty string
		);
	}

	private static class DummyClassWithSetter {
		private @Getter @Setter String field;
	}

	@Test
	public void testInvokeSetter() throws Exception {
		DummyClassWithSetter clazz = new DummyClassWithSetter();
		Method method = clazz.getClass().getDeclaredMethod("setField", new Class[] {String.class});
		ClassUtils.invokeSetter(clazz, method, "value");
		assertEquals("value", clazz.getField());
	}

	/** see CredentialProvider ClassUtilsTest to test results without Spring present */
	@Test
	public void testNameOf() {
		assertEquals("String", ClassUtils.nameOf("test"));
		assertEquals("ClassUtilsTest", ClassUtils.nameOf(this));
		assertEquals("ClassUtilsTest", ClassUtils.nameOf(this.getClass()));
		assertEquals("org.frankframework.util.ClassUtilsTest$1", ClassUtils.nameOf(new INamedObject() {
			private @Getter @Setter String name;
		}));
	}
}
