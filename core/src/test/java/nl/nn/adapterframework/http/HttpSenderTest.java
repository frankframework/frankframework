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
package nl.nn.adapterframework.http;

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.http.HttpSender.PostType;
import nl.nn.adapterframework.http.HttpSenderBase.HttpMethod;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class HttpSenderTest extends HttpSenderTestBase<HttpSender> {

	@Override
	public HttpSender createSender() {
		return spy(new HttpSender());
	}

	@Test
	public void relativeUrl() throws Throwable {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("must use an absolute url starting with http(s)://");

		sender = getSender(false); //Cannot add headers (aka parameters) for this test!

		sender.setMethodType(HttpMethod.GET);
		sender.setUrl("relative/path");

		sender.configure();
	}

	@Test
	public void relativeUrlParameter() throws Throwable {
		exception.expect(SenderException.class);
		exception.expectMessage("must use an absolute url starting with http(s)://");

		sender = getSender(false); //Cannot add headers (aka parameters) for this test!

		sender.setMethodType(HttpMethod.GET);
		sender.setUrl(null);
		Parameter urlParam = new Parameter();
		urlParam.setName("url");
		urlParam.setValue("relative/path");
		sender.addParameter(urlParam);

		sender.configure();
		sender.open();

		sender.sendMessage(null, null);
	}

	@Test
	public void simpleMockedHttpGetWithoutPRC() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithoutPRC.txt"), result.trim());
	}

	@Test
	public void testContentType() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setContentType("text/xml");
		sender.configure();
		assertEqualsIgnoreCRLF("text/xml; charset=UTF-8", sender.getFullContentType().toString());
	}

	@Test()
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
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGet.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetEncodeMessage() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("this is my dynamic url");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.GET);
		sender.setEncodeMessages(true);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
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
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
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
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithContentTypeAndCharset.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPost() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo this is my message");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPost.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostEncodeMessage() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo dit is mijn bericht");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)
		sender.setEncodeMessages(true);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostEncodeMessage.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostAppendParamsToBody() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/something&dummy=true");
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		Parameter param1 = new Parameter();
		param1.setName("key");
		param1.setValue("value");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostAppendParamsToBody.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostAppendParamsToBodyAndEmptyBody() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/something&dummy=true");
		Message input = new Message("");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		Parameter param1 = new Parameter();
		param1.setName("key");
		param1.setValue("value");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostAppendParamsToBodyAndEmptyBody.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPut() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.PUT); //should handle a mix of upper and lowercase characters :)

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPut.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPatch() throws Throwable {
		sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo patch request");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.PATCH); //should handle a mix of upper and lowercase characters :)

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPatch.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetWithParams() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("key");
		param1.setValue("value");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithParams.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpGetWithUrlParamAndPath() throws Throwable {
		sender = getSender();
		sender.setUrl(null); //unset URL
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter urlParam = new Parameter();
		urlParam.setName("url");
		urlParam.setValue("http://127.0.0.1/value%20value?path=tralala");
		sender.addParameter(urlParam);

		Parameter param1 = new Parameter();
		param1.setName("illegalCharacters");
		param1.setValue("o@t&h=e+r$V,a/lue");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("normalCharacters");
		param2.setValue("helloWorld");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithUrlParamAndPath.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpCharset() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setCharSet("ISO-8859-1");
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpCharset.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpUnknownHeaderParam() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("key");
		param1.setValue("value");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.GET);
		sender.setHeadersParams("custom-header, doesn-t-exist");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpGetWithParams.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPostUrlEncoded() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("key");
		param1.setValue("value");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setInputMessageParam("nameOfTheFirstContentId");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostUrlEncoded.txt"), result);
	}

	@Test
	public void postTypeUrlEncoded() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("key");
		param1.setValue("value");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.POST);
		sender.setPostType(PostType.URLENCODED);
		sender.setInputMessageParam("nameOfTheFirstContentId");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
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
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPostJSON.txt"), result);
	}

	@Test
	public void binaryHttpPostJSON() throws Throwable {
		sender = getSender();
		Message input = new Message(new ByteArrayInputStream("{\"key1\": \"value2\"}".getBytes())); //Let's pretend this is a big JSON stream!
		assertTrue("input message has to be of type binary", input.isBinary());

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setContentType("application/json");
		sender.setPostType(PostType.BINARY);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("binaryHttpPostJSON.txt"), result);
	}

	@Test
	public void binaryHttpPostPDF() throws Throwable {
		sender = getSender();
		URL url = TestFileUtils.getTestFileURL("/Documents/doc001.pdf");
		Message input = new UrlMessage(url);
		assertTrue("input message has to be a binary file", input.isBinary());

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setContentType("application/pdf");
		sender.setPostType(PostType.BINARY);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
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
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpPutJSON.txt"), result);
	}

	@Test
	public void binaryHttpPutJSON() throws Throwable {
		sender = getSender();
		Message input = new Message(new ByteArrayInputStream("{\"key1\": \"value2\"}".getBytes())); //Let's pretend this is a big JSON stream!
		assertTrue("input message has to be of type binary", input.isBinary());

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.PUT);
		sender.setContentType("application/json");
		sender.setPostType(PostType.BINARY);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("binaryHttpPutJSON.txt"), result);
	}

	@Test
	public void simpleMockedHttpMultipart() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setInputMessageParam("request");

		String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
				+ "sessionKey=\"part_file\" size=\"72833\" "
				+ "mimeType=\"application/pdf\"/></parts>";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMultipart.txt"), result.trim());
	}

	@Test
	public void postTypeMultipart() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setPostType(PostType.FORMDATA);
		sender.setInputMessageParam("request");

		String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
				+ "sessionKey=\"part_file\" size=\"72833\" "
				+ "mimeType=\"application/pdf\"/></parts>";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMultipart.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpMtom() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setInputMessageParam("request");

		String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
				+ "sessionKey=\"part_file\" size=\"72833\" "
				+ "mimeType=\"application/pdf\"/></parts>";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMtomEnabled(true);
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMtom.txt"), result.trim());
	}

	@Test
	public void postTypeMtom() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setPostType(PostType.MTOM);
		sender.setInputMessageParam("request");

		String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
				+ "sessionKey=\"part_file\" size=\"72833\" "
				+ "mimeType=\"application/pdf\"/></parts>";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMtomEnabled(true);
		sender.setMultipartXmlSessionKey("multipartXml");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("simpleMockedHttpMtom.txt"), result.trim());
	}

	@Test
	public void parametersToSkip() throws Throwable {
		sender = getSender();
		Message input = new Message("<xml>input</xml>");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST);
		sender.setParamsInUrl(false);
		sender.setInputMessageParam("request");

		String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
				+ "sessionKey=\"part_file\" size=\"72833\" "
				+ "mimeType=\"application/pdf\"/></parts>";
		pls.put("multipartXml", xmlMultipart);
		pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		sender.setMtomEnabled(true);
		sender.setMultipartXmlSessionKey("multipartXml");

		Parameter urlParam = new Parameter();
		urlParam.setName("url");
		urlParam.setValue("http://ignore.me");
		sender.addParameter(urlParam);

		Parameter partParam = new Parameter();
		partParam.setName("my-beautiful-part");
		partParam.setValue("<partContent/>");
		sender.addParameter(partParam);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("parametersToSkip.txt"), result.trim());
	}


	@Test
	public void specialCharactersInURLParam() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("url");
		param1.setValue("http://127.0.0.1/value%20value?param=Hello%20G%C3%BCnter");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("specialCharactersInURLParam.txt"), result.trim());
	}

	@Test
	public void specialCharactersDoubleEscaped() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("url");
		param1.setValue("HTTP://127.0.0.1/value%2Fvalue?param=Hello%2520%2FG%C3%BCnter");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("otherKey");
		param2.setValue("otherValue");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("specialCharactersDoubleEscaped.txt"), result.trim());
	}

	@Test(expected = SenderException.class)
	public void unsupportedScheme() throws Throwable {
		sender = getSender();
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("url");
		param1.setValue("ftp://127.0.0.1/value%2Fvalue?param=Hello%2520%2FG%C3%BCnter");
		sender.addParameter(param1);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.open();

		sender.sendMessage(input, pls).asString();

		// We expect sendMessage to throw expection
		assertTrue(false);
	}

	@Test
	public void paramsWithoutValue() throws Throwable {
		sender = getSender();
		Message input = new Message("paramterValue");

		PipeLineSession pls = new PipeLineSession(session);

		Parameter param1 = new Parameter();
		param1.setName("url");
		param1.setValue("http://127.0.0.1/value%2Fvalue?emptyParam");
		sender.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("myParam");
		param2.setValue("");
		sender.addParameter(param2);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString();
		assertEqualsIgnoreCRLF(getFile("paramsWithoutValue.txt"), result.trim());
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
		sender.open();

	}

	@Test
	public void testUsingSamePasswordForKeystoreAndKeyPairHavingDifferentPasswords() throws Exception { // keystore and the key pair have different password
		String keystore = "/Signature/ks_multipassword.jks";

		sender = getSender();
		sender.setKeystore(keystore);
		sender.setKeystorePassword("geheim");
		sender.setKeystoreType(KeystoreType.JKS);

		sender.setMethodType(HttpMethod.GET);
		exception.expect(SenderException.class);
		exception.expectMessage("cannot create or initialize SocketFactory");
		sender.configure();
		sender.open();

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
		sender.open();

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
		exception.expect(SenderException.class);
		exception.expectMessage("cannot create or initialize SocketFactory");
		sender.configure();
		sender.open();

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
		sender.open();

	}
}