package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import lombok.Getter;

import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;

public class EnumUtilsTest {

	@Test
	public void testParseNullValue() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class, () -> {
					EnumUtils.parse(TestEnumWithField.class, null);
				}
		);
		assertEquals("cannot set field [testEnumWithField] to unparsable value [null]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]", exception.getMessage());
	}

	@Test
	public void testParseEnum() {
		TestEnumWithField state = EnumUtils.parse(TestEnumWithField.class, "available");
		assertNotNull(state);
		assertEquals("Available", state.getName());

		TestEnumWithField state2 = EnumUtils.parseNormal(TestEnumWithField.class, "fieldName", "available");
		assertNotNull(state2);
		assertEquals("Available", state2.getName());
	}

	@Test
	public void testParseDocumentedEnum() {
		DocumentedTestEnum type = EnumUtils.parse(DocumentedTestEnum.class, "FTP");
		assertNotNull(type);
		assertEquals("FTP", type.getLabel());

		DocumentedTestEnum type2 = EnumUtils.parseDocumented(DocumentedTestEnum.class, "fieldName", "FTP");
		assertNotNull(type2);
		assertEquals("FTP", type2.getLabel());
	}

	@Test
	public void testParseNonExistingEnum() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EnumUtils.parse(TestEnumWithField.class, "tralala"));
		assertEquals("cannot set field [testEnumWithField] to unparsable value [tralala]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]", e.getMessage());
	}

	@Test
	public void testParseNonExistingEnumWithFieldName() {
		EnumUtils.parse(TestEnumWithField.class, "fieldname", "Available"); //Exists

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EnumUtils.parse(TestEnumWithField.class, "fieldname", "tralala2")); //Does not exist
		assertEquals("cannot set field [fieldname] to unparsable value [tralala2]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]", e.getMessage());
	}

	@Test
	public void testParseNonExistingNormalEnumWithCustomFieldName() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EnumUtils.parseNormal(TestEnumWithField.class, "fieldName", "tralala"));
		assertEquals("cannot set field [fieldName] to unparsable value [tralala]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]", e.getMessage());
	}

	@Test
	public void testParseNonExistingDocumentedEnum() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EnumUtils.parse(DocumentedTestEnum.class, "tralala"));
		assertEquals("cannot set field [documentedTestEnum] to unparsable value [tralala]. Must be one of [FTP, SFTP, FTPSI, FTPSX(TLS), FTPSX(SSL)]", e.getMessage());
	}

	@Test
	public void testParseNonExistingDocumentedEnumWithCustomFieldName() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EnumUtils.parseDocumented(DocumentedTestEnum.class, "fieldName", "tralala"));
		assertEquals("cannot set field [fieldName] to unparsable value [tralala]. Must be one of [FTP, SFTP, FTPSI, FTPSX(TLS), FTPSX(SSL)]", e.getMessage());
	}

	@Test
	public void testParseBoth() {
		assertEquals(TestDocumentedEnum.ONE, EnumUtils.parse(TestDocumentedEnum.class, "een", true));
		assertEquals(TestDocumentedEnum.ONE, EnumUtils.parse(TestDocumentedEnum.class, "one", true));
		assertEquals(TestNotDocumentedEnum.ONE, EnumUtils.parse(TestNotDocumentedEnum.class, "one", true));

		IllegalArgumentException exception1 = assertThrows(
				IllegalArgumentException.class, () -> {
					EnumUtils.parse(TestNotDocumentedEnum.class, "", true);
				}
		);
		assertEquals("cannot set field [testNotDocumentedEnum] to unparsable value []. Must be one of [ONE, TWO]", exception1.getMessage());

		IllegalArgumentException exception2 = assertThrows(
				IllegalArgumentException.class, () -> {
					EnumUtils.parse(TestNotDocumentedEnum.class, "zero", true); //unknown not-documented enum
				}
		);
		assertEquals("cannot set field [testNotDocumentedEnum] to unparsable value [zero]. Must be one of [ONE, TWO]", exception2.getMessage());
		assertEquals(0, exception2.getSuppressed().length);

		IllegalArgumentException exception3 = assertThrows(
				IllegalArgumentException.class, () -> {
					EnumUtils.parse(TestDocumentedEnum.class, "zero", true); //unknown documented enum
				}
		);
		assertEquals("cannot set field [testDocumentedEnum] to unparsable value [zero]. Must be one of [een, twee]", exception3.getMessage());
		assertEquals(1, exception3.getSuppressed().length);
	}

	@Test
	public void testIntegerValue() {
		EnumWithInteger e = EnumUtils.parseFromField(EnumWithInteger.class, "fieldName", 1, i -> i.i);
		assertNotNull(e);
		assertEquals("ONE", e.name());
	}

	@Test
	public void testParseNonExistingIntegerValue() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EnumUtils.parseFromField(EnumWithInteger.class, "fieldName", 3, i -> i.i));
		assertEquals("cannot set field [fieldName] to unparsable value [3]. Must be one of [1, 2]", e.getMessage());
	}

	enum TestEnumWithField {
		AVAILABLE("Available"),
		INPROCESS("InProcess"),
		DONE("Done"),
		ERROR("Error"),
		HOLD("Hold");

		@Getter
		private final String name;

		TestEnumWithField(String name) {
			this.name = name;
		}
	}

	enum DocumentedTestEnum implements DocumentedEnum {
		@EnumLabel("FTP") FTP(null, true),
		@EnumLabel("SFTP") SFTP(null, true),
		@EnumLabel("FTPSI") FTPS_IMPLICIT("TLS", true),
		@EnumLabel("FTPSX(TLS)") FTPS_EXPLICIT_TLS("TLS", false),
		@EnumLabel("FTPSX(SSL)") FTPS_EXPLICIT_SSL("SSL", false);

		private final @Getter boolean implicit;
		private final @Getter String protocol;

		DocumentedTestEnum(String protocol, boolean implicit) {
			this.protocol = protocol;
			this.implicit = implicit;
		}
	}

	public enum TestDocumentedEnum implements DocumentedEnum {@EnumLabel("een") ONE, @EnumLabel("twee") TWO}

	public enum TestNotDocumentedEnum {ONE, TWO}

	public enum EnumWithInteger {
		ONE(1), TWO(2);
		private int i = 0;

		EnumWithInteger(int i) {
			this.i = i;
		}
	}
}
