package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.integration.support.MessageBuilder;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.pipes.SenderPipe;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.webcontrol.api.FrankApiBase;

public class TestBrowseMessageBrowsers extends BusTestBase {
	private Adapter adapter;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		adapter = registerAdapter(getConfiguration());
	}

	protected Adapter registerAdapter(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("TestAdapter");

		DummyListenerWithMessageBrowsers listener = new DummyListenerWithMessageBrowsers();
		listener.setName("ListenerName");
		Receiver<String> receiver = new Receiver<>();
		receiver.setName("ReceiverName");
		receiver.setListener(listener);
		adapter.registerReceiver(receiver);
		PipeLine pipeline = new PipeLine();
		SenderPipe pipe = SpringUtils.createBean(configuration, SenderPipe.class);
		pipe.setMessageLog(getTransactionalStorage());
		pipe.setSender(new EchoSender());
		pipe.setName("PipeName");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		adapter.configure();
		getConfiguration().getAdapterManager().registerAdapter(adapter);

		return adapter;
	}

	@After
	@Override
	public void tearDown() throws Exception {
		if(adapter != null) {
			getConfiguration().getAdapterManager().unRegisterAdapter(adapter);
		}
		super.tearDown();
	}

	@Test
	public void getMessageById() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.GET);
		request.setHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, adapter.getName());

		request.setHeader("messageId", "1234");
		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("no StorageSource provided", be.getMessage());
		}
	}

	@Test
	public void getMessageByIdFromReceiver() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.GET);
		request.setHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("receiver", "ReceiverName");
		request.setHeader("processState", "Error");

		request.setHeader("messageId", "1234");

		String jsonResponse = (String) callSyncGateway(request).getPayload();

		MatchUtils.assertJsonEquals("{\"id\": \"1234\",\"message\": \"<no message found/>\"}", jsonResponse);
	}

	@Test
	public void getMessageByIdFromPipe() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.GET);
		request.setHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("pipe", "PipeName");

		request.setHeader("messageId", "1234");

		String jsonResponse = (String) callSyncGateway(request).getPayload();

		MatchUtils.assertJsonEquals("{\"id\": \"1234\",\"message\": \"<no message found/>\"}", jsonResponse);
	}

	public class DummyListenerWithMessageBrowsers extends JavaListener implements IProvidesMessageBrowsers<String> {

		private Set<ProcessState> knownProcessStates;
		private Map<ProcessState,Set<ProcessState>> targetProcessStates;

		public DummyListenerWithMessageBrowsers() {
			knownProcessStates = ProcessState.getMandatoryKnownStates();
			knownProcessStates.add(ProcessState.ERROR);
			targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates);
		}

		@Override
		public Set<ProcessState> knownProcessStates() {
			return knownProcessStates;
		}

		@Override
		public Map<ProcessState, Set<ProcessState>> targetProcessStates() {
			return targetProcessStates;
		}

		@Override
		public String changeProcessState(String message, ProcessState toState, String reason) throws ListenerException {
			return message;
		}

		@Override
		public IMessageBrowser getMessageBrowser(ProcessState state) {
			return getTransactionalStorage();
		}
	}

	private ITransactionalStorage getTransactionalStorage() {
		ITransactionalStorage<String> browser = mock(ITransactionalStorage.class);
		try {
			doReturn("silly mock because the storage requires a name").when(browser).getName();
			doAnswer(DummyMessageBrowsingIteratorItem.newInstance()).when(browser).getContext(anyString());
			doReturn(1).when(browser).getMessageCount();
		} catch (ListenerException e) {
			fail(e.getMessage());
		}
		return browser;
	}

	public abstract static class DummyMessageBrowsingIteratorItem implements Answer<IMessageBrowsingIteratorItem>, IMessageBrowsingIteratorItem {
		private String messageId;
		public static DummyMessageBrowsingIteratorItem newInstance() {
			return mock(DummyMessageBrowsingIteratorItem.class, CALLS_REAL_METHODS);
		}

		@Override
		public IMessageBrowsingIteratorItem answer(InvocationOnMock invocation) throws Throwable {
			this.messageId = (String) invocation.getArguments()[0];
			return this;
		}

		@Override
		public String getId() throws ListenerException {
			return messageId;
		}
	}
}
