package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;

public class EnumUtilsTest {
	@Rule public ExpectedException exception = ExpectedException.none();

	@Test
	public void testParseNullValue() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class, () -> {
					EnumUtils.parse(ProcessState.class, null);
			}
		);
		assertEquals("cannot set field [processState] to unparsable value [null]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]", exception.getMessage());
	}

	@Test
	public void testParseEnum() {
		ProcessState state = EnumUtils.parse(ProcessState.class, "available");
		assertNotNull(state);
		assertEquals("Available", state.getName());

		ProcessState state2 = EnumUtils.parseNormal(ProcessState.class, "fieldName", "available");
		assertNotNull(state2);
		assertEquals("Available", state2.getName());
	}

	@Test
	public void testParseDocumentedEnum() {
		FtpType type = EnumUtils.parse(FtpType.class, "FTP");
		assertNotNull(type);
		assertEquals("FTP", type.getLabel());

		FtpType type2 = EnumUtils.parseDocumented(FtpType.class, "fieldName", "FTP");
		assertNotNull(type2);
		assertEquals("FTP", type2.getLabel());
	}

	@Test
	public void testParseNonExistingEnum() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("cannot set field [processState] to unparsable value [tralala]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]");

		EnumUtils.parse(ProcessState.class, "tralala");
	}

	@Test
	public void testParseNonExistingEnumWithFieldName() {
		EnumUtils.parse(ProcessState.class, "fieldname", "Available"); //Exists
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("cannot set field [fieldname] to unparsable value [tralala2]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]");

		EnumUtils.parse(ProcessState.class, "fieldname", "tralala2"); //Does not exist
	}

	@Test
	public void testParseNonExistingNormalEnumWithCustomFieldName() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("cannot set field [fieldName] to unparsable value [tralala]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]");

		EnumUtils.parseNormal(ProcessState.class, "fieldName", "tralala");
	}

	@Test
	public void testParseNonExistingDocumentedEnum() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("cannot set field [ftpType] to unparsable value [tralala]. Must be one of [FTP, SFTP, FTPSI, FTPSX(TLS), FTPSX(SSL)]");
		EnumUtils.parse(FtpType.class, "tralala");
	}

	@Test
	public void testParseNonExistingDocumentedEnumWithCustomFieldName() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("cannot set field [fieldName] to unparsable value [tralala]. Must be one of [FTP, SFTP, FTPSI, FTPSX(TLS), FTPSX(SSL)]");
		EnumUtils.parseDocumented(FtpType.class, "fieldName", "tralala");
	}

	public static enum TestDocumentedEnum implements DocumentedEnum { @EnumLabel("een") ONE, @EnumLabel("twee") TWO; }
	public static enum TestNotDocumentedEnum { ONE, TWO; }

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
		assertTrue(exception2.getSuppressed().length == 0);

		IllegalArgumentException exception3 = assertThrows(
				IllegalArgumentException.class, () -> {
					EnumUtils.parse(TestDocumentedEnum.class, "zero", true); //unknown documented enum
			}
		);
		assertEquals("cannot set field [testDocumentedEnum] to unparsable value [zero]. Must be one of [een, twee]", exception3.getMessage());
		assertTrue(exception3.getSuppressed().length == 1);
	}

	public static enum EnumWithInteger {
		ONE(1), TWO(2);
		private int i = 0;
		private EnumWithInteger(int i) {
			this.i = i;
		}
	}

	@Test
	public void testIntegerValue() {
		EnumWithInteger e = EnumUtils.parseFromField(EnumWithInteger.class, "fieldName", 1, i -> i.i);
		assertNotNull(e);
		assertEquals("ONE", e.name());
	}

	@Test
	public void testParseNonExistingIntegerValue() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("cannot set field [fieldName] to unparsable value [3]. Must be one of [1, 2]");
		EnumUtils.parseFromField(EnumWithInteger.class, "fieldName", 3, i -> i.i);
	}
}
