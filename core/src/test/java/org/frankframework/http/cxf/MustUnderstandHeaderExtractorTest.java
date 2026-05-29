package org.frankframework.http.cxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Test;

class MustUnderstandHeaderExtractorTest {

	@Test
	void testExtractHeader() {
		// Arrange
		MustUnderstandHeaderExtractor interceptor = new MustUnderstandHeaderExtractor();

		SoapHeader header1 = new SoapHeader(new QName(null, "h1"), null, null, true, "actor1");
		SoapHeader header2 = new SoapHeader(new QName(null, "h2"), null, null, false, "actor2");
		SoapHeader header3 = new SoapHeader(new QName(null, "h3"), null, null, true);
		SoapMessage soapMessage = new SoapMessage(new MessageImpl());
		soapMessage.getHeaders().add(header1);
		soapMessage.getHeaders().add(header2);
		soapMessage.getHeaders().add(header3);

		// Act
		interceptor.handleMessage(soapMessage);
		Set<QName> understoodHeaders = interceptor.getUnderstoodHeaders();

		// Assert
		assertEquals(2, understoodHeaders.size());
		assertTrue(understoodHeaders.contains(header1.getName()));
		assertFalse(understoodHeaders.contains(header2.getName()));
		assertTrue(understoodHeaders.contains(header3.getName()));
	}
}
