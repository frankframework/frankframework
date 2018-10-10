package nl.nn.adapterframework.http;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.mime.MultipartEntity;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class HttpSenderTest extends Mockito {

	public HttpSender createHttpSender() throws IOException {
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

		Answer<HttpResponse> answer = new Answer<HttpResponse>() {

			private HttpResponse buildResponse(InputStream content) throws UnsupportedOperationException, IOException {
				CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
				StatusLine statusLine = mock(StatusLine.class);
				HttpEntity httpEntity = mock(HttpEntity.class);

				when(statusLine.getStatusCode()).thenReturn(200);
				when(httpResponse.getStatusLine()).thenReturn(statusLine);

				when(httpEntity.getContent()).thenReturn(content);
				when(httpResponse.getEntity()).thenReturn(httpEntity);
				return httpResponse;
			}

			@Override
			public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
				HttpHost host = (HttpHost) invocation.getArguments()[0];
				HttpRequestBase request = (HttpRequestBase) invocation.getArguments()[1];
				HttpContext context = (HttpContext) invocation.getArguments()[2];

				InputStream response = null;
				if(request instanceof HttpGet)
					response = doGet(host, (HttpGet) request, context);
				if(request instanceof HttpPost)
					response = doPost(host, (HttpPost) request, context);




				return buildResponse(response);
			}

			public InputStream doGet(HttpHost host, HttpGet request, HttpContext context) {
				assertEquals("GET", request.getMethod());
				StringBuilder response = new StringBuilder();
				String lineSeparator = System.getProperty("line.separator");
				response.append(request.toString() + lineSeparator);

				Header[] headers = request.getAllHeaders();
				for (Header header : headers) {
					response.append(header.getName() + ": " + header.getValue() + lineSeparator);
				}

				return new ByteArrayInputStream(response.toString().getBytes());
			}

			public InputStream doPost(HttpHost host, HttpPost request, HttpContext context) throws IOException {
				assertEquals("POST", request.getMethod());
				StringBuilder response = new StringBuilder();
				String lineSeparator = System.getProperty("line.separator");
				response.append(request.toString() + lineSeparator);

				Header[] headers = request.getAllHeaders();
				for (Header header : headers) {
					response.append(header.getName() + ": " + header.getValue() + lineSeparator);
				}
				HttpEntity entity = request.getEntity();
				if(entity instanceof MultipartEntity) {
					MultipartEntity multipartEntity = (MultipartEntity) entity;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					multipartEntity.writeTo(baos);
					String contentType = multipartEntity.getContentType().getValue();
					String boundary = getBoundary(contentType);
					contentType = contentType.replaceAll(boundary, "IGNORE");
					response.append("Content-Type: " + contentType + lineSeparator);

					response.append(lineSeparator);
					String content = new String(baos.toByteArray());
					content = content.replaceAll(boundary, "IGNORE");
					response.append(content);
				}
				else {
					response.append(lineSeparator);
					response.append(EntityUtils.toString(entity));
				}

				return new ByteArrayInputStream(response.toString().getBytes());
			}
			
			private String getBoundary(String contentType) {
				String boundary = contentType.substring(contentType.indexOf("boundary=")+9);
				boundary = boundary.substring(0, boundary.indexOf(";"));
				return boundary.replaceAll("\"", "");
			}
		};
		//Mock all requests
		when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenAnswer(answer);

		HttpSender sender = spy(new HttpSender());
		when(sender.getHttpClient()).thenReturn(httpClient);

		//Some default settings, url will be mocked.
		sender.setUrl("http://127.0.0.1/");
		sender.setIgnoreRedirects(true);
		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);

		return sender;
	}

	private final String BASEDIR = "/nl/nn/adapterframework/http/response/";
	private InputStream getFile(String file) throws IOException {
		URL url = this.getClass().getResource(BASEDIR+file);
		if (url == null) {
			throw new IOException("file not found");
		}
		return url.openStream();
	}

	@Test
	public void simpleMockedHttpGet() throws Throwable {
		HttpSender sender = createHttpSender();
		String input = "hallo";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedHttpGet.txt")), result);
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
		HttpSender sender = createHttpSender();
		String input = "hallo";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

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

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedHttpGetWithParams.txt")), result);
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
		HttpSender sender = createHttpSender();
		String input = "<xml>input</xml>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

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

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedHttpPost.txt")), result);
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
		HttpSender sender = createHttpSender();
		String input = "<xml>input</xml>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

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

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedHttpMultipart.txt")), result);
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
		HttpSender sender = createHttpSender();
		String input = "<xml>input</xml>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

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

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedHttpMtom.txt")), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}