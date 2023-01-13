package nl.nn.adapterframework.management.bus.endpoints;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.SpringUtils;

public class TestWebServices extends BusTestBase {
	private Adapter adapter;
	private ApiListener apiListener;

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		adapter = registerAdapterWithRestListener(getConfiguration());
		registerDummyApiListener();
	}

	private void registerDummyApiListener() throws Exception {
		apiListener = new ApiListener();
		apiListener.setMethod(HttpMethod.POST);
		apiListener.setUriPattern("api-uri-pattern");
		Receiver receiver = new Receiver();
		receiver.setName("ReceiverName2");
		apiListener.setReceiver(receiver);
		ApiServiceDispatcher.getInstance().registerServiceClient(apiListener);
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
		PipeLine pipeline = new PipeLine();
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
		if(adapter != null) {
			getConfiguration().getAdapterManager().unRegisterAdapter(adapter);
		}
		if(apiListener != null) {
			ApiServiceDispatcher.getInstance().unregisterServiceClient(apiListener);
		}
		super.tearDown();
	}

	@Test
	public void getWebServices() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.WEBSERVICES);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		String expectedJson = TestFileUtils.getTestFile("/Management/WebServices.json");
		MatchUtils.assertJsonEquals("JSON Mismatch", expectedJson, result);
	}
}
