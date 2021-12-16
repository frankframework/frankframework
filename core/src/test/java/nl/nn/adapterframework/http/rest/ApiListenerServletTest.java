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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class ApiListenerServletTest extends Mockito {
	private Logger log = LogUtil.getLogger(this);
	private List<ApiListener> listeners = Collections.synchronizedList(new ArrayList<ApiListener>());

	enum Methods {
		GET,POST,PUT,DELETE,OPTIONS
	}

	enum AuthMethods {
		COOKIE,HEADER,AUTHROLE
	}

	private ApiListenerServlet servlet;
	private Map<String, Object> session = null;

	@Before
	public void setUp() throws ServletException {
		servlet = spy(ApiListenerServlet.class);
		ServletConfig servletConfig = new MockServletConfig();
		when(servlet.getServletConfig()).thenReturn(servletConfig);
		servlet.init();

		session = null;
	}

	@After
	public void tearDown() {
		for(ApiListener listener : listeners) {
			listener.close();
		}
		listeners.clear();

		servlet.destroy();
		servlet = null;
	}

	@BeforeClass
	public static void beforeClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	@AfterClass
	public static void afterClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	private void addListener(String uri, Methods method) throws ListenerException, ConfigurationException {
		addListener(uri, method, null, null);
	}

	private void addListener(String uri, Methods method, AuthMethods authMethod) throws ListenerException, ConfigurationException {
		addListener(uri, method, null, null, authMethod);
	}

	private void addListener(String uri, Methods method, MediaTypes consumes, MediaTypes produces) throws ListenerException, ConfigurationException {
		addListener(uri, method, consumes, produces, null, null);
	}

	private void addListener(String uri, Methods method, MediaTypes consumes, MediaTypes produces, String multipartBodyName) throws ListenerException, ConfigurationException {
		addListener(uri, method, consumes, produces, null, multipartBodyName);
	}

	private void addListener(String uri, Methods method, MediaTypes consumes, MediaTypes produces, AuthMethods authMethod) throws ListenerException, ConfigurationException {
		addListener(uri, method, consumes, produces, authMethod, null);
	}

	private void addListener(String uri, Methods method, MediaTypes consumes, MediaTypes produces, AuthMethods authMethod, String multipartBodyName) throws ListenerException, ConfigurationException {
		ApiListener listener = spy(ApiListener.class);
		listener.setUriPattern(uri);
		listener.setMethod(method.name());

		IMessageHandler<Message> handler = new MessageHandler();
		listener.setHandler(handler);

		if(consumes != null)
			listener.setConsumes(consumes.name());
		if(produces != null)
			listener.setProduces(produces.name());

		if(multipartBodyName != null) {
			listener.setMultipartBodyName(multipartBodyName);
		}

		if(authMethod != null) {
			listener.setAuthenticationMethod(authMethod.name());
			listener.setAuthenticationRoles("IbisObserver,TestRole");
		}

		listener.configure();
		listener.open();

		listeners.add(listener);
		log.info("created ApiListener "+listener.toString());
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

		return createRequest(uriPattern, method, new String(requestContent.toByteArray()), headers);
	}

	private MockHttpServletRequest createRequest(String uriPattern, Methods method, String content, Map<String, String> headers) {
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
			if(content != null)
				request.setContent(content.getBytes());
			else
				request.setContent("".getBytes()); //Empty content
		}

		return request;
	}

	private Response service(HttpServletRequest request) throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();

		servlet.service(request, response);

		return new Response(response);
	}

	@Test
	public void noUri() throws ServletException, IOException, ListenerException, ConfigurationException {
		addListener("test", Methods.GET);

		Response result = service(createRequest(null, Methods.GET));
		assertEquals(400, result.getStatus());
	}

	@Test
	public void uriNotFound() throws ServletException, IOException, ListenerException, ConfigurationException {
		addListener("test", Methods.GET);

		Response result = service(createRequest("/not-test", Methods.GET));
		assertEquals(404, result.getStatus());
	}

	@Test
	public void methodNotAllowed() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/test";
		addListener(uri, Methods.GET);

		Response result = service(createRequest(uri, Methods.PUT));
		assertEquals(405, result.getStatus());
		assertNull(result.getErrorMessage());
	}

	@Test
	public void simpleGet() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/test1";
		addListener(uri, Methods.GET);

		Response result = service(createRequest(uri, Methods.GET));
		assertEquals(200, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void simpleGetWithSlashes() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/test2";
		addListener(uri, Methods.GET);

		Response result = service(createRequest(uri, Methods.GET));
		assertEquals(200, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void preFlightRequest() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/preflight/";
		addListener(uri, Methods.GET);

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
		addListener(uri, Methods.GET);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Access-Control-Request-Headers", "Message-Id,CustomHeader");
		Response result = service(createRequest(uri, Methods.OPTIONS, null, headers));
		assertEquals(200, result.getStatus());
		assertEquals("", result.getContentAsString()); //Pre-flight requests have no data
		assertNull(result.getErrorMessage());
		assertTrue(result.containsHeader("Access-Control-Allow-Origin"));
		assertTrue(result.containsHeader("Access-Control-Allow-Headers"));
		assertEquals(result.getHeader("Access-Control-Allow-Headers"), "Message-Id,CustomHeader");
		assertTrue(result.containsHeader("Access-Control-Expose-Headers"));
		assertTrue(result.containsHeader("Access-Control-Allow-Methods"));
	}

	@Test
	public void corsGetRequest() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/not-preflight/";
		addListener(uri, Methods.POST);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("origin", "https://ibissource.org");
		headers.put("Access-Control-Request-Headers", "Message-Id,CustomHeader");
		Response result = service(createRequest(uri, Methods.POST, "data", headers));
		assertEquals(200, result.getStatus());
		assertEquals("data", result.getContentAsString());
		assertNull(result.getErrorMessage());
		assertTrue(result.containsHeader("Access-Control-Allow-Origin"));
		assertTrue(result.containsHeader("Access-Control-Allow-Headers"));
		assertEquals(result.getHeader("Access-Control-Allow-Headers"), "Message-Id,CustomHeader");
		assertTrue(result.containsHeader("Access-Control-Expose-Headers"));
		assertTrue(result.containsHeader("Access-Control-Allow-Methods"));
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
	}

	@Test
	public void apiListenerThatProducesXML() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/ApiListenerThatProducesXML/";
		addListener(uri, Methods.PUT, null, MediaTypes.XML);

		Response result = service(createRequest(uri, Methods.PUT, "<xml>data</xml>"));
		assertEquals(200, result.getStatus());
		assertEquals("<xml>data</xml>", result.getContentAsString());
		assertEquals("OPTIONS, PUT", result.getHeader("Allow"));
		assertTrue("Content-Type header does not contain [application/xml]", result.getContentType().contains("application/xml"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void apiListenerThatProducesJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/ApiListenerThatProducesJSON/";
		addListener(uri, Methods.POST, null, MediaTypes.JSON);

		Response result = service(createRequest(uri, Methods.POST, "{}"));
		assertEquals(200, result.getStatus());
		assertEquals("{}", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue("Content-Type header does not contain [application/json]", result.getContentType().contains("application/json"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void clientAcceptHeaderDoesNotLikeJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/ApiListenerAllow/";
		addListener(uri, Methods.POST, null, MediaTypes.JSON);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/xml");
		Response result = service(createRequest(uri, Methods.POST, "{}", headers));
		assertEquals(406, result.getStatus());
	}

	@Test
	public void clientAcceptHeaderLovesJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/ApiListenerAllow/";
		addListener(uri, Methods.POST, null, MediaTypes.JSON);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		Response result = service(createRequest(uri, Methods.POST, "{}", headers));
		assertEquals(200, result.getStatus());
		assertEquals("{}", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue("Content-Type header does not contain [application/json]", result.getContentType().contains("application/json"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerDoesNotLikeContentTypeJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerDoesNotAcceptContentType";
		addListener(uri, Methods.POST, MediaTypes.XML, null);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri, Methods.POST, "{}", headers));
		assertEquals(415, result.getStatus());
		System.out.println(result);
	}

	@Test
	public void listenerLovesContentTypeJSON() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerLovesContentTypeJSON";
		addListener(uri, Methods.POST, MediaTypes.JSON, MediaTypes.JSON);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json; charset=UTF-8");
		Response result = service(createRequest(uri, Methods.POST, "{}", headers));
		assertEquals(200, result.getStatus());
		assertEquals("{}", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue("Content-Type header does not contain [application/json]", result.getContentType().contains("application/json"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerMultipartContentISO8859() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMultipartContent";
		addListener(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.JSON);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("gc0p4Jq0M2Yt08jU534c0p");
		builder.addTextBody("string1", "<hello>â¬ Ã¨</hello>");//Defaults to ISO_8859_1

		URL url1 = ClassUtils.getResourceURL("/Documents/doc001.pdf");
		builder.addBinaryBody("file1", url1.openStream(), ContentType.APPLICATION_OCTET_STREAM, "file1");

		URL url2 = ClassUtils.getResourceURL("/Documents/doc002.pdf");
		builder.addBinaryBody("file2", url2.openStream(), ContentType.APPLICATION_OCTET_STREAM, "file2");

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertTrue("Content-Type header does not contain [application/json]", result.getContentType().contains("application/json"));
		assertTrue("Content-Type header does not contain correct [charset]", result.getContentType().contains("charset=ISO-8859-1"));
		assertEquals("<hello>â¬ Ã¨</hello>", result.getContentAsString()); //Parsed as UTF-8
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void listenerMultipartContentUTF8() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMultipartContentCharset";
		addListener(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.JSON, "string2");

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addTextBody("string1", "<hello>€ è</hello>", ContentType.create("text/plain"));
		builder.addTextBody("string2", "<hello>€ è</hello>", ContentType.create("text/plain", "UTF-8"));//explicitly sent as UTF-8

		URL url1 = ClassUtils.getResourceURL("/test1.xml");
		assertNotNull(url1);
		builder.addBinaryBody("file1", url1.openStream(), ContentType.APPLICATION_XML, "file1");

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertEquals("<hello>€ è</hello>", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue("Content-Type header does not contain [application/json]", result.getContentType().contains("application/json"));
		assertTrue("Content-Type header does not contain correct [charset]", result.getContentType().contains("charset=UTF-8"));
		assertNull(result.getErrorMessage());

		String multipartXml = (String) session.get("multipartAttachments");
		assertNotNull(multipartXml);
		MatchUtils.assertXmlEquals("<parts>\n"
				+ "  <part name=\"string1\" type=\"text\" value=\"&lt;hello&gt;€ è&lt;/hello&gt;\" />\n"
				+ "  <part name=\"file1\" type=\"file\" filename=\"file1\" size=\"1041\" sessionKey=\"file1\" charSet=\"ISO-8859-1\" mimeType=\"application/xml\"/>\n"
				+ "</parts>", multipartXml);
	}

	@Test
	public void listenerMtomContent() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMtomContent";
		addListener(uri, Methods.POST, MediaTypes.MULTIPART_RELATED, MediaTypes.JSON);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMtomMultipart();
		builder.addTextBody("string1", "<hello>€ è</hello>", ContentType.TEXT_XML);//explicitly sent as UTF-8

		URL url1 = ClassUtils.getResourceURL("/test1.xml");
		assertNotNull(url1);
		builder.addBinaryBody("file1", url1.openStream(), ContentType.APPLICATION_XML, "file1");

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertEquals("<hello>€ è</hello>", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue("Content-Type header does not contain [application/json]", result.getContentType().contains("application/json"));
		assertTrue("Content-Type header does not contain correct [charset]", result.getContentType().contains("charset=UTF-8"));
		assertNull(result.getErrorMessage());

		String multipartXml = (String) session.get("multipartAttachments");
		assertNotNull(multipartXml);
		MatchUtils.assertXmlEquals("<parts>\n"
				+ "  <part name=\"string1\" type=\"text\" value=\"&lt;hello&gt;€ è&lt;/hello&gt;\" />\n"
				+ "  <part name=\"file1\" type=\"file\" filename=\"file1\" size=\"1041\" sessionKey=\"file1\" charSet=\"ISO-8859-1\" mimeType=\"application/xml\"/>\n"
				+ "</parts>", multipartXml);
	}

	@Test
	public void listenerMultipartContentNoContentType() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/listenerMultipartContentNoContentType";
		addListener(uri, Methods.POST, MediaTypes.MULTIPART, MediaTypes.JSON);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addTextBody("string1", "<request/>", ContentType.create("text/xml"));//explicitly sent as UTF-8

		Response result = service(createRequest(uri, Methods.POST, builder.build()));
		assertEquals(200, result.getStatus());
		assertEquals("<request/>", result.getContentAsString());
		assertEquals("OPTIONS, POST", result.getHeader("Allow"));
		assertTrue("Content-Type header does not contain [application/json]", result.getContentType().contains("application/json"));
		assertTrue("Content-Type header does not contain correct [charset]", result.getContentType().contains("charset=UTF-8"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void getRequestWithQueryParameters() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/queryParamTest";
		addListener(uri, Methods.GET);

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
		addListener(uri, Methods.GET);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri+"?transport=car&transport=bike&transport=moped&maxSpeed=60", Methods.GET, null, headers));

		assertEquals(200, result.getStatus());
		assertEquals("60", session.get("maxSpeed"));
		List<String> transportList = Arrays.asList(new String[] {"car","bike","moped"});
		assertEquals(transportList, session.get("transport"));
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void getRequestWithDynamicPath() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/dynamic/";
		addListener(uri+"{poef}", Methods.GET);

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
		addListener(uri+"*", Methods.GET);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		Response result = service(createRequest(uri+"tralala", Methods.GET, null, headers));
		System.out.println(session);

		assertEquals(200, result.getStatus());
		assertEquals("tralala", session.get("uriIdentifier_0"));
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void customExitCode() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/exitcode";
		addListener(uri, Methods.GET);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		session = new HashMap<String, Object>();
		session.put("exitcode", "234");
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		assertEquals(234, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void apiListenerShouldReturnEtag() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri="/etag1";
		addListener(uri, Methods.GET);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("content-type", "application/json");
		session = new HashMap<String, Object>();
		session.put("response-content", "{\"tralalalallala\":true}");
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		assertEquals(200, result.getStatus());
		assertEquals("OPTIONS, GET", result.getHeader("Allow"));
		assertNull(result.getErrorMessage());
		assertTrue(result.containsHeader("etag"));
	}

	@Test
	public void eTagGetEtagMatches() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/etag32";
		addListener(uri, Methods.GET);
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-none-match", "my-etag-value");
		session = new HashMap<String, Object>();
		session.put("response-content", "{\"tralalalallala\":true}");
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		assertEquals(304, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void eTagGetEtagDoesNotMatch() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/etag32";
		addListener(uri, Methods.GET);
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-none-match", "my-etag-value7");
		session = new HashMap<String, Object>();
		session.put("response-content", "{\"tralalalallala\":true}");
		Response result = service(createRequest(uri, Methods.GET, null, headers));

		assertEquals(200, result.getStatus());
		assertTrue(result.containsHeader("Allow"));
		assertEquals("{\"tralalalallala\":true}", result.getContentAsString());
		assertNull(result.getErrorMessage());
	}

	@Test
	public void eTagPostEtagIfMatches() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/etag45";
		addListener(uri, Methods.POST);
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-match", "my-etag-value");
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		System.out.println(result);
		assertEquals(200, result.getStatus());
		assertEquals("{\"tralalalallala\":true}", result.getContentAsString());
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void eTagPostEtagDoesNotMatch() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/etag46";
		addListener(uri, Methods.POST);
		String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
		ApiCacheManager.getInstance().put(etagCacheKey, "my-etag-value");

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("if-match", "my-etag-value2");
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		assertEquals(412, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void cookieAuthentication401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		addListener(uri, Methods.POST, AuthMethods.COOKIE);

		Map<String, String> headers = new HashMap<String, String>();
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void cookieNotFoundInCache401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		addListener(uri, Methods.POST, AuthMethods.COOKIE);

		Map<String, String> headers = new HashMap<String, String>();
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);
		Cookie cookies = new Cookie("authenticationToken", "myToken");
		request.setCookies(cookies);
		Response result = service(request);

		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void cookieAuthentication200() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		addListener(uri, Methods.POST, AuthMethods.COOKIE);
		String authToken = "random-token_thing";

		ApiPrincipal principal = new ApiPrincipal();
		assertTrue("principal is not logged in? ttl expired?", principal.isLoggedIn());
		ApiCacheManager.getInstance().put(authToken, principal);

		Map<String, String> headers = new HashMap<String, String>();
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);
		Cookie cookies = new Cookie("authenticationToken", authToken);
		request.setCookies(cookies);
		Response result = service(request);

		String sessionAuthToken = (String) session.get("authorizationToken");
		assertNotNull("session should contain auth token", sessionAuthToken);
		assertEquals("auth tokens should match", authToken, sessionAuthToken);

		assertEquals(200, result.getStatus());
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
		assertTrue("response contains auth cookie", result.containsCookie("authenticationToken"));
	}

	@Test
	public void headerAuthentication401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/cookie";
		addListener(uri, Methods.POST, AuthMethods.HEADER);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "blalablaaaa");
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void headerAuthentication200() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/header";
		addListener(uri, Methods.POST, AuthMethods.HEADER);
		String authToken = "random-token_thing";

		ApiPrincipal principal = new ApiPrincipal();
		assertTrue("principal is not logged in? ttl expired?", principal.isLoggedIn());
		ApiCacheManager.getInstance().put(authToken, principal);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", authToken);
		Response result = service(createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers));

		String sessionAuthToken = (String) session.get("authorizationToken");
		assertNotNull("session should contain auth token", sessionAuthToken);
		assertEquals("auth tokens should match", authToken, sessionAuthToken);

		assertEquals(200, result.getStatus());
		assertTrue(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void authRoleAuthentication401() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/authRole2";
		addListener(uri, Methods.POST, AuthMethods.AUTHROLE);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", "am9objpkb2U=");
		MockHttpServletRequest request = createRequest(uri, Methods.POST, "{\"tralalalallala\":true}", headers);
		request.setAuthType("BASIC_AUTH");

		request.addUserRole("non-existing-role");

		Response result = service(request);

		assertEquals(401, result.getStatus());
		assertFalse(result.containsHeader("Allow"));
		assertNull(result.getErrorMessage());
	}

	@Test
	public void authRoleAuthentication200() throws ServletException, IOException, ListenerException, ConfigurationException {
		String uri = "/authRole2";
		addListener(uri, Methods.POST, AuthMethods.AUTHROLE);

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









	private class MessageHandler implements IMessageHandler<Message> {

		@Override
		public void processRawMessage(IListener<Message> origin, Message message, Map<String, Object> context, boolean duplicatesAlreadyChecked) throws ListenerException {
			fail("method should not be called");
		}

		@Override
		public void processRawMessage(IListener<Message> origin, Message message, Map<String, Object> context, long waitingTime, boolean duplicatesAlreadyChecked) throws ListenerException {
			fail("method should not be called");
		}

		@Override
		public void processRawMessage(IListener<Message> origin, Message message) throws ListenerException {
			fail("method should not be called");
		}


		@Override
		public Message processRequest(IListener<Message> origin, String correlationId, Message rawMessage, Message message, Map<String, Object> context) throws ListenerException {
			if(session != null) {
				context.putAll(session);
			}
			session = context;
			if(session.containsKey("response-content")) {
				return Message.asMessage(session.get("response-content"));
			}
			return message;
		}

		@Override
		public Message formatException(String extrainfo, String correlationId, Message message, Throwable t) {
			t.printStackTrace();

			return new Message(t.getMessage());
		}
	}

	private class Response {
		private MockHttpServletResponse response;

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
			catch (Exception e) {}

			return "status["+getStatus()+"] contentType["+getContentType()+"] inError["+(getErrorMessage()!=null)+"] content["+content+"]";
		}
	}
}
