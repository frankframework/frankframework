package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;

public class MailFileSystemUtilsTest {

	public void testGetValidAddress(String address) {
		testGetValidAddress(address, address);
	}

	public void testGetValidAddress(String expected, String address) {
		assertEquals(expected, MailFileSystemUtils.getValidAddress("test", address));
	}

	@Test
	public void testGetValidAddress() {
		testGetValidAddress("xxx@yy.nl");
		testGetValidAddress("Xxx <xxx@yy.nl>");
		testGetValidAddress("Xxx <xxx@yy.nl>", "\"Xxx\" <xxx@yy.nl>");
	}

	@Test
	public void testGetValidAddressWithLineFeed() {
		testGetValidAddress("Xxx <xxx@yy.nl>", "\"Xxx\n\" <xxx@yy.nl>");
	}

	@Test
	public void testGetValidAddressWithComma1() {
		testGetValidAddress("\"Brakel, G. van\" <gerrit@waf.nl>");
	}

	@Test
	public void testGetValidAddressWithComma2() throws UnsupportedEncodingException {
		InternetAddress address = new InternetAddress("gerrit@waf.nl", "Brakel, G. van");
		testGetValidAddress("\"Brakel, G. van\" <gerrit@waf.nl>", address.toString());
	}

	@Test
	public void testgetValidAddressWithWhitespace1() {
		testGetValidAddress(null, "scan@ <popp.dk scan@popp.dk>");
	}

	@Test
	public void testGetValidAddresssWithWhitespace2() {
		testGetValidAddress(null, "abc xxx@yy.nl");
	}

	@Test
	public void testGetValidAddressWithWhitespace3() {
		testGetValidAddress(null, "xxx@yy.nl xxx@yy.nl");
	}

	@Test
	void testFindBestReplyAddress() {
		assertNull(MailFileSystemUtils.findBestReplyAddress(Collections.emptyMap(), ""));
		assertNull(MailFileSystemUtils.findBestReplyAddress(Collections.emptyMap(), IMailFileSystem.REPLY_ADDRESS_FIELDS_DEFAULT));

		Map<String, Object> headers = new HashMap<>();
		headers.put("replyTo", "replyTo@example.org");
		headers.put("from", "from@example.org");

		assertEquals("replyTo@example.org", MailFileSystemUtils.findBestReplyAddress(headers, IMailFileSystem.REPLY_ADDRESS_FIELDS_DEFAULT));
	}

	@Test
	void testGetValidAddressFromHeader() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("test", Collections.emptyList());
		headers.put("test2", "test@example.org");
		headers.put("test3", List.of("listitem@example.org"));

		// first if
		assertNull(MailFileSystemUtils.getValidAddressFromHeader("test", Collections.emptyMap()));

		// if instanceof list
		assertNull(MailFileSystemUtils.getValidAddressFromHeader("test", headers));
		assertEquals("listitem@example.org", MailFileSystemUtils.getValidAddressFromHeader("test3", headers));

		// default return
		assertEquals("test@example.org", MailFileSystemUtils.getValidAddressFromHeader("test2", headers));
	}

	@Test
	void testGetInvalidAddress() {
		assertNull(MailFileSystemUtils.getValidAddress("test", ""));
	}
}
