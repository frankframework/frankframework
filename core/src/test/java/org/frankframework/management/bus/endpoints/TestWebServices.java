package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.http.RestListener;
import org.frankframework.http.WebServiceListener;
import org.frankframework.http.rest.ApiListener;
import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.http.rest.ApiServiceDispatcher;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.XmlValidator;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StreamUtil;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestWebServices extends BusTestBase {
	private static final String API_LISTENER_ENDPOINT = "/api-uri-pattern";
	private Adapter adapterWithRestListener;
	private Adapter adapterWithWebServiceListener;
	private ApiListener apiListener;

	@BeforeAll
	public static void beforeClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		adapterWithRestListener = registerAdapterWithRestListener(getConfiguration());
		adapterWithWebServiceListener = registerAdapterWithWebServiceListener(getConfiguration());
		apiListener = registerDummyApiListener(getConfiguration());
	}

	private ApiListener registerDummyApiListener(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("ApiTestAdapter");
		ApiListener listener = SpringUtils.createBean(adapter, ApiListener.class);
		listener.setMethod(HttpMethod.POST);
		listener.setUriPattern(API_LISTENER_ENDPOINT);
		Receiver receiver = SpringUtils.createBean(adapter, Receiver.class);
		receiver.setName("ReceiverName2");
		listener.setReceiver(receiver);
		receiver.setAdapter(adapter);
		adapter.addReceiver(receiver);
		PipeLine pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		EchoPipe pipe = SpringUtils.createBean(adapter, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);
		ApiServiceDispatcher.getInstance().registerServiceClient(listener);
		return listener;
	}

	protected Adapter registerAdapterWithRestListener(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("TestAdapter");

		RestListener listener = SpringUtils.createBean(adapter, RestListener.class);
		listener.setName("TestRestListener");
		listener.setMethod("GET");
		listener.setUriPattern("rest-uri-pattern");
		Receiver receiver = SpringUtils.createBean(adapter, Receiver.class);
		receiver.setName("ReceiverName1");
		receiver.setListener(listener);
		adapter.addReceiver(receiver);
		receiver.setAdapter(adapter);
		PipeLine pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		EchoPipe pipe = SpringUtils.createBean(adapter, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		getConfiguration().addAdapter(adapter);
		return adapter;
	}

	protected Adapter registerAdapterWithWebServiceListener(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("wsl-adapter");

		WebServiceListener listener = SpringUtils.createBean(adapter, WebServiceListener.class);
		listener.setName("TestWebServiceListener");
		listener.setServiceNamespaceURI("urn:test");
		Receiver receiver = SpringUtils.createBean(adapter, Receiver.class);
		receiver.setName("ReceiverName3");
		receiver.setListener(listener);
		adapter.addReceiver(receiver);
		receiver.setAdapter(adapter);
		PipeLine pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		XmlValidator validator = SpringUtils.createBean(adapter, XmlValidator.class);
		validator.setSchema("Validation/Basic/xsd/A_correct.xsd");
		pipeline.setInputValidator(validator);
		EchoPipe pipe = SpringUtils.createBean(adapter, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		getConfiguration().addAdapter(adapter);
		return adapter;
	}

	@AfterEach
	@Override
	public void tearDown() {
		if(apiListener != null) {
			ApiServiceDispatcher.getInstance().unregisterServiceClient(apiListener);
		}
		super.tearDown();
	}

	@Test
	public void getWebServices() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.GET);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		String expectedJson = TestFileUtils.getTestFile("/Management/WebServices/get-services.json");
		MatchUtils.assertJsonEquals("JSON Mismatch", expectedJson, result);
	}

	@Test
	public void openApiSpecNotFound() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "openapi");
		request.setHeader("uri", "dummy");
		MessageHandlingException e = assertThrows(MessageHandlingException.class, ()-> callSyncGateway(request));
		assertTrue(e.getCause() instanceof BusException);
		assertEquals("unable to find Dispatch configuration for uri", e.getCause().getMessage());
	}

	@Test
	public void getOpenApiSpec() throws Exception {
		assertEquals(1, ApiServiceDispatcher.getInstance().findAllMatchingConfigsForUri(API_LISTENER_ENDPOINT).size());

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "openapi");
		request.setHeader("uri", API_LISTENER_ENDPOINT);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		result = result.replaceFirst("auto-generated at .* for", "auto-generated at -timestamp- for");
		String expectedJson = TestFileUtils.getTestFile("/Management/WebServices/OpenApiSpec.json");
		MatchUtils.assertJsonEquals("JSON Mismatch", expectedJson, result);
	}

	@Test
	public void getFullOpenApiSpec() throws Exception {
		assertEquals(1, ApiServiceDispatcher.getInstance().findAllMatchingConfigsForUri(API_LISTENER_ENDPOINT).size());

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "openapi");
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		result = result.replaceFirst("auto-generated at .* for", "auto-generated at -timestamp- for");
		String expectedJson = TestFileUtils.getTestFile("/Management/WebServices/OpenApiSpec.json");
		MatchUtils.assertJsonEquals("JSON Mismatch", expectedJson, result);
	}

	@Test
	public void wsdlNotFound() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithRestListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		MessageHandlingException e = assertThrows(MessageHandlingException.class, ()-> callSyncGateway(request));
		assertTrue(e.getCause() instanceof BusException);
		assertEquals("unable to create WSDL generator: (IllegalStateException) No inputvalidator provided", e.getCause().getMessage());
	}

	private String convertPayloadAndApplyIgnores(Message<?> message) throws IOException {
		String string = StreamUtil.streamToString((InputStream) message.getPayload());
		return convertPayloadAndApplyIgnores(string);
	}

	private String convertPayloadAndApplyIgnores(String string) {// 2023-01-19 15:54:27
		return string.replaceFirst("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "-timestamp-");
	}

	@Test
	public void getWsdl() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());

		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));

		String expectedWsdl = TestFileUtils.getTestFile("/Management/WebServices/wsdl-without-includes.wsdl");
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(response));
	}

	@Test
	public void getWsdlWithIncludes() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("useIncludes", true);

		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));

		String expectedWsdl = TestFileUtils.getTestFile("/Management/WebServices/wsdl-with-includes.wsdl");
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(response));
	}

	@Test
	public void getWsdlWithoutIndent() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("indent", false);

		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));

		String expectedWsdl = TestFileUtils.getTestFile("/Management/WebServices/wsdl-without-includes.wsdl");
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(response));
	}

	@Test
	public void getWsdlAsZip() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("zip", true);

		Message<?> response = callSyncGateway(request);
		assertEquals("application/octet-stream", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));
		InputStream payload = (InputStream) response.getPayload();

		String wsdl = null;
		try(ZipInputStream archive = new ZipInputStream(payload)) {
			byte[] buffer = new byte[2048];
			for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
				String name = entry.getName();
				if("wsl-adapter.wsdl".equals(name)) {
					int len;
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					while((len = archive.read(buffer)) > 0) {
						bos.write(buffer, 0, len);
					}
					wsdl = new String(bos.toByteArray());
				}
				archive.closeEntry();
			}
		}
		assertNotNull(wsdl, "wsdl file not found in zip archive");

		String expectedWsdl = TestFileUtils.getTestFile("/Management/WebServices/wsdl-with-includes.wsdl");
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(wsdl));
	}
}
