package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.frankframework.core.INamedObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClassUtilsTest {

	@BeforeAll
	public static void ensureOptionalDependenciesAreNotOnTheClasspath() {
		assertAll(
			() -> assertFalse(isPresent("org.springframework.util.ClassUtils"), "found Spring.ClassUtils on the classpath, unable to test optional dependency")
		);
	}

	@Test
	public void testIfSpringDependencyOptional() {
		assertEquals("String", ClassUtils.nameOf("test"));
		assertEquals("ClassUtilsTest", ClassUtils.nameOf(this));
		assertEquals("ClassUtilsTest", ClassUtils.nameOf(this.getClass()));
		assertEquals("org.frankframework.util.ClassUtilsTest$1", ClassUtils.nameOf(new INamedObject() {
			@Override
			public void setName(String name) {
				// nothing to set
			}

			@Override
			public String getName() {
				return null;
			}
		}));
	}

	public static boolean isPresent(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (Throwable ex) {
			// Class or one of its dependencies is not present...
			return false;
		}
	}
}
