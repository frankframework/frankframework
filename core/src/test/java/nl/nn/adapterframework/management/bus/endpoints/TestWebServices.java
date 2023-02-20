package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.SpringUtils;

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
		ApiListener listener = new ApiListener();
		listener.setMethod(HttpMethod.POST);
		listener.setUriPattern(API_LISTENER_ENDPOINT);
		Receiver receiver = new Receiver();
		receiver.setName("ReceiverName2");
		listener.setReceiver(receiver);
		receiver.setAdapter(adapter);
		adapter.registerReceiver(receiver);
		PipeLine pipeline = new PipeLine();
		EchoPipe pipe = SpringUtils.createBean(configuration, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);
		ApiServiceDispatcher.getInstance().registerServiceClient(listener);
		return listener;
	}

	protected Adapter registerAdapterWithRestListener(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("TestAdapter");

		RestListener listener = new RestListener();
		listener.setName("TestRestListener");
		listener.setMethod("GET");
		listener.setUriPattern("rest-uri-pattern");
		Receiver receiver = new Receiver<>();
		receiver.setName("ReceiverName1");
		receiver.setListener(listener);
		adapter.registerReceiver(receiver);
		receiver.setAdapter(adapter);
		PipeLine pipeline = new PipeLine();
		EchoPipe pipe = SpringUtils.createBean(configuration, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		getConfiguration().registerAdapter(adapter);
		return adapter;
	}

	protected Adapter registerAdapterWithWebServiceListener(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("wsl-adapter");

		WebServiceListener listener = new WebServiceListener();
		listener.setName("TestWebServiceListener");
		listener.setServiceNamespaceURI("urn:test");
		Receiver receiver = new Receiver<>();
		receiver.setName("ReceiverName3");
		receiver.setListener(listener);
		adapter.registerReceiver(receiver);
		receiver.setAdapter(adapter);
		PipeLine pipeline = new PipeLine();
		XmlValidator validator = new XmlValidator();
		validator.setSchema("Validation/Basic/xsd/A_correct.xsd");
		pipeline.setInputValidator(validator);
		EchoPipe pipe = SpringUtils.createBean(configuration, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		getConfiguration().registerAdapter(adapter);
		return adapter;
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		if(adapterWithRestListener != null) {
			getConfiguration().getAdapterManager().unRegisterAdapter(adapterWithRestListener);
		}
		if(adapterWithWebServiceListener != null) {
			getConfiguration().getAdapterManager().unRegisterAdapter(adapterWithWebServiceListener);
		}
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
	public void openApiSpecNotFound() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "openapi");
		request.setHeader("uri", "dummy");
		MessageHandlingException e = assertThrows(MessageHandlingException.class, ()-> callSyncGateway(request));
		assertTrue(e.getCause() instanceof BusException);
		assertEquals("unable to find Dispatch configuration for uri", e.getCause().getMessage());
	}

	@Test
	public void getOpenApiSpec() throws Exception {
		assertEquals(1, ApiServiceDispatcher.getInstance().findMatchingConfigsForUri(API_LISTENER_ENDPOINT).size());

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
		assertEquals(1, ApiServiceDispatcher.getInstance().findMatchingConfigsForUri(API_LISTENER_ENDPOINT).size());

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "openapi");
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		result = result.replaceFirst("auto-generated at .* for", "auto-generated at -timestamp- for");
		String expectedJson = TestFileUtils.getTestFile("/Management/WebServices/OpenApiSpec.json");
		MatchUtils.assertJsonEquals("JSON Mismatch", expectedJson, result);
	}

	@Test
	public void wsdlNotFound() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithRestListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		MessageHandlingException e = assertThrows(MessageHandlingException.class, ()-> callSyncGateway(request));
		assertTrue(e.getCause() instanceof BusException);
		assertEquals("unable to create WSDL generator: (IllegalStateException) No inputvalidator provided", e.getCause().getMessage());
	}

	private String convertPayloadAndApplyIgnores(byte[] payload) {// 2023-01-19 15:54:27
		String result = new String(payload).replaceFirst("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "-timestamp-");
		return result;
	}

	@Test
	public void getWsdl() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());

		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", response.getHeaders().get("meta:type"));
		byte[] payload = (byte[]) response.getPayload();

		String expectedWsdl = TestFileUtils.getTestFile("/Management/WebServices/wsdl-without-includes.wsdl");
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(payload));
	}

	@Test
	public void getWsdlWithIncludes() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("useIncludes", true);

		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", response.getHeaders().get("meta:type"));
		byte[] payload = (byte[]) response.getPayload();

		String expectedWsdl = TestFileUtils.getTestFile("/Management/WebServices/wsdl-with-includes.wsdl");
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(payload));
	}

	@Test
	public void getWsdlWithoutIndent() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("indent", false);

		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", response.getHeaders().get("meta:type"));
		byte[] payload = (byte[]) response.getPayload();

		String expectedWsdl = TestFileUtils.getTestFile("/Management/WebServices/wsdl-without-includes.wsdl");
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(payload));
	}

	@Test
	public void getWsdlAsZip() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.setHeader("type", "wsdl");
		request.setHeader("adapter", adapterWithWebServiceListener.getName());
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("zip", true);

		Message<?> response = callSyncGateway(request);
		assertEquals("application/octet-stream", response.getHeaders().get("meta:type"));
		byte[] payload = (byte[]) response.getPayload();

		String wsdl = null;
		try(ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(payload))) {
			byte[] buffer = new byte[2048];
			for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
				String name = entry.getName();
				if(name.equals("wsl-adapter.wsdl")) {
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
		MatchUtils.assertXmlEquals(expectedWsdl, convertPayloadAndApplyIgnores(wsdl.getBytes()));
	}
}
