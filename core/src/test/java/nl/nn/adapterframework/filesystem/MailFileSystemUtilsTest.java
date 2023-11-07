package nl.nn.adapterframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;

public class MailFileSystemUtilsTest {

	public void testGetValidAddress(String address) throws AddressException {
		testGetValidAddress(address, address);
	}

	public void testGetValidAddress(String expected, String address) throws AddressException {
		assertEquals(expected, MailFileSystemUtils.getValidAddress("test", address));
	}

	@Test
	public void testGetValidAddress() throws AddressException {
		testGetValidAddress("xxx@yy.nl");
		testGetValidAddress("Xxx <xxx@yy.nl>");
		testGetValidAddress("Xxx <xxx@yy.nl>", "\"Xxx\" <xxx@yy.nl>");
	}

	@Test
	public void testGetValidAddressWithLineFeed() throws AddressException {
		testGetValidAddress("Xxx <xxx@yy.nl>", "\"Xxx\n\" <xxx@yy.nl>");
	}

	@Test
	public void testGetValidAddressWithComma1() throws AddressException {
		testGetValidAddress("\"Brakel, G. van\" <gerrit@waf.nl>");
	}

	@Test
	public void testGetValidAddressWithComma2() throws AddressException, UnsupportedEncodingException {
		InternetAddress address = new InternetAddress("gerrit@waf.nl", "Brakel, G. van");
		testGetValidAddress("\"Brakel, G. van\" <gerrit@waf.nl>", address.toString());
	}

	@Test
	public void testgetValidAddressWithWhitespace1() throws AddressException {
		testGetValidAddress(null, "scan@ <popp.dk scan@popp.dk>");
	}

	@Test
	public void testGetValidAddresssWithWhitespace2() throws AddressException {
		testGetValidAddress(null, "abc xxx@yy.nl");
	}

	@Test
	public void testGetValidAddressWithWhitespace3() throws AddressException {
		testGetValidAddress(null, "xxx@yy.nl xxx@yy.nl");
	}
}
