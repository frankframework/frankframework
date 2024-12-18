package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.frankframework.core.INamedObject;

/**
 * This Class tests whether or not the spring-core dependency is present on the classpath.
 * This dependency is present but marked as optional in the frankframework-commons module.
 * 
 * In the commons module this optional compile module is present during tests. And thus spring-core will always be present.
 */
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
