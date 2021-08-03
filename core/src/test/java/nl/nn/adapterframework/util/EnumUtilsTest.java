package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;

public class EnumUtilsTest {
	@Rule public ExpectedException exception = ExpectedException.none();

	@Test
	public void testParseNullValue() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("unknown processState value [null]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]");

		EnumUtils.parse(ProcessState.class, null);
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
	public void testParseNonExistingNormalEnum() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("unknown processState value [tralala]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]");

		EnumUtils.parse(ProcessState.class, "tralala");
	}

	@Test
	public void testParseNonExistingNormalEnumWithCustomFieldName() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("unknown fieldName value [tralala]. Must be one of [AVAILABLE, INPROCESS, DONE, ERROR, HOLD]");

		EnumUtils.parseNormal(ProcessState.class, "fieldName", "tralala");
	}

	@Test
	public void testParseNonExistingDocumentedEnum() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("unknown ftpType value [tralala]. Must be one of [FTP, SFTP, FTPSI, FTPSX(TLS), FTPSX(SSL)]");
		EnumUtils.parse(FtpType.class, "tralala");
	}

	@Test
	public void testParseNonExistingDocumentedEnumWithCustomFieldName() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("unknown fieldName value [tralala]. Must be one of [FTP, SFTP, FTPSI, FTPSX(TLS), FTPSX(SSL)]");
		EnumUtils.parseDocumented(FtpType.class, "fieldName", "tralala");
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
		exception.expectMessage("unknown fieldName value [3]. Must be one of [1, 2]");
		EnumUtils.parseFromField(EnumWithInteger.class, "fieldName", 3, i -> i.i);
	}
}
