/*
   Copyright 2018 Nationale-Nederlanden

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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.mockito.Mockito;

public class HttpSenderResultTest extends Mockito {

	public HttpSender createHttpSender() throws IOException {
		InputStream dummyXmlString = new ByteArrayInputStream("<dummy result/>".getBytes());
		return createHttpSender(dummyXmlString, null);
	}

	public HttpSender createHttpSender(InputStream responseStream, String contentType) throws IOException {
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		HttpEntity httpEntity = mock(HttpEntity.class);

		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);

		if(contentType != null) {
			Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
			when(httpEntity.getContentType()).thenReturn(contentTypeHeader);
		}
		when(httpEntity.getContent()).thenReturn(responseStream);
		when(httpResponse.getEntity()).thenReturn(httpEntity);

		//Mock all requests
		when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenReturn(httpResponse);

		HttpSender sender = spy(new HttpSender());
		when(sender.getHttpClient()).thenReturn(httpClient);

		//Some default settings, url will be mocked.
		sender.setUrl("http://127.0.0.1/");
		sender.setIgnoreRedirects(true);
		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);

		return sender;
	}

	private final String BASEDIR = "/nl/nn/adapterframework/http/";
	private InputStream getFile(String file) throws IOException {
		URL url = this.getClass().getResource(BASEDIR+file);
		if (url == null) {
			throw new IOException("file not found");
		}
		return url.openStream();
	}

	public HttpSender createHttpSenderFromFile(String testFile) throws IOException {
		InputStream file = getFile(testFile);
		byte[] fileArray = Misc.streamToBytes(file);
		String contentType = null;

		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileArray)));
		for (String line = null; null != (line = reader.readLine());) {
			if(line.startsWith("Content-Type")) {
				contentType = line.substring(line.indexOf(":") + 1).trim();
				break;
			}
		}
		reader.close();

		if(contentType != null)
			System.out.println("found Content-Type ["+contentType+"]");

		InputStream dummyXmlString = new ByteArrayInputStream(fileArray);
		return createHttpSender(dummyXmlString, contentType);
	}

	@Test
	public void simpleMockedHttpGet() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSender();

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			//Use InputStream 'content' as result.
			String result = sender.sendMessage(null, "", prc);
			assertEquals("<dummy result/>", result);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleBase64MockedHttpGet() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSender();

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("GET");
			sender.setBase64(true);

			sender.configure();
			sender.open();

			//Use InputStream 'content' as result.
			String result = sender.sendMessage(null, "", prc);
			assertEquals("PGR1bW15IHJlc3VsdC8+", result.trim());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void testBase64Decoder() throws IOException {
		HttpSender sender = createHttpSender();
		InputStream content = new ByteArrayInputStream("<dummy result/>".getBytes());
		String result = sender.getResponseBodyAsBase64(content).trim();
		assertEquals("PGR1bW15IHJlc3VsdC8+", result);
	}

	@Test
	public void simpleBase64MockedHttpPost() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSender();

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setParamsInUrl(false);
			sender.setInputMessageParam("inputMessageParam");
			sender.setMethodType("POST");
			sender.setBase64(true);

			sender.configure();
			sender.open();

			//Use InputStream 'content' as result.
			String result = sender.sendMessage(null, "tralala", prc);
			assertEquals("PGR1bW15IHJlc3VsdC8+", result.trim());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleByteArrayInSessionKeyMockedHttpGet() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSender();
		String SESSIONKEY_KEY = "result";
		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("GET");
			sender.setStoreResultAsByteArrayInSessionKey(SESSIONKEY_KEY);

			sender.configure();
			sender.open();

			//Use InputStream 'content' as result.
			String result = sender.sendMessage(null, "tralala", prc);
			assertEquals("", result);
			pls = prc.getSession();

			byte[] byteArray = (byte[])pls.get(SESSIONKEY_KEY);
			assertEquals("<dummy result/>", new String(byteArray, "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleByteArrayInSessionKeyMockedHttpPost() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSender();
		String SESSIONKEY_KEY = "result";
		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("POST");
			sender.setStoreResultAsByteArrayInSessionKey(SESSIONKEY_KEY);

			sender.configure();
			sender.open();

			//Use InputStream 'content' as result.
			String result = sender.sendMessage(null, "tralala", prc);
			assertEquals("", result);
			pls = prc.getSession();

			byte[] byteArray = (byte[])pls.get(SESSIONKEY_KEY);
			assertEquals("<dummy result/>", new String(byteArray, "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleResultAsStreamMockedHttpGet() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSender();
		String SESSIONKEY_KEY = "result";
		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("GET");
			sender.setStoreResultAsStreamInSessionKey(SESSIONKEY_KEY);

			sender.configure();
			sender.open();

			//Use InputStream 'content' as result.
			String result = sender.sendMessage(null, "tralala", prc);
			assertEquals("", result);
			pls = prc.getSession();

			InputStream stream = (InputStream)pls.get(SESSIONKEY_KEY);
			assertEquals("<dummy result/>", Misc.streamToString(stream));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleResultAsStreamMockedHttpPost() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSender();
		String SESSIONKEY_KEY = "result";
		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("POST");
			sender.setStoreResultAsStreamInSessionKey(SESSIONKEY_KEY);

			sender.configure();
			sender.open();

			//Use InputStream 'content' as result.
			String result = sender.sendMessage(null, "tralala", prc);
			assertEquals("", result);
			pls = prc.getSession();

			InputStream stream = (InputStream)pls.get(SESSIONKEY_KEY);
			assertEquals("<dummy result/>", Misc.streamToString(stream));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMultiPartResponseMockedHttpGet() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSenderFromFile("multipart1.txt");

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("GET");
			sender.setMultipartResponse(true);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, "tralala", prc);
			assertEquals("text default", result);
			pls = prc.getSession();

			int multipartAttachmentCount = 0;
			for (Map.Entry<String, Object> entry : pls.entrySet()) {
				System.out.println("found multipart ["+entry.getKey()+"]");
				multipartAttachmentCount++;
			}
			assertEquals(2, multipartAttachmentCount);

			InputStream multipart1 = (InputStream)pls.get("multipart1");
			assertEquals("Content of a txt file.", Misc.streamToString(multipart1).trim());

			InputStream multipart2 = (InputStream)pls.get("multipart2");
			assertEquals("<!DOCTYPE html><title>Content of a html file.</title>", Misc.streamToString(multipart2).trim());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMtomResponseMockedHttpGet() throws ClientProtocolException, IOException {
		HttpSender sender = createHttpSenderFromFile("mtom-multipart.txt");

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext("dummy prc", pls);

			sender.setMethodType("GET");
			sender.setMultipartResponse(true);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, "tralala", prc);
			assertEquals("<soap:Envelope/>", result.trim());

			pls = prc.getSession();
			int multipartAttachmentCount = 0;
			for (Map.Entry<String, Object> entry : pls.entrySet()) {
				System.out.println("found multipart key["+entry.getKey()+"] type["+(entry.getValue().getClass())+"]");
				multipartAttachmentCount++;
			}
			assertEquals(1, multipartAttachmentCount);

			InputStream multipart1 = (InputStream)pls.get("multipart1");
			assertEquals("PDF-1.4 content", Misc.streamToString(multipart1).trim());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}