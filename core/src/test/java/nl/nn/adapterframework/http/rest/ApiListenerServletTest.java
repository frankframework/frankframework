/*
Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.http.rest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.encryption.PkiUtil;
import nl.nn.adapterframework.http.HttpMessageEntity;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.http.rest.ApiListener.AuthenticationMethods;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;

public class ApiListenerServletTest extends Mockito {
	private Logger log = LogUtil.getLogger(this);
	private List<ApiListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private static final String JWT_VALIDATION_URI="/jwtvalidator";

	private static final String PAYLOAD="{\"sub\":\"UnitTest\",\"aud\":\"Framework\",\"iss\":\"JWTPipeTest\",\"jti\":\"1234\"}";

	enum Methods {
		GET,POST,PUT,DELETE,OPTIONS
	}

	enum AuthMethods {
		COOKIE,HEADER,AUTHROLE
	}

	private ApiListenerServlet servlet;
	private PipeLineSession session = null;
	private Message requestMessage = null;
	private boolean handlerInvoked;

	@BeforeEach
	public void setUp() throws ServletException {
		servlet = spy(ApiListenerServlet.class);
		ServletConfig servletConfig = new MockServletConfig();
		when(servlet.getServletConfig()).thenReturn(servletConfig);
		servlet.init();

		session = null;
		handlerInvoked = false;
	}

	@AfterEach
	public void tearDown() {
		for(ApiListener listener : listeners) {
			listener.close();
		}
		listeners.clear();

		servlet.destroy();
		servlet = null;
	}

	@BeforeAll
	public static void beforeClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	@AfterAll
	public static void afterClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	private HttpServletRequest createRequest(String uriPattern, Methods method) {
		return createRequest(uriPattern, method, null, null);
	}

	private HttpServletRequest createRequest(String uriPattern, Methods method, String content) {
		return createRequest(uriPattern, method, content, null);
	}

	private HttpServletRequest createRequest(String uriPattern, Methods method, HttpEntity entity) throws IOException {
		ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
		entity.writeTo(requestContent);
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", entity.getContentType().getValue());
		headers.put("Content-Length", entity.getContentLength()+"");

		return doCreateRequest(uriPattern, method, requestContent.toByteArray(), headers);
	}

	private MockHttpServletRequest createRequest(String uriPattern, Methods method, String content, Map<String, String> headers) {
		return doCreateRequest(uriPattern, method, (content==null)? "".getBytes():content.getBytes(), headers);
	}

	private MockHttpServletRequest doCreateRequest(String uriPattern, Methods method, byte[] content, Map<String, String> headers) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(method.name());
		if(uriPattern == null) return request; // there's no path nor any params, return a blank ServletRequest

		String path = uriPattern;
		URIBuilder uri;
		try {
			uri = new URIBuilder(uriPattern);
			List<NameValuePair> queryParameters = uri.getQueryParams();
			Map<String, String[]> parameters = new HashMap<>();
			for (NameValuePair nameValuePair : queryParameters) {
				String name = nameValuePair.getName();
				String value = nameValuePair.getValue();
				if(!parameters.containsKey(name)) {
					parameters.put(name, new String[] { value });
				} else {
					String[] values = parameters.remove(name);
					parameters.put(name, ArrayUtils.add(values, value));
				}
			}
			request.setParameters(parameters);
			path = uri.getPath();
		} catch (URISyntaxException e) {
			fail("invalid url ["+uriPattern+"] cannot parse");
		}

		request.setPathInfo(path);

		if(headers != null) {
			for (String name : headers.keySet()) {
				request.addHeader(name, headers.get(name));
			}
		}

		if(!method.equals(Methods.GET)) {
			request.setContent(content);
		}

		return request;
	}

	private Response service(HttpServletRequest request) throws ServletException, IOException {
		MockHttpServletResponse response = new StricterMockHttpServletResponse();

		servlet.service(request, response);

		return new Response(response);
	}

	@Test
	public void noUri() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		new ApiListenerBuilder("test", Methods.GET).build();

		// Act
		Response result = service(createRequest(null, Methods.GET));

		// Assert
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(400, result.getStatus());
	}

	@Test
	public void uriNotFound() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		new ApiListenerBuilder("test", Methods.GET).build();

		// Act
		Response result = service(createRequest("/not-test", Methods.GET));

		// Assert
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(404, result.getStatus());
	}

	@Test
	public void methodNotAllowed() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/test";
		new ApiListenerBuilder(uri, Methods.GET).build();

		Response result = service(createRequest(uri, Methods.PUT));
		assertEquals(405, result.getStatus());
		assertNull(result.getErrorMessage());
	}

	@Test
	public void simpleGet() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/test1";
		new ApiListenerBuilder(uri, Methods.GET).build();

		Response result = service(createRequest(uri, Methods.GET));
		assertEquals(200, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testAfterServiceMethodThreadContextMustBeCleared() throws ServletException, IOException, ListenerException, ConfigurationException {
		// arrange
		ThreadContext.put("fakeMdcKey", "fakeContextValue");
		ThreadContext.push("fakeNdcKey", "fakeStackItem");

		// act
		simpleGet();

		// assert
		assertEquals(0, ThreadContext.getDepth());
		assertTrue(ThreadContext.isEmpty());
	}

	@Test
	public void simpleGetWithSlashes() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/test2";
		new ApiListenerBuilder(uri, Methods.GET).build();

		Response result = service(createRequest(uri, Methods.GET));
		assertEquals(200, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void preFlightRequest() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/preflight/";
		new ApiListenerBuilder(uri, Methods.GET).build();

		Response result = service(createRequest(uri, Methods.OPTIONS));
		assertEquals(200, result.getStatus());
		assertEquals("", result.getContentAsString()); //Pre-flight requests have no data
		assertNull(result.getErrorMessage());
		assertTrue(result.containsHeader("Access-Control-Allow-Origin"));
		assertFalse(result.containsHeader("Access-Control-Allow-Headers"));
		assertTrue(result.containsHeader("Access-Control-Expose-Headers"));
		assertTrue(result.containsHeader("Access-Control-Allow-Methods"));
	}

	@Test
	public void preFlightRequestWithRequestHeader() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/preflight/";
		new ApiListenerBuilder(uri, Methods.GET).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Access-Control-Request-Headers", "Message-Id,CustomHeader");
		Response result = service(createRequest(uri, Methods.OPTIONS, null, headers));
		assertEquals(200, result.getStatus());
		assertEquals("", result.getContentAsString()); //Pre-flight requests have no data
		assertNull(result.getErrorMessage());
		assertTrue(result.containsHeader("Access-Control-Allow-Origin"));
		assertTrue(result.containsHeader("Access-Control-Allow-Headers"));
		assertEquals("Message-Id,CustomHeader", result.getHeader("Access-Control-Allow-Headers"));
		assertTrue(result.containsHeader("Access-Control-Expose-Headers"));
		assertTrue(result.containsHeader("Access-Control-Allow-Methods"));
	}

	@Test
	public void corsGetRequest() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/not-preflight/";
		new ApiListenerBuilder(uri, Methods.POST).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("origin", "https://ibissource.org");
		headers.put("Access-Control-Request-Headers", "Message-Id,CustomHeader");
		Response result = service(createRequest(uri, Methods.POST, "data", headers));
		assertEquals(200, result.getStatus());
		assertEquals("data", result.getContentAsString());
		assertNull(result.getErrorMessage());
		assertTrue(result.containsHeader("Access-Control-Allow-Origin"));
		assertTrue(result.containsHeader("Access-Control-Allow-Headers"));
		assertEquals("Message-Id,CustomHeader", result.getHeader("Access-Control-Allow-Headers"));
		assertTrue(result.containsHeader("Access-Control-Expose-Headers"));
		assertTrue(result.containsHeader("Access-Control-Allow-Methods"));
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
	}

	@Test
	public void apiListenerThatProducesXML() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/ApiListenerThatProducesXML/";
		new ApiListenerBuilder(uri, Methods.PUT, null, MediaTypes.XML).build();

		Response result = service(createRequest(uri, Methods.PUT, "<xml>data</xml>"));
		assertEquals(200, result.getStatus());
		assertEquals("<xml>data</xml>", result.getContentAsString());
		assertEquals("OPTIONS, PUT", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/xml"), "Content-Type header does not contain [application/xml]");
		assertNull(result.getErrorMessage());
	}

	@Test
	public void apiListenerThatProducesJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/ApiListenerThatProducesJSON/";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.JSON).build();

		Response result = service(createRequest(uri, Methods.POST, "{}"));
		assertEquals(200, result.getStatus());
		assertEquals("{}", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertNull(result.getErrorMessage());
	}

	@Test
	public void apiListenerThatProducesJSONReturnsNoOutput() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri="/ApiListenerThatProducesJSONReturnsNoOutput/";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.JSON)
			.withResponseContent("")
			.build();
		HttpServletRequest request = createRequest(uri, Methods.POST, "{}");

		// Act
		Response result = service(request);

		// Assert
		assertAll(
			() -> assertEquals(200, result.getStatus()),
			() -> assertEquals("", result.getContentAsString(), "Content found but was not expected"),
			() -> assertEquals("OPTIONS, POST", result.getHeader("Allow")),
			() -> assertNull(result.getContentType(), "Content-Type header not supposed to be set"),
			() -> assertEquals(0, result.response.getContentLength(), "Content-Length header not supposed to be set"),
			() -> assertNull(result.getErrorMessage())
		);
	}

	@Test
	public void apiListenerThatProducesJSONReturnsNoOutputEmptyStream() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri="/ApiListenerThatProducesJSONReturnsNoOutput/";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.JSON)
			.withResponseContent(new ByteArrayInputStream(new byte[0]))
			.build();
		HttpServletRequest request = createRequest(uri, Methods.POST, "{}");

		// Act
		Response result = service(request);

		// Assert
		assertAll(
			() -> assertEquals(200, result.getStatus()),
			() -> assertEquals("", result.getContentAsString(), "Content found but was not expected"),
			() -> assertEquals("OPTIONS, POST", result.getHeader("Allow")),
			() -> assertNull(result.getContentType(), "Content-Type header not supposed to be set"),
			() -> assertEquals(0, result.response.getContentLength(), "Content-Length header not supposed to be set"),
			() -> assertNull(result.getErrorMessage())
		);
	}

	@Test
	public void apiListenerThatProducesJSONReturnsNoOutputEmptyReader() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri="/ApiListenerThatProducesJSONReturnsNoOutput/";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.JSON)
			.withResponseContent(new StringReader(""))
			.build();
		HttpServletRequest request = createRequest(uri, Methods.POST, "{}");

		// Act
		Response result = service(request);

		// Assert
		assertAll(
			() -> assertEquals(200, result.getStatus()),
			() -> assertEquals("", result.getContentAsString(), "Content found but was not expected"),
			() -> assertEquals("OPTIONS, POST", result.getHeader("Allow")),
			() -> assertNull(result.getContentType(), "Content-Type header not supposed to be set"),
			() -> assertEquals(0, result.response.getContentLength(), "Content-Length header not supposed to be set"),
			() -> assertNull(result.getErrorMessage())
		);
	}

	@Test
	public void apiListenerThatProducesXMLReturnsNoOutputNonStringResultMessage() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri="/ApiListenerThatProducesXMLReturnsNoOutputNonStringResultMessage/";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.XML).build();
		Map<String, String> headers = new HashMap<>();

		HttpServletRequest request = createRequest(uri, Methods.POST, null, headers);

		// Act
		Response result = service(request);

		// Assert
		assertAll(
			() -> assertEquals(200, result.getStatus()),
			() -> assertEquals("", result.getContentAsString(), "Content found but was not expected"),
			() -> assertEquals("OPTIONS, POST", result.getHeader("Allow")),
			() -> assertNull(result.getContentType(), "Content-Type header not supposed to be set"),
			() -> assertEquals(0, result.response.getContentLength(), "Content-Length header not supposed to be set"),
			() -> assertNull(result.getErrorMessage())
		);
	}

	@Test
	public void clientAcceptHeaderDoesNotLikeJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri="/ApiListenerAllow/";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.JSON)
			.build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/xml");
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{}", headers);

		// Act
		Response result = service(request);

		// Assert
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(406, result.getStatus());
	}

	@Test
	public void clientAcceptHeaderLovesJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/ApiListenerAllow/";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.JSON).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		Response result = service(createRequest(uri, Methods.POST, "{}", headers));
		assertEquals(200, result.getStatus());
		assertEquals("{}", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerDoesNotLikeContentTypeJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerDoesNotAcceptContentType";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.XML, null).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri, Methods.POST, "{}", headers));
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(415, result.getStatus());
	}

	@Test
	public void listenerLovesContentTypeJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerLovesContentTypeJSON";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.JSON, MediaTypes.JSON).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json; charset=UTF-8");
		Response result = service(createRequest(uri, Methods.POST, "{}", headers));
		assertEquals(200, result.getStatus());
		assertEquals("{}", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerRejectsRequestWithoutContentTypeHeaderWhenConsumesAttributeSet() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri="/listenerDoesNotAcceptRequestWithoutContentTypeHeader";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.XML, null).build();

		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json");
		HttpServletRequest request = createRequest(uri, Methods.POST, "{}", headers);

		// Act
		Response result = service(request);

		// Assert
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(415, result.getStatus());
	}

	@Test
	public void listenerAcceptsRequestWithoutContentTypeHeaderWhenConsumesAttributeNotSet() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri="/listenerAcceptsContentTypeJSON";
		new ApiListenerBuilder(uri, Methods.POST, null, MediaTypes.JSON).build();

		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json");
		HttpServletRequest request = createRequest(uri, Methods.POST, "{}", headers);

		// Act
		Response result = service(request);

		// Assert
		assertEquals(200, result.getStatus());
		assertEquals("{}", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerDetectContentTypeAndCharsetISO8859() throws Exception {
		String uri="/listenerDetectMimeType";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.TEXT, MediaTypes.DETECT).build();

		URL url = ClassUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);

		// It does not need to compute the MimeType as the value should be provided by the HttpEntity
		Response result = service(createRequest(uri, Methods.POST, new HttpMessageEntity(message, ContentType.parse("text/plain;charset=iso-8859-1"))));
		assertEquals(200, result.getStatus());
		assertTrue(result.getContentType().contains("text/plain"), "Content-Type header does not contain [text/plain]");
		assertTrue(result.getContentType().contains("charset=ISO-8859-1"), "Content-Type header does not contain correct [charset]");
		assertEquals(message.asString("ISO-8859-1"), result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerMultipartContentDetectContentTypeAndCharsetISO8859() throws Exception {
		String uri="/listenerMultipartContent_DETECT";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.DETECT).build();

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("gc0p4Jq0M2Yt08jU534c0p");
		builder.addTextBody("string1", "<hello>â¬ Ã¨</hello>");//Defaults to 'text/plain;charset=ISO-8859-1'

		URL url1 = ClassUtils.getResourceURL("/Documents/doc001.pdf");
		builder.addBinaryBody("file1", url1.openStream(), ContentType.APPLICATION_OCTET_STREAM, "file1");

		URL url2 = ClassUtils.getResourceURL("/Documents/doc002.pdf");
		builder.addBinaryBody("file2", url2.openStream(), ContentType.APPLICATION_OCTET_STREAM, "file2");

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertTrue(result.getContentType().contains("text/plain"), "Content-Type header does not contain [text/plain]");
		assertTrue(result.getContentType().contains("charset=ISO-8859-1"), "Content-Type header does not contain correct [charset]");
		assertEquals("<hello>â¬ Ã¨</hello>", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerMultipartContentISO8859_JSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMultipartContent_JSON";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.JSON).build();

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("gc0p4Jq0M2Yt08jU534c0p");
		builder.addTextBody("string1", "<hello>â¬ Ã¨</hello>");// ISO_8859_1 encoded but is since we don't set the charset, it will be parsed as UTF-8 (€ è)

		URL url1 = ClassUtils.getResourceURL("/Documents/doc001.pdf");
		builder.addPart("file1", new UrlMessage(url1));

		URL url2 = ClassUtils.getResourceURL("/Documents/doc002.pdf");
		builder.addPart("file2", new UrlMessage(url2));

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertTrue(result.getContentType().contains("charset=UTF-8"), "Content-Type header does not contain correct [charset]");
		assertEquals("<hello>€ è</hello>", result.getContentAsString()); //Parsed as UTF-8
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerMultipartContentISO8859_ANY() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMultipartContent_ANY";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.ANY).build();

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("gc0p4Jq0M2Yt08jU534c0p");
		builder.addTextBody("string1", "<hello>â¬ Ã¨</hello>");// ISO_8859_1 encoded but is since we don't set the charset, it will be parsed as UTF-8 (€ è)

		URL url1 = ClassUtils.getResourceURL("/Documents/doc001.pdf");
		builder.addBinaryBody("file1", url1.openStream(), ContentType.APPLICATION_OCTET_STREAM, "file1");

		URL url2 = ClassUtils.getResourceURL("/Documents/doc002.pdf");
		builder.addBinaryBody("file2", url2.openStream(), ContentType.APPLICATION_OCTET_STREAM, "file2");

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertTrue(result.getContentType().contains("text/plain"), "Content-Type header does not contain [application/json]");
		assertTrue(result.getContentType().contains("charset=ISO-8859-1"), "Content-Type header does not contain correct [charset]");
		assertEquals("<hello>â¬ Ã¨</hello>", result.getContentAsString()); //Parsed as ISO-8859-1
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerMultipartContentUTF8() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMultipartContentCharset";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.JSON, null, "string2").build();

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addTextBody("string1", "<hallo>€ è</hallo>", ContentType.create("text/plain"));
		builder.addTextBody("string2", "<hello>€ è</hello>", ContentType.create("text/plain", "UTF-8"));//explicitly sent as UTF-8

		URL url1 = ClassUtils.getResourceURL("/test1.xml");
		assertNotNull(url1);
		builder.addBinaryBody("file1", url1.openStream(), ContentType.APPLICATION_XML, "file1");

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertEquals("<hello>€ è</hello>", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertTrue(result.getContentType().contains("charset=UTF-8"), "Content-Type header does not contain correct [charset]");
		assertNull(result.getErrorMessage());

		String multipartXml = (String) session.get("multipartAttachments");
		assertNotNull(multipartXml);
		MatchUtils.assertXmlEquals("<parts>\n"
				+ "  <part name=\"string1\" type=\"text\" value=\"&lt;hallo&gt;? ?&lt;/hallo&gt;\" />\n"
				+ "  <part name=\"string2\" type=\"text\" value=\"&lt;hello&gt;€ è&lt;/hello&gt;\" />\n"
				+ "  <part name=\"file1\" type=\"file\" filename=\"file1\" size=\"1041\" sessionKey=\"file1\" mimeType=\"application/xml\"/>\n"
				+ "</parts>", multipartXml);
		Message file = (Message) session.get("file1");
		assertEquals("ISO-8859-1", file.getCharset());
	}

	@Test
	public void listenerMtomContent() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMtomContent";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.MULTIPART_RELATED, MediaTypes.JSON).build();

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMtomMultipart();
		builder.addTextBody("string1", "<hello>€ è</hello>", ContentType.create("text/xml", "UTF-8"));//explicitly sent as UTF-8, defaults to ISO-8859-1

		URL url1 = ClassUtils.getResourceURL("/test1.xml");
		assertNotNull(url1);
		builder.addBinaryBody("file1", url1.openStream(), ContentType.APPLICATION_XML, "file1");

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertEquals("<hello>€ è</hello>", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertTrue(result.getContentType().contains("charset=UTF-8"), "Content-Type header does not contain correct [charset]");
		assertNull(result.getErrorMessage());

		String multipartXml = (String) session.get("multipartAttachments");
		assertNotNull(multipartXml);
		MatchUtils.assertXmlEquals("<parts>\n"
				+ "  <part name=\"string1\" type=\"text\" value=\"&lt;hello&gt;€ è&lt;/hello&gt;\" />\n"
				+ "  <part name=\"file1\" type=\"file\" filename=\"file1\" size=\"1041\" sessionKey=\"file1\" mimeType=\"application/xml\"/>\n"
				+ "</parts>", multipartXml);
		Message file = (Message) session.get("file1");
		assertEquals("ISO-8859-1", file.getCharset());
	}

	@Test
	public void listenerMultipartContentNoContentType() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMultipartContentNoContentType";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.JSON).build();

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addTextBody("string1", "<request/>", ContentType.create("text/xml"));//explicitly sent as UTF-8

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertEquals("<request/>", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue(result.getContentType().contains("application/json"), "Content-Type header does not contain [application/json]");
		assertTrue(result.getContentType().contains("charset=UTF-8"), "Content-Type header does not contain correct [charset]");
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerInvalidMultipartContent() throws ServletException, IOException, ListenerException, ConfigurationException {

		// Arrange
		String uri="/listenerMultipartContentNoContent";
		new ApiListenerBuilder(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.JSON).build();

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		HttpServletRequest request = createRequest(uri, Methods.POST, builder.build());

		// Act
		Response result = service(request);

		// Assert
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(400, result.getStatus());
		assertEquals("Could not read mime multipart response", result.getErrorMessage());
	}

	@Test
	public void getRequestWithQueryParameters() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/queryParamTest";
		new ApiListenerBuilder(uri, Methods.GET).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri+"?name=JOHN-DOE&GENDER=MALE", Methods.GET, null, headers));

		assertEquals(200, result.getStatus());
		assertEquals("JOHN-DOE", session.get("name"));
		assertEquals("MALE", session.get("GENDER"));
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void getRequestWithQueryListParameters() throws Exception {
		String uri="/queryParamTestWithListsItems";
		new ApiListenerBuilder(uri, Methods.GET).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri+"?transport=car&transport=bike&transport=moped&maxSpeed=60", Methods.GET, null, headers));

		assertEquals(200, result.getStatus());
		assertEquals("60", session.get("maxSpeed"));
		List<String> transportList = Arrays.asList("car","bike","moped");
		assertEquals(transportList, session.get("transport"));
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void getRequestWithDynamicPath() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/dynamic/";
		new ApiListenerBuilder(uri+"{poef}", Methods.GET).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri+"tralala", Methods.GET, null, headers));

		assertEquals(200, result.getStatus());
		assertEquals("tralala", session.get("poef"));
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void getRequestWithAsteriskPath() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/dynamic/";
		new ApiListenerBuilder(uri+"*", Methods.GET).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri+"tralala", Methods.GET, null, headers));

		assertEquals(200, result.getStatus());
		assertEquals("tralala", session.get("uriIdentifier_0"));
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void customExitCode() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/exitcode";
		new ApiListenerBuilder(uri, Methods.GET)
			.withExitCode(234)
			.build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		assertEquals(234, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void apiListenerWithRepeatableMessageShouldReturnEtag() throws Exception {
		// Arrange
		String uri="/etag1";
		Message repeatableMessage = new Message("{\"tralalalallala\":true}", new MessageContext().withModificationTime("2023-01-13 14:02:00"));
		new ApiListenerBuilder(uri, Methods.GET)
			.withResponseContent(repeatableMessage)
			.build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");

		// Act
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		// Assert
		assertEquals(200, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
		assertTrue(result.containsHeader("etag"));
		assertEquals("must-revalidate, max-age=0, post-check=0, pre-check=0", result.getHeader("Cache-Control"));
		assertFalse(result.containsHeader("pragma"));
		assertEquals("Fri, 13 Jan 2023 13:02:00 GMT", result.getHeader("Last-Modified"));
	}

	@Test
	public void apiListenerWithNonRepeatableMessageShouldNotReturnEtag() throws Exception {
		// Arrange
		String uri="/etag2";
		Message repeatableMessage = Message.asMessage(new Message("{\"tralalalallala\":true}").asByteArray());
		Message nonRepeatableMessage = new Message(new FilterInputStream(repeatableMessage.asInputStream()) {}, new MessageContext().withModificationTime("2023-01-13 14:02:00"));
		new ApiListenerBuilder(uri, Methods.GET)
			.withResponseContent(nonRepeatableMessage)
			.build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");

		// Act
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		// Assert
		assertEquals(200, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
		assertFalse(result.containsHeader("etag"));
		assertEquals("no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0", result.getHeader("Cache-Control"));
		assertTrue(result.containsHeader("pragma"));
		assertEquals("Fri, 13 Jan 2023 13:02:00 GMT", result.getHeader("Last-Modified"));
	}

	@Test
	public void eTagGetEtagMatches() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri = "/etag31";
		new ApiListenerBuilder(uri, Methods.GET)
			.withExitCode(201)
			.withResponseContent("{\"tralalalallala\":true}")
			.build();
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-none-match", "my-etag-value");

		// Act
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		// Assert
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, etag should have matched");
		assertEquals(304, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void eTagGetEtagDoesNotMatch() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri = "/etag32";
		new ApiListenerBuilder(uri, Methods.GET)
			.withResponseContent("{\"tralalalallala\":true}")
			.build();
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-none-match", "my-etag-value7");

		// Act
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		// Assert
		assertEquals(200, result.getStatus());
		assertTrue(result.containsHeader("Allow"));
		assertEquals("{\"tralalalallala\":true}", result.getContentAsString());
		assertNull(result.getErrorMessage());
	}

	@Test
	public void eTagPostEtagIfMatches() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/etag45";
		new ApiListenerBuilder(uri, Methods.POST).build();
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-match", "my-etag-value");
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		assertEquals(200, result.getStatus());
		assertEquals("{\"tralalalallala\":true}", result.getContentAsString());
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void eTagPostEtagDoesNotMatch() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/etag46";
		new ApiListenerBuilder(uri, Methods.POST).build();
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-match", "my-etag-value2");
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, etag-matching should have aborted before handling request");
		assertEquals(412, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void cookieAuthentication401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		new ApiListenerBuilder(uri, Methods.POST, AuthMethods.COOKIE).build();

		Map<String, String> headers = new HashMap<String, String>();
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void cookieNotFoundInCache401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		new ApiListenerBuilder(uri, Methods.POST, AuthMethods.COOKIE).build();

		Map<String, String> headers = new HashMap<String, String>();
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);
		Cookie cookies = new Cookie("authenticationToken", "myToken");
		request.setCookies(cookies);
		Response result = service(request);

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void cookieAuthentication200() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		new ApiListenerBuilder(uri, Methods.POST, AuthMethods.COOKIE).build();
		String authToken = "random-token_thing";

		ApiPrincipal principal = new ApiPrincipal();
		assertTrue(principal.isLoggedIn(), "principal is not logged in? ttl expired?");
		ApiCacheManager.getInstance().put(authToken, principal);

		Map<String, String> headers = new HashMap<String, String>();
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);
		Cookie cookies = new Cookie("authenticationToken", authToken);
		request.setCookies(cookies);
		Response result = service(request);

		String sessionAuthToken = (String) session.get("authorizationToken");
		assertNotNull("session should contain auth token", sessionAuthToken);
		assertEquals(authToken, sessionAuthToken, "auth tokens should match");

		assertEquals(200, result.getStatus());
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
		assertTrue(result.containsCookie("authenticationToken"), "response contains auth cookie");
	}

	@Test
	public void headerAuthentication401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		new ApiListenerBuilder(uri, Methods.POST, AuthMethods.HEADER).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "blalablaaaa");
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void headerAuthentication200() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/header";
		new ApiListenerBuilder(uri, Methods.POST, AuthMethods.HEADER).build();
		String authToken = "random-token_thing";

		ApiPrincipal principal = new ApiPrincipal();
		assertTrue(principal.isLoggedIn(), "principal is not logged in? ttl expired?");
		ApiCacheManager.getInstance().put(authToken, principal);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", authToken);
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		String sessionAuthToken = (String) session.get("authorizationToken");
		assertNotNull("session should contain auth token", sessionAuthToken);
		assertEquals(authToken, sessionAuthToken, "auth tokens should match");

		assertEquals(200, result.getStatus());
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void authRoleAuthentication401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/authRole2";
		new ApiListenerBuilder(uri, Methods.POST, AuthMethods.AUTHROLE).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "am9objpkb2U=");
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);
		request.setAuthType("BASIC_AUTH");

		request.addUserRole("non-existing-role");

		Response result = service(request);

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void authRoleAuthentication200() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/authRole2";
		new ApiListenerBuilder(uri, Methods.POST, AuthMethods.AUTHROLE).build();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "am9objpkb2U=");
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);
		request.setAuthType("BASIC_AUTH");

		request.addUserRole("IbisObserver");

		Response result = service(request);

		assertEquals(200, result.getStatus());
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testRequestWithMessageId() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri = "/messageIdTest1";
		new ApiListenerBuilder(uri, Methods.POST)
			.setMessageIdHeader("X-Message-ID")
			.setCorrelationIdHeader("X-Correlation-ID")
			.build();

		Map<String, String> headers = new HashMap<>();
		headers.put("X-Message-ID", "msg1");
		HttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);

		// Act
		Response result = service(request);

		// Assert
		assertEquals(200, result.getStatus());
		assertTrue(session.containsKey(PipeLineSession.messageIdKey));
		assertEquals("msg1", session.get(PipeLineSession.messageIdKey));
		assertTrue(session.containsKey(PipeLineSession.correlationIdKey));
		assertEquals("msg1", session.get(PipeLineSession.correlationIdKey));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testRequestWithMessageIdAndCorrelationId() throws ServletException, IOException, ListenerException, ConfigurationException {
		// Arrange
		String uri = "/messageIdTest2";
		new ApiListenerBuilder(uri, Methods.POST)
			.setMessageIdHeader("X-Message-ID")
			.setCorrelationIdHeader("X-Correlation-ID")
			.build();

		Map<String, String> headers = new HashMap<>();
		headers.put("X-Message-ID", "msg1");
		headers.put("X-Correlation-ID", "msg2");
		HttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);

		// Act
		Response result = service(request);

		// Assert
		assertEquals(200, result.getStatus());
		assertTrue(session.containsKey(PipeLineSession.messageIdKey));
		assertEquals("msg1", session.get(PipeLineSession.messageIdKey));
		assertTrue(session.containsKey(PipeLineSession.correlationIdKey));
		assertEquals("msg2", session.get(PipeLineSession.correlationIdKey));
		assertNull(result.getErrorMessage());
	}

	@ParameterizedTest
	@EnumSource(Methods.class)
	public void testRequestWithAccept(Methods method) throws Exception {
		//you may not set the OPTIONS method on an ApiListener, the Servlet should handle this without calling the adapter
		assumeFalse(method.equals(Methods.OPTIONS));

		// Arrange
		String uri = "/messageWithJson2XmlValidator";
		new ApiListenerBuilder(uri, method, null, MediaTypes.XML).build();

		Map<String, String> headers = new HashMap<>();
		headers.put("accept", "application/xml");
		HttpServletRequest request = createRequest(uri, method, null, headers);

		// Act
		Response result = service(request);

		// Assert
		assertEquals(200, result.getStatus());
		Message input = requestMessage;
		assertEquals("application/xml", input.getContext().get("Header.accept"));
		assertNull(result.getErrorMessage());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@CsvSource(delimiter='-', value = {"application/xhtml+xml, application/xml;q=0.9", "*/*;q=0.8"})
	public void testParseAcceptHeaderAndValidateProducesXML(String acceptHeaderValues) throws Exception {
		setupParseAcceptHeaderAndValidateProduces(acceptHeaderValues, MediaTypes.XML);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@CsvSource(delimiter='-', value = {"application/json, application/*+xml;q=0.9", "*/*;q=0.8", "text/xml, application/json;q=0.8, */*;q=0.4"})
	public void testParseAcceptHeaderAndValidateProducesJSON(String acceptHeaderValues) throws Exception {
		setupParseAcceptHeaderAndValidateProduces(acceptHeaderValues, MediaTypes.JSON);
	}

	public void setupParseAcceptHeaderAndValidateProduces(String acceptHeaderValue, MediaTypes produces) throws Exception {
		// Arrange
		String uri = "/messageWithAcceptHeaderAndProduces"+produces;
		new ApiListenerBuilder(uri, Methods.GET, null, produces).build();

		Map<String, String> headers = new HashMap<>();
		if(acceptHeaderValue != null) {
			headers.put("accept", acceptHeaderValue);
		}
		HttpServletRequest request = createRequest(uri, Methods.GET, null, headers);

		// Act
		Response result = service(request);

		// Assert
		assertEquals(200, result.getStatus());
		assertNull(result.getErrorMessage());
	}

	@ParameterizedTest
	@CsvSource(delimiter='-', value = {"application/xhtml+xml, application/xml;q=0.9", "text/xml;q=0.8"})
	public void testEndpointDoesNotAcceptHeader(String acceptHeaderValue) throws Exception {
		// Arrange
		String uri = "/messageThatDoesNotAcceptAcceptHeader";
		new ApiListenerBuilder(uri, Methods.GET, null, MediaTypes.JSON)
			.build();

		Map<String, String> headers = new HashMap<>();
		headers.put("accept", acceptHeaderValue);
		HttpServletRequest request = createRequest(uri, Methods.GET, null, headers);

		// Act
		Response result = service(request);

		// Assert
		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(406, result.getStatus());
		assertEquals("endpoint cannot provide the supplied MimeType", result.getErrorMessage());
	}

	@Test
	public void testRequestExceptionHandling() throws Exception {
		// Arrange
		String uri = "/testThrowsError";
		new ApiListenerBuilder(uri, Methods.GET, null, null)
			.withShouldThrow(true)
			.build();

		HttpServletRequest request = createRequest(uri, Methods.GET, null, null);

		// Act
		Response result = service(request);

		// Assert
		assertEquals(500, result.getStatus());
		assertNotNull(result.getErrorMessage());
	}

	@Test
	public void testJwtTokenParsingWithRequiredIssuer() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setRequiredIssuer("JWTPipeTest")
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertEquals(200, result.getStatus());
		assertEquals(PAYLOAD, session.get("ClaimsSet"));
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testJwtTokenParsingWithoutRequiredIssuer() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertEquals(200, result.getStatus());
		assertEquals(PAYLOAD, session.get("ClaimsSet"));
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testJwtTokenParsingWithIllegalRequiredIssuer() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.setRequiredIssuer("test")
			.build();

		Response result = service(prepareJWTRequest(null));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(401, result.getStatus());
		assertEquals("illegal issuer [JWTPipeTest], must be [test]", result.getErrorMessage());
	}

	@Test
	public void testJwtTokenParsingWithInterceptedPayload() throws Exception {
		String token="eyJhbGciOiJSUzI1NiJ9."
				+ "eyJpc3MiOiJKV1RQaXBlVGVHzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCJ9."
				+ "U1VsMoITf5kUEHtzfgJTyRWEDZ2gjtTuQI3DVRrJcpden2pjCsAWwl4VOr6McmQkcndZj0GPvN4w3NkJR712ltlsIXw1zMm67vuFY0_id7TP2zIJh3jMkKrTuSPE-SBXZyVnIq22Q54R1VMnOTjO6spbrbYowIzyyeAC7U1RzyB3aKxTgeYJS6auLBaiR3-SWoXs_hBnbIIgYT7AC2e76ICpMlFPQS_e2bcqe1B-yz69se8ZlJgwWK-YhqHMoOCA9oQy3t_cObQI0KSzg7cYDkkQ17cWF3SoyTSTs6Cek_Y97Z17lJX2RVBayPc2uI_oWWuaIUbukxAOIUkgpgtf6g";
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
		.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
		.setAuthenticationMethod(AuthenticationMethods.JWT)
		.build();

		Response result = service(prepareJWTRequest(token));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(401, result.getStatus());
		assertEquals("Payload of JWS object is not a valid JSON object",result.getErrorMessage());

	}

	@Test
	public void testJwtTokenParsingInvalidSignature() throws Exception {
		String token="eyJhbGciOiJSUzI1NiJ9."
				+ "eyJpc3MiOiJKV1RQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCJ9."
				+ "U1VsMoITf5kUEHtzfgJTyRWKEDZ2gjtTuQI3DVRrJcpden2pjCsAWwl4VOr6McmQkcndZj0GPvN4w3NkJR712ltlsIXw1zMm67vuFY0_id7TP2zIJh3jMkKrTuSPE-SBXZyVnIq22Q54R1VMnOTjO6spbrbYowIzyyeAC7U1RzyB3aKxTgeYJS6auLBaiR3-SWoXs_hBnbIIgYT7AC2e76ICpMlFPQS_e2bcqe1B-yz69se8ZlJgwWK-YhqHMoOCA9oQy3t_cObQI0KSzg7cYDkkQ17cWF3SoyTSTs6Cek_Y97Z17lJX2RVBayPc2uI_oWWuaIUbukxAOIUkgpgtf6g";
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(token));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(401, result.getStatus());
		assertEquals("Signed JWT rejected: Invalid signature",result.getErrorMessage());

	}

	@Test
	public void testJwtTokenParsingWithRequiredClaims() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setRequiredIssuer("JWTPipeTest")
			.setRequiredClaims("sub, aud")
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertEquals(200, result.getStatus());
		assertEquals(PAYLOAD, session.get("ClaimsSet"));
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testJwtTokenParsingWithExactMatchClaims() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setRequiredIssuer("JWTPipeTest")
			.setExactMatchClaims("sub=UnitTest, aud=Framework")
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertEquals(200, result.getStatus());
		assertEquals(PAYLOAD, session.get("ClaimsSet"));
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testJwtTokenMissingRequiredClaims() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setRequiredIssuer("JWTPipeTest")
			.setRequiredClaims("sub, aud, kid")
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(403, result.getStatus());
		assertEquals("JWT missing required claims: [kid]", result.getErrorMessage());
	}

	@Test
	public void testJwtTokenUnexpectedValueForExactMatchClaim() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setRequiredIssuer("JWTPipeTest")
			.setExactMatchClaims("sub=UnitTest, aud=test")
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertFalse(handlerInvoked, "Request Handler should not have been invoked, pre-conditions should have failed and stopped request-processing");
		assertEquals(403, result.getStatus());
		assertEquals("JWT aud claim has value [Framework], must be [test]", result.getErrorMessage());
	}

	@Test
	public void testJwtTokenWithRoleClaim() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setRequiredIssuer("JWTPipeTest")
			.setRoleClaim("sub")
			.setAuthenticationRoles("UnitTest")
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertEquals(200, result.getStatus());
		assertEquals(PAYLOAD, session.get("ClaimsSet"));
	}

	@Test
	public void testJwtTokenWithRoleClaimAndEmptyAuthRoles() throws Exception {
		new ApiListenerBuilder(JWT_VALIDATION_URI, Methods.GET)
			.setJwksURL(TestFileUtils.getTestFileURL("/JWT/jwks.json").toString())
			.setRequiredIssuer("JWTPipeTest")
			.setRoleClaim("sub")
			.setAuthenticationMethod(AuthenticationMethods.JWT)
			.build();

		Response result = service(prepareJWTRequest(null));

		assertEquals(200, result.getStatus());
		assertEquals(PAYLOAD, session.get("ClaimsSet"));
	}

	private String createJWT() throws Exception {
		JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).build();

		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
		builder.issuer("JWTPipeTest");
		builder.subject("UnitTest");
		builder.audience("Framework");
		builder.jwtID("1234");

		SignedJWT signedJWT = new SignedJWT(jwsHeader, builder.build());

		KeyStore keystore = PkiUtil.createKeyStore(TestFileUtils.getTestFileURL("/JWT/jwt_keystore.p12"), "geheim", KeystoreType.PKCS12, "Keys for signing");
		KeyManager[] keymanagers = PkiUtil.createKeyManagers(keystore, "geheim", null);
		X509KeyManager keyManager = (X509KeyManager)keymanagers[0];
		PrivateKey privateKey = keyManager.getPrivateKey("1");
		PublicKey publicKey = keystore.getCertificate("1").getPublicKey();

		JWK jwk = new RSAKey.Builder((RSAPublicKey) publicKey)
				.privateKey(privateKey)
				.keyUse(KeyUse.SIGNATURE)
				.keyOperations(Collections.singleton(KeyOperation.SIGN))
				.algorithm(JWSAlgorithm.RS256)
				.keyStore(keystore)
				.build();

		DefaultJWSSignerFactory factory = new DefaultJWSSignerFactory();
		JWSSigner jwsSigner = factory.createJWSSigner(jwk, JWSAlgorithm.RS256);
		signedJWT.sign(jwsSigner);

		return signedJWT.serialize();
	}

	public MockHttpServletRequest prepareJWTRequest(String token) throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "Bearer "+ (token != null ? token : createJWT()) );

		return createRequest(JWT_VALIDATION_URI, Methods.GET, null, headers);
	}

	private static class StricterMockHttpServletResponse extends MockHttpServletResponse {
		private static Logger log = LogUtil.getLogger(StricterMockHttpServletResponse.class);
		boolean responseAccessed = false;
		boolean responseCommitted = false;

		private void assertResponseNotAccessed() {
			if (responseAccessed) {
				throw new IllegalStateException("Cannot perform this operation after response writer has been accessed");
			}
			assertResponseNotCommitted();
		}

		private void assertResponseNotCommitted() {
			if (responseCommitted) {
				throw new IllegalStateException("Operation cannot be performed after response has been committed");
			}
		}

		@Override
		@Nonnull
		public PrintWriter getWriter() throws UnsupportedEncodingException {
			responseAccessed = true;
			return super.getWriter();
		}

		@Override
		public ServletOutputStream getOutputStream() {
			responseAccessed = true;
			return super.getOutputStream();
		}

		@Override
		public void flushBuffer() {
			log.warn("Flushing buffer. Committing response.");
			responseCommitted = true;
			super.flushBuffer();
		}

		@Override
		public void setCommitted(boolean committed) {
			log.warn("Set Committed = {}", committed);
			responseCommitted = committed;
			super.setCommitted(committed);
		}

		@Override
		public void sendError(int status, String errorMessage) throws IOException {
			log.warn("Send Error. Committing response.");
			assertResponseNotCommitted();
			responseCommitted = true;
			super.sendError(status, errorMessage);
		}

		@Override
		public void sendError(int status) throws IOException {
			log.warn("Send Error. Committing response.");
			assertResponseNotCommitted();
			responseCommitted = true;
			super.sendError(status);
		}

		@Override
		public void reset() {
			assertResponseNotCommitted();
			responseAccessed = false;
			super.reset();
		}

		@Override
		public void resetBuffer() {
			assertResponseNotCommitted();
			responseAccessed = false;
			super.resetBuffer();
		}

		@Override
		public void setLocale(Locale locale) {
			assertResponseNotAccessed();
			super.setLocale(locale);
		}

		@Override
		public void setContentType(String contentType) {
			assertResponseNotAccessed();
			super.setContentType(contentType);
		}

		@Override
		public void setContentLength(int contentLength) {
			assertResponseNotAccessed();
			super.setContentLength(contentLength);
		}

		@Override
		public void addDateHeader(String name, long value) {
			assertResponseNotAccessed();
			super.addDateHeader(name, value);
		}

		@Override
		public void setDateHeader(String name, long value) {
			assertResponseNotAccessed();
			super.setDateHeader(name, value);
		}

		@Override
		public void addIntHeader(String name, int value) {
			assertResponseNotAccessed();
			super.addIntHeader(name, value);
		}

		@Override
		public void setIntHeader(String name, int value) {
			assertResponseNotAccessed();
			super.setIntHeader(name, value);
		}

		@Override
		public void addHeader(String name, String value) {
			assertResponseNotAccessed();
			super.addHeader(name, value);
			if ("Content-Type".equalsIgnoreCase(name)) {
				setContentType(value);
			}
		}

		@Override
		public void setHeader(String name, String value) {
			assertResponseNotAccessed();
			super.setHeader(name, value);
			if ("Content-Type".equalsIgnoreCase(name)) {
				setContentType(value);
			}
		}

		@Override
		public void addCookie(Cookie cookie) {
			assertResponseNotAccessed();
			super.addCookie(cookie);
		}

		@Override
		public void setStatus(int status) {
			assertResponseNotAccessed();
			super.setStatus(status);
		}
	}

	private class ApiListenerBuilder {

		private final ApiListener listener;
		private MessageHandler handler;

		public ApiListenerBuilder(String uri, Methods method) throws ListenerException, ConfigurationException {
			this(uri, method, null, null);
		}

		public ApiListenerBuilder(String uri, Methods method, AuthMethods authMethod) throws ListenerException, ConfigurationException {
			this(uri, method, null, null, authMethod,null);
		}

		public ApiListenerBuilder(String uri, Methods method, MediaTypes consumes, MediaTypes produces) throws ListenerException, ConfigurationException {
			this(uri, method, consumes, produces, null, null);
		}

		public ApiListenerBuilder(String uri, Methods method, MediaTypes consumes, MediaTypes produces, AuthMethods authMethod, String multipartBodyName) throws ListenerException, ConfigurationException {
			listener = spy(ApiListener.class);
			listener.setUriPattern(uri);
			listener.setMethod(EnumUtils.parse(HttpMethod.class, method.name()));

			handler = new MessageHandler();
			listener.setHandler(handler);

			if(consumes != null)
				listener.setConsumes(consumes);
			if(produces != null)
				listener.setProduces(produces);
			if(multipartBodyName != null) {
				listener.setMultipartBodyName(multipartBodyName);
			}
			if(authMethod != null) {
				listener.setAuthenticationMethod(EnumUtils.parse(AuthenticationMethods.class, authMethod.name()));
				listener.setAuthenticationRoles("IbisObserver,TestRole");
			}

		}

		public ApiListenerBuilder setAuthenticationRoles(String roles) {
			listener.setAuthenticationRoles(roles);
			return this;
		}

		public ApiListenerBuilder setRequiredClaims(String requiredClaims) {
			listener.setRequiredClaims(requiredClaims);
			return this;
		}

		public ApiListenerBuilder setRoleClaim(String string) {
			listener.setRoleClaim(string);
			return this;
		}

		public ApiListenerBuilder setRequiredIssuer(String requiredIssuer) {
			listener.setRequiredIssuer(requiredIssuer);
			return this;
		}

		public ApiListenerBuilder setJwksURL(String jwksURL) {
			listener.setJwksURL(jwksURL);
			return this;
		}

		public ApiListenerBuilder setAuthenticationMethod(AuthenticationMethods authMethod) {
			listener.setAuthenticationMethod(authMethod);
			return this;
		}

		public ApiListenerBuilder setExactMatchClaims(String exactMatchClaims) {
			listener.setExactMatchClaims(exactMatchClaims);
			return this;
		}

		public ApiListenerBuilder setMessageIdHeader(String headerName) {
			listener.setMessageIdHeader(headerName);
			return this;
		}

		public ApiListenerBuilder setCorrelationIdHeader(String headerName) {
			listener.setCorrelationIdHeader(headerName);
			return this;
		}

		public ApiListenerBuilder withExitCode(int exitCode) {
			handler.setExitCode(exitCode);
			return this;
		}

		public ApiListenerBuilder withShouldThrow(boolean shouldThrow) {
			handler.setShouldThrow(shouldThrow);
			return this;
		}

		public ApiListenerBuilder withResponseContent(Object responseContent) {
			handler.setResponseContent(responseContent);
			return this;
		}

		public ApiListener build() throws ConfigurationException, ListenerException {
			listener.configure();
			listener.open();

			listeners.add(listener);
			log.info("created ApiListener "+ listener);
			return listener;
		}
	}


	private class MessageHandler implements IMessageHandler<Message> {
		private @Setter int exitCode = 0;
		private @Setter boolean shouldThrow = false;
		private @Setter Object responseContent = null;

		@Override
		public void processRawMessage(IListener<Message> origin, Message message, PipeLineSession session, boolean duplicatesAlreadyChecked) throws ListenerException {
			fail("method should not be called");
		}

		@Override
		public void processRawMessage(IListener<Message> origin, Message message, PipeLineSession session, long waitingTime, boolean duplicatesAlreadyChecked) throws ListenerException {
			fail("method should not be called");
		}

		@Override
		public void processRawMessage(IListener<Message> origin, Message message) throws ListenerException {
			fail("method should not be called");
		}


		@Override
		public Message processRequest(IListener<Message> origin, Message rawMessage, Message message, PipeLineSession context) throws ListenerException {
			handlerInvoked = true;
			if(session != null) {
				context.putAll(session);
			}
			session = context;
			requestMessage = message;
			if (shouldThrow) {
				throw new ListenerException("Hard Throw");
			}
			if (exitCode > 0) {
				context.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, exitCode);
			}
			if (responseContent != null) {
				return Message.asMessage(responseContent);
			}
			return message;
		}

		@Override
		public Message formatException(String extraInfo, String correlationId, Message message, Throwable t) {
			t.printStackTrace();

			return new Message(t.getMessage());
		}
	}

	private static class Response {
		private final MockHttpServletResponse response;

		Response(MockHttpServletResponse response) {
			this.response = response;
		}

		public boolean containsCookie(String name) {
			return null != response.getCookie(name);
		}

		public String getHeader(String name) {
			return response.getHeader(name);
		}

		public boolean containsHeader(String name) {
			return response.containsHeader(name);
		}

		public String getContentAsString() throws UnsupportedEncodingException {
			return response.getContentAsString();
		}

		public int getStatus() {
			return response.getStatus();
		}

		public String getErrorMessage() {
			return response.getErrorMessage();
		}

		public String getContentType() {
			return response.getContentType();
		}

		@Override
		public String toString() {
			String content = "unknown";
			try {
				content = getContentAsString();
			}
			catch (Exception e) {
				// Ignore
			}

			return "status["+getStatus()+"] contentType["+getContentType()+"] inError["+(getErrorMessage()!=null)+"] content["+content+"]";
		}
	}
}
