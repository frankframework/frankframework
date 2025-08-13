package org.frankframework.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class KeystoreTypeTest {

	@ParameterizedTest(name = "valueOf round-trip works for {0}")
	@EnumSource(KeystoreType.class)
	@DisplayName("Enum valueOf/toString round-trip")
	void valueOfRoundTrip(KeystoreType type) {
		assertNotNull(type);
		assertSame(type, KeystoreType.valueOf(type.name()), "valueOf(name) should return the same constant");
		assertEquals(type.name(), type.toString(), "toString() should equal the enum name");
	}

	@ParameterizedTest(name = "{0} name is uppercase")
	@EnumSource(KeystoreType.class)
	@DisplayName("Enum constant names are uppercase")
	void namesAreUppercase(KeystoreType type) {
		assertEquals(type.name().toUpperCase(Locale.ROOT), type.name());
	}

	@ParameterizedTest(name = "{0} is not PEM")
	@EnumSource(value = KeystoreType.class, mode = EnumSource.Mode.EXCLUDE, names = "PEM")
	@DisplayName("Non-PEM types exclude PEM")
	void nonPemValuesExcludePem(KeystoreType type) {
		assertNotEquals(KeystoreType.PEM, type);
	}
}
