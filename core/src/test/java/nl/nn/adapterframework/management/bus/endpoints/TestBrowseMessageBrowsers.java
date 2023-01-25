package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.pipes.SenderPipe;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.mock.TransactionManagerMock;
import nl.nn.adapterframework.util.SpringUtils;

public class TestBrowseMessageBrowsers extends BusTestBase {
	private static final String JSON_MESSAGE = "{\"dummy\":1}";
	private static final String XML_MESSAGE = "<dummy>2</dummy>";
	private Adapter adapter;

	@BeforeEach
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
		Receiver<String> receiver = spy(Receiver.class);
		receiver.setName("ReceiverName");
		receiver.setListener(listener);
		doAnswer(p -> { throw new ListenerException("testing message ->"+p.getArgument(0)); }).when(receiver).retryMessage(anyString()); //does not actually test the retry mechanism
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

	@AfterEach
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
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());

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
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("receiver", "ReceiverName");
		request.setHeader("processState", "Error");

		request.setHeader("messageId", "1234");

		String jsonResponse = (String) callSyncGateway(request).getPayload();

		MatchUtils.assertJsonEquals("{\"id\": \"1234\",\"originalId\": \"1234\",\"message\": \"<xml>1234</xml>\"}", jsonResponse);
	}

	@Test
	public void downloadJsonMessageByIdFromReceiver() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("receiver", "ReceiverName");
		request.setHeader("processState", "Error");
		request.setHeader("messageId", "1");

		Message<?> response = callSyncGateway(request);
		assertEquals(JSON_MESSAGE, response.getPayload());
		assertEquals("application/json", response.getHeaders().get(ResponseMessage.MIMETYPE_KEY));
	}

	@Test
	public void downloadXmlMessageByIdFromReceiver() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("receiver", "ReceiverName");
		request.setHeader("processState", "Error");
		request.setHeader("messageId", "2");

		Message<?> response = callSyncGateway(request);
		assertEquals(XML_MESSAGE, response.getPayload());
		assertEquals("application/xml", response.getHeaders().get(ResponseMessage.MIMETYPE_KEY));
	}

	@Test
	public void getMessageByIdFromPipe() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.GET);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("pipe", "PipeName");

		request.setHeader("messageId", "1234");

		String jsonResponse = (String) callSyncGateway(request).getPayload();

		MatchUtils.assertJsonEquals("{\"id\": \"1234\",\"originalId\": \"1234\",\"message\": \"<xml>1234</xml>\"}", jsonResponse);
	}

	@Test
	public void downloadJsonMessageByIdFromPipe() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("pipe", "PipeName");

		request.setHeader("messageId", "1");

		Message<?> response = callSyncGateway(request);
		assertEquals(JSON_MESSAGE, response.getPayload());
		assertEquals("application/json", response.getHeaders().get(ResponseMessage.MIMETYPE_KEY));
	}

	@Test
	public void downloadXmlMessageByIdFromPipe() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("pipe", "PipeName");

		request.setHeader("messageId", "2");

		Message<?> response = callSyncGateway(request);
		assertEquals(XML_MESSAGE, response.getPayload());
		assertEquals("application/xml", response.getHeaders().get(ResponseMessage.MIMETYPE_KEY));
	}

	@Test
	public void findAllMessageById() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.FIND);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("pipe", "PipeName");

		// filter should match any item
		String jsonResponse = (String) callSyncGateway(request).getPayload();
		MatchUtils.assertJsonEquals(TestFileUtils.getTestFile("/Management/MessageBrowserFindAll.json"), jsonResponse);
	}

	@Test
	public void findNoneMessageById() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.FIND);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("pipe", "PipeName");

		// filter should not match any item
		request.setHeader("messageId", "1234");
		String jsonResponse = (String) callSyncGateway(request).getPayload();
		MatchUtils.assertJsonEquals(TestFileUtils.getTestFile("/Management/MessageBrowserFindNone.json"), jsonResponse);
	}

	@Test
	public void findOneMessageById() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.FIND);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("receiver", "ReceiverName");
		request.setHeader("processState", "Error");

		// filter should match only 1 item
		request.setHeader("messageId", "2");
		String jsonResponse = (String) callSyncGateway(request).getPayload();
		MatchUtils.assertJsonEquals(TestFileUtils.getTestFile("/Management/MessageBrowserFindOne.json"), jsonResponse);
	}

	@Test
	public void resendMessageById() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.STATUS);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("receiver", "ReceiverName");

		// filter should match only 1 item
		request.setHeader("messageId", "2");
		try {
			callAsyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("unable to retry message with id [2]: testing message ->2", be.getMessage());
		}
	}

	@Test
	public void deleteMessageById() throws Exception {
		TransactionManagerMock.reset();
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MESSAGE_BROWSER, BusAction.DELETE);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter.getName());
		request.setHeader("receiver", "ReceiverName");

		// filter should match only 1 item
		request.setHeader("messageId", "2");
		try {
			callAsyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("unable to delete message with id [2]: testing message ->2", be.getMessage());

			assertTrue(TransactionManagerMock.peek().isCompleted());
			assertTrue(TransactionManagerMock.peek().hasBeenRolledBack());
		}
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
			doAnswer(DummyMessageBrowsingIteratorItem.newMock()).when(browser).getContext(anyString());
			DummyMessageBrowsingIterator iterator = new DummyMessageBrowsingIterator();
			doReturn(iterator.size()).when(browser).getMessageCount();
			doReturn(iterator).when(browser).getIterator(any(Date.class), any(Date.class), any(SortOrder.class));
			doReturn(iterator).when(browser).getIterator(isNull(), isNull(), any(SortOrder.class));
			doAnswer(this::messageMock).when(browser).browseMessage(anyString());
			doAnswer(p -> { throw new ListenerException("testing message ->"+p.getArgument(0)); }).when(browser).deleteMessage(anyString()); //does not actually test the delete mechanism
		} catch (ListenerException e) {
			fail(e.getMessage());
		}
		return browser;
	}


	public String messageMock(InvocationOnMock invocation) {
		String id = (String) invocation.getArguments()[0];
		switch (id) {
		case "1":
			return JSON_MESSAGE;
		case "2":
			return XML_MESSAGE;
		default:
			return "<xml>"+id+"</xml>";
		}
	}

	public static class DummyMessageBrowsingIterator implements IMessageBrowsingIterator {
		private Deque<IMessageBrowsingIteratorItem> items = new LinkedList<>();
		public DummyMessageBrowsingIterator() {
			items.add(DummyMessageBrowsingIteratorItem.newInstance("1"));
			items.add(DummyMessageBrowsingIteratorItem.newInstance("2"));
		}

		@Override
		public boolean hasNext() throws ListenerException {
			return !items.isEmpty();
		}

		@Override
		public IMessageBrowsingIteratorItem next() throws ListenerException {
			return items.poll();
		}

		@Override
		public void close() throws ListenerException {
			items.clear();
		}

		public int size() {
			return items.size();
		}
	}

	public abstract static class DummyMessageBrowsingIteratorItem implements Answer<IMessageBrowsingIteratorItem>, IMessageBrowsingIteratorItem {
		private String messageId;
		public static DummyMessageBrowsingIteratorItem newMock() {
			return mock(DummyMessageBrowsingIteratorItem.class, CALLS_REAL_METHODS);
		}

		public static DummyMessageBrowsingIteratorItem newInstance(String messageId) {
			DummyMessageBrowsingIteratorItem item = newMock();
			item.messageId = messageId;
			return item;
		}

		@Override
		public IMessageBrowsingIteratorItem answer(InvocationOnMock invocation) throws Throwable {
			messageId = (String) invocation.getArguments()[0];
			return this;
		}

		@Override
		public String getId() throws ListenerException {
			return messageId;
		}

		@Override
		public String getOriginalId() throws ListenerException {
			return messageId;
		}
	}
}
