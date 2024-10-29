package org.frankframework.http.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.http.rest.ApiServiceDispatcher;
import org.frankframework.parameters.Parameter;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestFileUtils;

class OpenApiTest extends OpenApiTestBase {

	public static final String DEFAULT_SUMMARY = "description4simple-get";
	public static final String DEFAULT_ADAPTER_NAME = "myAdapterName";
	private final ApiServiceDispatcher dispatcher = ApiServiceDispatcher.getInstance();

	@Test
	void simpleEndpointGetTest() throws Exception {
		String uri = "/users";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, HttpMethod.GET, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/simple.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void simpleEndpointPostTest() throws Exception {
		String uri = "/simpleEndpointPostTest";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, HttpMethod.POST, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/simplePost.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testChoiceWithComplexType() throws Exception {
		String uri = "/transaction";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, HttpMethod.POST, null)
				.setInputValidator("transaction.xsd", "transaction", null, null)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/transaction.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testChoiceWithSimpleType() throws Exception {
		String uri = "/options";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, HttpMethod.POST, null)
				.setInputValidator("Options.xsd", "Options", null, null)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/Options.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testMultipleChoices() throws Exception {
		String uri = "/multipleChoices";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, HttpMethod.POST, null)
				.setInputValidator("multipleChoices.xsd", "EmbeddedChoice", null, null)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/multipleChoices.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void simpleEndpointPostWithEmptyExitTest() throws Exception {
		String uri = "/simpleEndpointPostWithEmptyExitTest";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, HttpMethod.POST, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500, null, true)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/simplePostWithEmptyExit.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void simpleEndpointWithOperationIdTest() throws Exception {
		String uri = "/simpleEndpointWithOperationIdTest";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "get envelope adapter description")
				.setListener(uri, HttpMethod.GET, "operationId")
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse", null)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/envelope.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void simpleEndpointQueryParamTest() throws Exception {
		String uri = "/simpleEndpointQueryParamTest";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");
		Parameter param = ParameterBuilder.create("parameter", "parameter").withSessionKey("parameter");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "get envelope adapter description")
				.setListener(uri, HttpMethod.GET, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse", param)
				.addExit(200)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "get envelope adapter description")
				.setListener(uri, HttpMethod.POST, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse", param)
				.addExit(200)
				.build(true);

		assertEquals(2, dispatcher.findExactMatchingConfigForUri(uri).getMethods().size(), "more then 2 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/envelopeQueryParam.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void pathParamQueryParamTest() throws Exception {
		String uri = "/pathParamQueryParamTest";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");
		Parameter param = ParameterBuilder.create("parameter", "parameter").withSessionKey("parameter");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "get envelope adapter description")
				.setListener(uri + "/{pattern}", HttpMethod.GET, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse", param)
				.addExit(200)
				.addExit(500)
				.addExit(403)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "get envelope adapter description")
				.setListener(uri + "/{pattern}/sub/{path}", HttpMethod.POST, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse", param)
				.addExit(200)
				.addExit(500)
				.addExit(403)
				.build(true);

		assertEquals(2, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 2 registered pattern found!");
		String expected = TestFileUtils.getTestFile("/OpenApi/envelopePathParamQueryParam.json");

		String result = callOpenApi(uri + "/{pattern}");
		MatchUtils.assertJsonEquals(expected, result);

		String encodedResult = callOpenApi(uri + "/%7Bpattern%7D");
		MatchUtils.assertJsonEquals("Test should pass in escaped form!", expected, encodedResult);
	}

	@Test
	void exitElementNamesTest() throws Exception {
		String uri = "/envelope";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");
		Parameter param = ParameterBuilder.create("parameter", "parameter").withSessionKey("parameter");

		String responseRoot = "EnvelopeResponse,EnvelopeError403,EnvelopeError500";
		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "each exit have specific element name")
				.setListener(uri, HttpMethod.GET, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", responseRoot, param)
				.addExit(200, "EnvelopeResponse", false)
				.addExit(500, "EnvelopeError500", false)
				.addExit(403, "EnvelopeError403", false)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "200 code will retrieve the ref from first of response root")
				.setListener(uri + "/test", HttpMethod.GET, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", responseRoot, param)
				.addExit(200, null, false)
				.addExit(500, "EnvelopeError500", false)
				.addExit(403, "EnvelopeError403", false)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "no element name responseRoot will be used as source for refs")
				.setListener(uri + "/elementNames", HttpMethod.GET, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", responseRoot, param)
				.addExit(200)
				.addExit(500)
				.addExit(403)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "403 empty exit")
				.setListener(uri + "/{pattern}/sub/{path}", HttpMethod.POST, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", responseRoot, param)
				.addExit(200)
				.addExit(500)
				.addExit(403, null, true)
				.build(true);

		assertEquals(4, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 4 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/envelopeExits.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void petStore() throws Exception {
		String uriBase = "/pets";
		//Make sure all adapters have been registered on the dispatcher
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uriBase).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder("listPets", "List all pets")
				.setListener(uriBase, HttpMethod.GET, null)
				.setInputValidator("petstore.xsd", null, "Pets", null)
				.addExit(200)
				.addExit(500, "Error", false)
				.build(true);

		new AdapterBuilder("createPets", "Create a pet")
				.setListener(uriBase, HttpMethod.POST, null)
				.setInputValidator("petstore.xsd", "Pet", "Pet", null)
				.addExit(201, null, true)
				.addExit(500, "Error", false)
				.build(true);

		new AdapterBuilder("showPetById", "Info for a specific pet")
				.setListener(uriBase + "/{petId}", HttpMethod.GET, null)
				.setInputValidator("petstore.xsd", null, "Pet", null)
				.addExit(200)
				.addExit(500, "Error", false)
				.build(true);

		//getPets.start(getPets, postPet, getPet); //Async start

		// Thread.sleep(1200); //Adding a small timeout to fix async starting issues

		assertNotNull(dispatcher.findExactMatchingConfigForUri(uriBase), "unable to find DispatchConfig for uri [pets]");
		assertEquals(2, dispatcher.findExactMatchingConfigForUri(uriBase).getMethods().size(), "not all listener uri [pets] are registered on the dispatcher");
		assertNotNull(dispatcher.findExactMatchingConfigForUri(uriBase + "/a"), "unable to find DispatchConfig for uri [pets/a]");
		assertEquals(1, dispatcher.findExactMatchingConfigForUri(uriBase + "/a").getMethods().size(), "listener uri [pets/a] not registered on dispatcher");

		String result = callOpenApi(uriBase);

		String expected = TestFileUtils.getTestFile("/OpenApi/petstore.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void rootSchemaTest() throws Exception {
		String uri = "/";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri + "users", HttpMethod.GET, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri + "test", HttpMethod.GET, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(2, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 2 registered pattern found!");
		String result = callOpenApi(uri + "users");

		String expected = TestFileUtils.getTestFile("/OpenApi/simpleRoot.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testSchemaGenerationWhenWildcardMatchersArePresent() throws Exception {
		String uri = "/";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri + "**", HttpMethod.GET, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri + "users", HttpMethod.GET, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri + "test", HttpMethod.GET, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(2, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 2 registered pattern found!");
		String result = callOpenApi(uri + "users");

		String expected = TestFileUtils.getTestFile("/OpenApi/simpleRoot.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void twoEndpointsOneWithoutValidatorTest() throws Exception {
		String uri = "/path";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri + "/validator", HttpMethod.GET, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri + "/noValidator", HttpMethod.GET, null)
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(2, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 2 registered pattern found!");
		String result = callOpenApi(uri + "/validator");

		String expected = TestFileUtils.getTestFile("/OpenApi/noValidatorForOneEndpoint.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void parametersFromHeader() throws Exception {
		String uri = "/headerparams";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, List.of(HttpMethod.GET), null, null)
				.setHeaderParams("envelopeId, envelopeType")
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		MockHttpServletRequest request = createRequest("GET", uri + "/openapi.json");
		request.addHeader("envelopeId", "dummy");
		request.addHeader("envelopeType", "dummyType");

		String result = service(request);

		String expected = TestFileUtils.getTestFile("/OpenApi/twoHeaderParams.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

//	@Test
//	public void parametersFromCookie() throws Exception {
//		String uri="/cookieparams";
//		assertEquals("there are still registered patterns! Threading issue?", 0, dispatcher.findAllMatchingConfigsForUri(uri).size());
//
//		new AdapterBuilder("myAdapterName", "description4simple-get")
//			.setListener(uri, "get", null, null)
//			.setCookieParams("envelopeId, envelopeType")
//			.setValidator("simple.xsd", null, "user", null)
//			.addExit(200)
//			.addExit(500)
//			.build(true);
//
//		assertEquals("more then 1 registered pattern found!", 1, dispatcher.findAllMatchingConfigsForUri(uri).size());
//		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri + "/openapi.json");
//		request.setServerName("dummy");
//		request.setPathInfo(uri + "/openapi.json");
//		Cookie[] cookies = {new Cookie("envelopeId", "dummy"), new Cookie("envelopeType", "dummyType")};
//		request.setCookies(cookies);
//
//		String result = service(request);
//
//		String expected = TestFileUtils.getTestFile("/OpenApi/cookieParams.json");
//		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
//	}

//	@Test
//	public void parametersFromCookieAndHeader() throws Exception {
//		String uri="/cookieplusheaderparams";
//		assertEquals("there are still registered patterns! Threading issue?", 0, dispatcher.findAllMatchingConfigsForUri(uri).size());
//
//		new AdapterBuilder("myAdapterName", "description4simple-get")
//			.setListener(uri, "get", null, null)
//			.setHeaderParams("headerparam")
//			.setCookieParams("envelopeId, envelopeType")
//			.setValidator("simple.xsd", null, "user", null)
//			.addExit(200)
//			.addExit(500)
//			.build(true);
//
//		assertEquals("more then 1 registered pattern found!", 1, dispatcher.findAllMatchingConfigsForUri(uri).size());
//		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri + "/openapi.json");
//		request.setServerName("dummy");
//		request.setPathInfo(uri + "/openapi.json");
//		Cookie[] cookies = {new Cookie("envelopeId", "dummy"), new Cookie("envelopeType", "dummyType")};
//		request.setCookies(cookies);
//		request.addHeader("headerparam", "dummy");
//
//		String result = service(request);
//
//		String expected = TestFileUtils.getTestFile("/OpenApi/parametersFromCookieAndHeader.json");
//		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
//	}

	@Test
	void validatorParamFromHeaderNotQuery() throws Exception {
		String uri = "/validatorParamFromHeaderNotQuery";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");
		Parameter param = ParameterBuilder.create("parameter", "parameter").withSessionKey("parameter");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "get envelope adapter description")
				.setListener(uri, List.of(HttpMethod.GET), null, null)
				.setHeaderParams("parameter")
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse", param)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findExactMatchingConfigForUri(uri).getMethods().size(), "more then 2 registered pattern found!");
		MockHttpServletRequest request = createRequest("GET", uri + "/openapi.json");
		request.addHeader("parameter", "dummy");

		String result = service(request);

		String expected = TestFileUtils.getTestFile("/OpenApi/validatorParamFromHeaderNotQuery.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void messageIdHeaderTest() throws Exception {
		String uri = "/messageIdHeaderTest";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, "get envelope adapter description")
				.setListener(uri, List.of(HttpMethod.GET), null, null)
				.setMessageIdHeader("x-message-id")
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse", null)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findExactMatchingConfigForUri(uri).getMethods().size(), "more then 2 registered pattern found!");
		MockHttpServletRequest request = createRequest("GET", uri + "/openapi.json");
		request.addHeader("x-message-id", "dummy");

		String result = service(request);

		String expected = TestFileUtils.getTestFile("/OpenApi/messageIdHeaderTest.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testHeaderParamIsnotAddedAsQueryParam() throws Exception {
		String uri = "/headerparams";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");
		Parameter p = ParameterBuilder.create("envelopeId", "envelopeType").withSessionKey("headers");
		p.setXpathExpression("/headers/header[@name='envelopeId']");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, List.of(HttpMethod.GET), null, null)
				.setHeaderParams("envelopeId, envelopeType")
				.setInputValidator("simple.xsd", null, "user", p)
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		MockHttpServletRequest request = createRequest("GET", uri + "/openapi.json");
		request.addHeader("envelopeId", "dummy");
		request.addHeader("envelopeType", "dummyType");

		String result = service(request);

		String expected = TestFileUtils.getTestFile("/OpenApi/twoHeaderParams.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testOutputValidator() throws Exception {
		String uri = "/outputValidator";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, List.of(HttpMethod.GET), null, null)
				.setOutputValidator("simple.xsd", "user")
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");

		String result = callOpenApi(uri);
		String expected = TestFileUtils.getTestFile("/OpenApi/outputValidator.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testInputOutputValidator() throws Exception {
		String uri = "/outputValidator";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, List.of(HttpMethod.POST), null, null)
				.setInputValidator("envelope.xsd", "EnvelopeRequest", "EnvelopeResponse, EnvelopeError500", null)
				.setOutputValidator("simple.xsd", "user")
				.addExit(200)
				.addExit(500, "EnvelopeError500", false)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");

		String result = callOpenApi(uri);
		String expected = TestFileUtils.getTestFile("/OpenApi/inputOutputValidators.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	void testWithoutValidator() throws Exception {
		String uri = "/noValidator";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, List.of(HttpMethod.GET), null, null)
				.addExit(200)
				.addExit(500)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");

		String result = callOpenApi(uri);
		String expected = TestFileUtils.getTestFile("/OpenApi/noValidator.json");
		MatchUtils.assertJsonEquals(expected, result);
	}

	@Test
	@DisplayName("Asserts that an endpoint with multiple accepted http methods renders correctly ")
	void testMultipleMethodsEndpoint() throws Exception {
		String uri = "/multiple";
		assertEquals(0, dispatcher.findAllMatchingConfigsForUri(uri).size(), "there are still registered patterns! Threading issue?");

		new AdapterBuilder(DEFAULT_ADAPTER_NAME, DEFAULT_SUMMARY)
				.setListener(uri, List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH), null, null)
				.setInputValidator("simple.xsd", null, "user", null)
				.addExit(200)
				.build(true);

		assertEquals(1, dispatcher.findAllMatchingConfigsForUri(uri).size(), "more then 1 registered pattern found!");
		String result = callOpenApi(uri);

		String expected = TestFileUtils.getTestFile("/OpenApi/multipleMethods.json");
		MatchUtils.assertJsonEquals(expected, result);
	}
}
