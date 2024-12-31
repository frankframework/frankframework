/*
   Copyright 2018-2020 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.http;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.net.URL;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestFileUtils;

public class HttpSenderTest extends HttpSenderTestBase<HttpSender> {

	@Override
	public HttpSender createSender() {
		return spy(new HttpSender());
	}

	@Test
	public void relativeUrl() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!

		sender.setMethodType(HttpMethod.GET);
		sender.setUrl("relative/path");

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertThat(e.getMessage(), Matchers.endsWith("must use an absolute url starting with http(s)://"));
	}

	@Test
	public void relativeUrlParameter() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!

		sender.setMethodType(HttpMethod.GET);
		sender.setUrl(null);
		sender.addParameter(new Parameter("url", "relative/path"));

		sender.configure();
		sender.start();

		SenderException e = assertThrows(SenderException.class, ()->sender.sendMessageOrThrow(null, null));
		assertThat(e.getMessage(), Matchers.endsWith("must use an absolute url starting with http(s)://"));
	}

	@Test
	public void testWithDefaultPort() throws Throwable {
		sender = getSender(false);

		sender.setMethodType(HttpMethod.GET);
		sender.setUrl("http://127.0.0.1:80/path/here");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(new Message("dummy"), session).asString();
		assertEqualsIgnoreCRLF(getFile("testWithDefaultPort.txt"), result.trim());
	}

	@Test
	public void testWithRandomPort() throws Throwable {
		sender = getSender(false);

		sender.setMethodType(HttpMethod.GET);
		sender.setUrl("http://127.0.0.1:1337/path/here");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(new Message("dummy"), session).asString();
		assertEqualsIgnoreCRLF(getFile("testWithRandomPort.txt"), result.trim());
	}

	@Test
	public void testKnownProtocol() throws Exception {
		sender = getSender();
		sender.setProtocol("TLSv1.2");
		assertDoesNotThrow(sender::configure);
	}

	@Test
	public void testUnknownProtocol() throws Exception {
		sender = getSender();
		sender.setProtocol("pietje-puk");
		ConfigurationException ex = assertThrows(ConfigurationException.class, sender::configure);
		assertTrue(ex.getMessage().contains("unknown protocol [pietje-puk], must be one of ["));
		assertTrue(ex.getMessage().contains("TLSv1.2"));
	}

	@Test
	public void testSupportedCipherSuite() throws Exception {
		sender = getSender();
		sender.setSupportedCipherSuites("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
		assertDoesNotThrow(sender::configure);
	}

	@Test
	public void testUnsupportedCipherSuite() throws Exception { //Also validates if the protocol may be empty (JDK default)
		sender = getSender();
		sender.setSupportedCipherSuites("TLS_Tralala");
		ConfigurationException ex = assertThrows(ConfigurationException.class, sender::configure);
		assertTrue(ex.getMessage().contains("Unsupported CipherSuite(s), must be one (or more) of ["));
		assertTrue(ex.getMessage().contains("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"));
	}

	@Test
	public void testEmptyCipherSuite() throws Exception {
		sender = getSender();
		sender.setSupportedCipherSuites("");
		assertDoesNotThrow(sender::configure);
	}

	@Test
	public void simpleMockedHttpGetWithoutPRC() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		sender.setMethodType(HttpMethod.GET);
		sender.setTreatInputMessageAsParameters(true);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithoutPRC.txt"), result.trim());
	}

	@Test
	public void testContentType() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setContentType("text/xml");
		sender.configure();
		assertEqualsIgnoreCRLF("text/xml; charset=UTF-8", sender.getFullContentType().toString());
	}

	@Test
	public void testCharset() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setCharSet("ISO-8859-1");
		sender.setMethodType(HttpMethod.POST);
		sender.configure();
		assertEqualsIgnoreCRLF("text/html; charset=ISO-8859-1", sender.getFullContentType().toString());
	}

	@Test
	public void testContentTypeAndCharset1() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.setCharSet("IsO-8859-1");
		sender.setContentType("text/xml");
		sender.configure();

		//Make sure charset is parsed properly and capital case
		assertEqualsIgnoreCRLF("text/xml; charset=ISO-8859-1", sender.getFullContentType().toString());
	}

	@Test
	public void testContentTypeAndCharset2() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.setContentType("application/xml");
		sender.setCharSet("uTf-8");
		sender.configure();

		//Make sure charset is parsed properly and capital case
		assertEqualsIgnoreCRLF("application/xml; charset=UTF-8", sender.getFullContentType().toString());
	}

	@Test
	public void parseContentTypeWithCharset() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.setContentType("text/xml; charset=ISO-8859-1");
		sender.configure();
		assertEqualsIgnoreCRLF("text/xml; charset=ISO-8859-1", sender.getFullContentType().toString());
	}

	@Test
	public void notContentTypeUnlessExplicitlySet() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.configure();
		assertNull(sender.getFullContentType());
	}

	@Test()
	public void notCharsetUnlessContentTypeExplicitlySet() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setCharSet("ISO-8859-1");
		sender.configure();
		assertNull(sender.getFullContentType());
	}

	@Test
	public void simpleMockedHttpGet() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGet.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetEncodeMessage() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("this is my dynamic url");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.GET);
		sender.setEncodeMessages(true);
		sender.setTreatInputMessageAsParameters(true);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetEncodeMessage.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetWithContentType() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.GET); //Make sure its a GET request
		sender.setContentType("application/json");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithContentType.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetWithContentTypeAndCharset() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.GET); //Make sure its a GET request
		sender.setContentType("application/json");
		sender.setCharSet("ISO-8859-1");


		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithContentTypeAndCharset.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPost() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo this is my message");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPost.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostWithUrlPathAndQueryParam() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/value%20value?path=tr%40lala");
		Message input = new Message("hallo this is my message");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostWithUrlPathAndQueryParam.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostEncodeMessage() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo dit is mijn bericht");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)
		sender.setEncodeMessages(true);
		sender.setTreatInputMessageAsParameters(true);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostEncodeMessage.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostAppendParamsToBody() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/something&dummy=true");
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		sender.addParameter(new Parameter("key", "value"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostAppendParamsToBody.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostParamsOnly() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/something&dummy=true");
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		sender.addParameter(new Parameter("key", "value"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.setTreatInputMessageAsParameters(false);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostAppendParamsToBodyAndEmptyBody.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostAppendParamsToBodyAndEmptyBody() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/something&dummy=true");
		Message input = new Message("");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		sender.addParameter(new Parameter("key", "value"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostAppendParamsToBodyAndEmptyBody.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPut() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.PUT); //should handle a mix of upper and lowercase characters :)

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPut.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPatch() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo patch request");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.PATCH); //should handle a mix of upper and lowercase characters :)

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPatch.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetWithParams() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("key", "value"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithParams.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetWithUrlParamAndPath() throws Throwable {
		sender = getSender();
		sender.setUrl(null); //unset URL
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("url", "http://127.0.0.1/value%20value?path=tr%40lala"));

		sender.addParameter(new Parameter("illegalCharacters", "o@t&h=e+r$V,a/lue"));

		sender.addParameter(new Parameter("normalCharacters", "helloWorld"));

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithUrlParamAndPath.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostCharset() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setCharSet("ISO-8859-1");
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostCharset.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpUnknownHeaderParam() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("key", "value"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.setMethodType(HttpMethod.GET);
		sender.setHeadersParams("custom-header, doesn-t-exist");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithParams.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostUrlEncoded() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("key", "value"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setFirstBodyPartName("nameOfTheFirstContentId");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostUrlEncoded.txt"), result);
	}

	@Test
	public void postTypeUrlEncoded() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("key", "value"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.setMethodType(HttpMethod.POST);
		sender.setPostType(HttpEntityType.URLENCODED);
		sender.setFirstBodyPartName("nameOfTheFirstContentId");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostUrlEncoded.txt"), result);
	}

	@Test
	public void simpleMockedHttpPostJSON() throws Throwable {
		sender = getSender();
		Message input = new Message("{\"key\": \"value\"}");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setContentType("application/json");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostJSON.txt"), result);
	}

	@Test
	public void binaryHttpPostJSON() throws Throwable {
		sender = getSender();
		Message input = new Message(new ByteArrayInputStream("{\"key1\": \"value2\"}".getBytes())); //Let's pretend this is a big JSON stream!
		assertTrue(input.isBinary(), "input message has to be of type binary");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setContentType("application/json");
		sender.setPostType(HttpEntityType.BINARY);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("binaryHttpPostJSON.txt"), result);
	}

	@Test
	public void binaryHttpPostPDF() throws Throwable {
		sender = getSender();
		URL url = TestFileUtils.getTestFileURL("/Documents/doc001.pdf");
		Message input = new UrlMessage(url);
		assertTrue(input.isBinary(), "input message has to be of type binary");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setContentType("application/pdf");
		sender.setPostType(HttpEntityType.BINARY);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("binaryHttpPostPDF.txt"), result);
	}

	@Test
	public void simpleMockedHttpPutJSON() throws Throwable {
		sender = getSender();
		Message input = new Message("{\"key\": \"value\"}");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.PUT);
		sender.setContentType("application/json");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPutJSON.txt"), result);
	}

	@Test
	public void binaryHttpPutJSON() throws Throwable {
		sender = getSender();
		Message input = new Message(new ByteArrayInputStream("{\"key1\": \"value2\"}".getBytes())); //Let's pretend this is a big JSON stream!
		assertTrue(input.isBinary(), "input message has to be of type binary");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.PUT);
		sender.setContentType("application/json");
		sender.setPostType(HttpEntityType.BINARY);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("binaryHttpPutJSON.txt"), result);
	}

	@Test
	public void simpleMockedHttpMultipart() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setFirstBodyPartName("request");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMultipart.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpMultipartWithUrlPathAndQueryParam() throws Throwable {
		sender = getSender();
		sender.setUrl("http://127.0.0.1/value%20value?path=tr%40lala");

		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setFirstBodyPartName("request");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMultipartWithUrlPathAndQueryParam.txt"), result.trim());
	}

	@Test
	public void postTypeMultipart() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setPostType(HttpEntityType.FORMDATA);
		sender.setFirstBodyPartName("request");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMultipart.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpMtom() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setFirstBodyPartName("request");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMtom.txt"), result.trim());
	}

	@Test
	public void multipartWithoutFirstBodyPartName() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("multipartWithoutFirstBodyPartName.txt"), result.trim());
	}

	@Test
	public void mtomWithoutFirstBodyPartName() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("mtomWithoutFirstBodyPartName.txt"), result.trim());
	}

	@Test
	public void simpleMultipartFromParameters() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setFirstBodyPartName("request");
		sender.setMtomContentTransferEncoding("binary");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/>\
				<part name="string.txt" \
				sessionKey="stringPart" \
				mimeType="text/plain"/></parts>\
				""";
		session.put("multipartXml", xmlMultipart);
		session.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");
		sender.addParameter(new Parameter("string-part", "<string content/>"));

		session.put("stringPart", new Message("mock pdf content"));
		session.put("binaryPart", new Message(new Message("mock pdf content").asInputStream()));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.configure();
		sender.start();

		String result = sendMessage(input).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMultipartFromParametersAndMultipartXml.txt"), result.trim());
	}

	@Test
	public void simpleMtomFromParameters() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		sender.setMethodType(HttpMethod.POST);
		sender.setFirstBodyPartName("request");
		sender.setPostType(HttpEntityType.MTOM);
		sender.setMtomContentTransferEncoding("base64");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/>\
				<part name="string.txt" \
				sessionKey="stringPart" \
				mimeType="text/plain"/></parts>\
				""";
		session.put("multipartXml", xmlMultipart);
		session.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");
		sender.addParameter(new Parameter("string-part", "<string content/>"));

		session.put("stringPart", new Message("mock pdf content"));
		session.put("binaryPart", new Message(new Message("mock pdf content").asInputStream()));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.configure();
		sender.start();

		String result = sendMessage(input).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMtomFromParametersAndMultipartXml.txt"), result.trim());
	}

	@Test
	public void simpleMultipartFromParametersNoPartName() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setFirstBodyPartName("request");
		sender.setMtomContentTransferEncoding("binary");

		String xmlMultipart = """
				<parts><part type="file" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/>\
				<part sessionKey="stringPart" \
				mimeType="text/plain"/></parts>\
				""";
		session.put("multipartXml", xmlMultipart);
		session.put("part_file", new Message(new ByteArrayInputStream("<dummy xml file/>".getBytes()), new MessageContext().withName("PartFile.xml")));

		sender.setMultipartXmlSessionKey("multipartXml");
		sender.addParameter(new Parameter("string-part", "<string content/>"));

		session.put("stringPart", new Message("mock pdf content"));
		session.put("binaryPart", new Message(new Message("mock pdf content").asInputStream()));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.configure();
		sender.start();

		String result = sendMessage(input).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMultipartFromParametersAndMultipartXmlNoPartName.txt"), result.trim());
	}

	@Test
	public void simpleMtomFromParametersNoPartName() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		sender.setMethodType(HttpMethod.POST);
		sender.setFirstBodyPartName("request");
		sender.setPostType(HttpEntityType.MTOM);
		sender.setMtomContentTransferEncoding("base64");

		String xmlMultipart = """
				<parts><part type="file" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/>\
				<part sessionKey="stringPart" \
				mimeType="text/plain"/></parts>\
				""";
		session.put("multipartXml", xmlMultipart);
		session.put("part_file", new Message(new ByteArrayInputStream("<dummy xml file/>".getBytes()), new MessageContext().withName("PartFile.xml")));

		sender.setMultipartXmlSessionKey("multipartXml");
		sender.addParameter(new Parameter("string-part", "<string content/>"));

		session.put("stringPart", new Message("mock pdf content"));
		session.put("binaryPart", new Message(new Message("mock pdf content").asInputStream()));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.configure();
		sender.start();

		String result = sendMessage(input).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMtomFromParametersAndMultipartXmlNoPartName.txt"), result.trim());
	}


	@Test
	public void postTypeMtom() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setPostType(HttpEntityType.MTOM);
		sender.setFirstBodyPartName("request");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMtom.txt"), result.trim());
	}

	@Test
	public void skipUrlParameter() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setFirstBodyPartName("request");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.addParameter(new Parameter("url", "http://ignore.me")); //skip this

		sender.addParameter(new Parameter("my-beautiful-part", "<partContent/>"));

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("parametersToSkip.txt"), result.trim());
	}

	@Test
	public void skipEmptyParameter() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setFirstBodyPartName("request");
		sender.setParametersToSkipWhenEmpty("empty-param");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.addParameter(new Parameter("url", "http://ignore.me")); //skip this

		Parameter emptyParam = new Parameter("empty-param", "");
		emptyParam.setSessionKey("empty-does-not-exist");
		sender.addParameter(emptyParam);
		sender.addParameter(new Parameter("my-beautiful-part", "<partContent/>"));

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("parametersToSkip.txt"), result.trim());
	}

	@Test
	public void skipEmptyMultipartXmlSessionKey() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setFirstBodyPartName("request");
		sender.setParametersToSkipWhenEmpty("empty-param");

		session.put("multipartXml", ""); //empty!
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.addParameter(new Parameter("url", "http://ignore.me")); //skip this

		Parameter emptyParam = new Parameter("empty-param", "");
		emptyParam.setSessionKey("empty-does-not-exist");
		sender.addParameter(emptyParam);
		sender.addParameter(new Parameter("my-beautiful-part", "<partContent/>"));

		sender.configure();
		sender.start();

		String result = sendMessage(input).asString();
		assertEqualsIgnoreCRLF(getFile("skipEmptyMultipartXmlSessionKey.txt"), result.trim());
	}

	@Test
	public void specialCharactersInURLParam() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("url", "http://127.0.0.1/value%20value?param=Hello%20G%C3%BCnter"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("specialCharactersInURLParam.txt"), result.trim());
	}

	@Test
	public void specialCharactersDoubleEscaped() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("url", "HTTP://127.0.0.1/value%2Fvalue?param=Hello%2520%2FG%C3%BCnter"));

		sender.addParameter(new Parameter("otherKey", "otherValue"));

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("specialCharactersDoubleEscaped.txt"), result.trim());
	}

	@Test
	public void unsupportedScheme() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.addParameter(new Parameter("url", "ftp://127.0.0.1/value%2Fvalue?param=Hello%2520%2FG%C3%BCnter"));

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(input, pls));
	}

	@Test
	public void paramsWithoutValue() throws Throwable {
		sender = getSender();
		sender.addParameter(new Parameter("url", "http://127.0.0.1/value%2Fvalue?emptyParam"));
		sender.addParameter(new Parameter("myParam", ""));
		sender.setMethodType(HttpMethod.GET);
		sender.configure();
		sender.start();

		Message input = new Message("");
		PipeLineSession pls = new PipeLineSession(session);

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("paramsWithoutValue.txt"), result.trim());
	}

	@Test
	public void paramsWithoutValueSkipped() throws Throwable {
		sender = getSender();
		sender.addParameter(new Parameter("url", "http://127.0.0.1/value%2Fvalue?emptyParam"));
		sender.addParameter(new Parameter("myParam", ""));
		sender.setMethodType(HttpMethod.GET);
		sender.setParametersToSkipWhenEmpty("myParam");
		sender.configure();
		sender.start();

		Message input = new Message("");
		PipeLineSession pls = new PipeLineSession(session);

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("paramsWithoutValue-skipped.txt"), result.trim());
	}

	@Test
	public void paramsWithoutValueSkippedAll() throws Throwable {
		sender = getSender();
		sender.addParameter(new Parameter("url", "http://127.0.0.1/value%2Fvalue?emptyParam"));
		sender.addParameter(new Parameter("myParam", ""));
		sender.setMethodType(HttpMethod.GET);
		sender.setParametersToSkipWhenEmpty("*");
		sender.configure();
		sender.start();

		Message input = new Message("");
		PipeLineSession pls = new PipeLineSession(session);

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("paramsWithoutValue-skipped.txt"), result.trim());
	}

	@Test
	public void testWithKeystoreAndKeyPairHavingDifferentPasswords() throws Throwable { // keystore and the key pair have different password
		String keystore = "/Signature/ks_multipassword.jks";

		sender = getSender();
		sender.setKeystore(keystore);
		sender.setKeystorePassword("geheim");
		sender.setKeystoreType(KeystoreType.JKS);
		sender.setKeystoreAliasPassword("test");

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();
	}

	@Test
	public void testUsingSamePasswordForKeystoreAndKeyPairHavingDifferentPasswords() throws Exception { // keystore and the key pair have different password
		String keystore = "/Signature/ks_multipassword.jks";

		sender = getSender();
		sender.setKeystore(keystore);
		sender.setKeystorePassword("geheim");
		sender.setKeystoreType(KeystoreType.JKS);

		sender.setMethodType(HttpMethod.GET);

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertThat(e.getMessage(), Matchers.containsString("cannot create or initialize SocketFactory"));
	}

	@Test
	public void testWithKeystoreHavingMultipleEntriesWithSamePassword() throws Exception { // keystore and the key pairs have the same password
		String keystore = "/Signature/ks_multientry_samepassword.jks";

		sender = getSender();
		sender.setKeystore(keystore);
		sender.setKeystorePassword("geheim");
		sender.setKeystoreType(KeystoreType.JKS);
		sender.setKeystoreAliasPassword("test");

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();
	}

	@Test
	public void testWithKeystoreHavingMultipleEntriesAndEachWithDifferentPasswords() throws Throwable {
		// It would be difficult to provide password for each entry in a keystore
		//which has multiple entries each with a different password

		String keystore = "/Signature/ks_multientry_differentpassword.jks";

		sender = getSender();
		sender.setKeystore(keystore);
		sender.setKeystorePassword("geheim");
		sender.setKeystoreType(KeystoreType.JKS);
		sender.setKeystoreAliasPassword("test");

		sender.setMethodType(HttpMethod.GET);

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertThat(e.getMessage(), Matchers.containsString("cannot create or initialize SocketFactory"));
	}

	@Test
	public void testTargetingSpecificKeyPairInMultiEntryKeystore() throws Throwable {

		String keystore = "/Signature/ks_multientry_differentpassword.jks";

		sender = getSender();
		sender.setKeystore(keystore);
		sender.setKeystorePassword("geheim");
		sender.setKeystoreType(KeystoreType.JKS);
		sender.setKeystoreAlias("2nd");
		sender.setKeystoreAliasPassword("test2");

		sender.setMethodType(HttpMethod.GET);
		sender.configure();
		sender.start();
	}

	@Test
	public void simpleMockedHttpHead() throws Throwable {
		sender = getSender(false);
		Message input = new Message("ignored");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.HEAD);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpHead.txt"), result.trim());
	}
}
