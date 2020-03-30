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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

public class HttpSenderTest extends HttpSenderTestBase<HttpSender> {

	@Override
	public HttpSender createSender() {
		return spy(new HttpSender());
	}

	@Test
	public void simpleMockedHttpGetWithoutPRC() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		try {
			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, null).asString();
			assertEquals(getFile("simpleMockedHttpGetWithoutPRC.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void testContentType() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setContentType("text/xml");
		sender.configure();
		assertEquals("text/xml; charset=UTF-8", sender.getFullContentType().toString());
	}

	@Test()
	public void testCharset() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setCharSet("ISO-8859-1");
		sender.setMethodType("post");
		sender.configure();
		assertEquals("text/html; charset=ISO-8859-1", sender.getFullContentType().toString());
	}

	@Test
	public void testContentTypeAndCharset1() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.setCharSet("IsO-8859-1");
		sender.setContentType("text/xml");
		sender.configure();

		//Make sure charset is parsed properly and capital case
		assertEquals("text/xml; charset=ISO-8859-1", sender.getFullContentType().toString());
	}

	@Test
	public void testContentTypeAndCharset2() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.setContentType("application/xml");
		sender.setCharSet("uTf-8");
		sender.configure();

		//Make sure charset is parsed properly and capital case
		assertEquals("application/xml; charset=UTF-8", sender.getFullContentType().toString());
	}

	@Test
	public void parseContentTypeWithCharset() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.setContentType("text/xml; charset=ISO-8859-1");
		sender.configure();
		assertEquals("text/xml; charset=ISO-8859-1", sender.getFullContentType().toString());
	}

	@Test
	public void notContentTypeUnlessExplicitlySet() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test
		sender.configure();
		assertNull(sender.getFullContentType());
	}

	@Test()
	public void notCharsetUnlessContentTypeExplicitlySet() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setCharSet("ISO-8859-1");
		sender.configure();
		assertNull(sender.getFullContentType());
	}

	@Test
	public void simpleMockedHttpGet() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpGet.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpGetWithContentType() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("GET"); //Make sure its a GET request
			sender.setContentType("application/json");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpGetWithContentType.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpGetWithContentTypeAndCharset() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("GET"); //Make sure its a GET request
			sender.setContentType("application/json");
			sender.setCharSet("ISO-8859-1");


			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpGetWithContentTypeAndCharset.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPost() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("post"); //should handle both upper and lowercase methodtypes :)

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpPost.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPostAppendParamsToBody() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/something&dummy=true");
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("post"); //should handle both upper and lowercase methodtypes :)

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
			assertEquals(getFile("simpleMockedHttpPostAppendParamsToBody.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPostAppendParamsToBodyAndEmptyBody() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		sender.setUrl("http://127.0.0.1/something&dummy=true");
		Message input = new Message("");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("post"); //should handle both upper and lowercase methodtypes :)

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
			assertEquals(getFile("simpleMockedHttpPostAppendParamsToBodyAndEmptyBody.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPut() throws Throwable {
		HttpSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("pUT"); //should handle a mix of upper and lowercase characters :)

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpPut.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpGetWithParams() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			Parameter param1 = new Parameter();
			param1.setName("key");
			param1.setValue("value");
			sender.addParameter(param1);

			Parameter param2 = new Parameter();
			param2.setName("otherKey");
			param2.setValue("otherValue");
			sender.addParameter(param2);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpGetWithParams.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpGetWithUrlParamAndPath() throws Throwable {
		HttpSender sender = getSender();
		sender.setUrl(null); //unset URL
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

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

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpGetWithUrlParamAndPath.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpCharset() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setCharSet("ISO-8859-1");
			sender.setMethodType("POST");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpCharset.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpUnknownHeaderParam() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("hallo");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			Parameter param1 = new Parameter();
			param1.setName("key");
			param1.setValue("value");
			sender.addParameter(param1);

			Parameter param2 = new Parameter();
			param2.setName("otherKey");
			param2.setValue("otherValue");
			sender.addParameter(param2);

			sender.setMethodType("GET");
			sender.setHeadersParams("custom-header, doesn-t-exist");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpGetWithParams.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPostUrlEncoded() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			Parameter param1 = new Parameter();
			param1.setName("key");
			param1.setValue("value");
			sender.addParameter(param1);

			Parameter param2 = new Parameter();
			param2.setName("otherKey");
			param2.setValue("otherValue");
			sender.addParameter(param2);

			sender.setMethodType("POST");
			sender.setParamsInUrl(false);
			sender.setInputMessageParam("nameOfTheFirstContentId");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpPostUrlEncoded.txt"), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPostJSON() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("{\"key\": \"value\"}");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("POST");
			sender.setContentType("application/json");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpPostJSON.txt"), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPutJSON() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("{\"key\": \"value\"}");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("PUT");
			sender.setContentType("application/json");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedHttpPutJSON.txt"), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpMultipart() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("POST");
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
			assertEquals(getFile("simpleMockedHttpMultipart.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpMtom() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("POST");
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
			assertEquals(getFile("simpleMockedHttpMtom.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void parametersToSkip() throws Throwable {
		HttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			sender.setMethodType("POST");
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
			assertEquals(getFile("parametersToSkip.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}