/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;

import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.util.SpringUtils;

public class TestInlineStorage extends BusTestBase {
	private Adapter adapter;

	private AdapterManager getAdapterManager() {
		return getConfiguration().getAdapterManager();
	}

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
		Receiver<String> receiver = new Receiver<>();
		receiver.setName("ReceiverName");
		receiver.setListener(listener);
		adapter.registerReceiver(receiver);
		PipeLine pipeline = new PipeLine();
		EchoPipe pipe = SpringUtils.createBean(configuration, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		adapter.configure();
		getAdapterManager().registerAdapter(adapter);

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
	public void getInlineStorage() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.INLINESTORAGE_SUMMARY);
		String className = TestScopeProvider.class.getCanonicalName();
		request.setHeader("className", className);
		String jsonResponse = (String) callSyncGateway(request).getPayload();

		String expectedJson = TestFileUtils.getTestFile("/Management/getProcessStorage.json");
		MatchUtils.assertJsonEquals(expectedJson, jsonResponse);
	}

	public class DummyListenerWithMessageBrowsers extends JavaListener implements IProvidesMessageBrowsers<String> {

		private Set<ProcessState> knownProcessStates = ProcessState.getMandatoryKnownStates();
		private Map<ProcessState,Set<ProcessState>> targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates);

		@Override
		public Set<ProcessState> knownProcessStates() {
			return knownProcessStates;
		}

		@Override
		public Map<ProcessState, Set<ProcessState>> targetProcessStates() {
			return targetProcessStates;
		}

		@Override
		public RawMessageWrapper<String> changeProcessState(RawMessageWrapper<String> message, ProcessState toState, String reason) throws ListenerException {
			return message;
		}

		@Override
		public IMessageBrowser<String> getMessageBrowser(ProcessState state) {
			IMessageBrowser<String> browser = mock(IMessageBrowser.class);
			try {
				doReturn(2).when(browser).getMessageCount();
			} catch (ListenerException e) {
				fail(e.getMessage());
			}
			return browser;
		}

	}
}
