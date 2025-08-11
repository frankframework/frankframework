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
package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IProvidesMessageBrowsers;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.ProcessState;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.SpringUtils;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestInlineStorage extends BusTestBase {

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		registerAdapter(getConfiguration());
	}

	protected Adapter registerAdapter(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration);
		adapter.setName("TestAdapter");

		DummyListenerWithMessageBrowsers listener = new DummyListenerWithMessageBrowsers();
		listener.setName("ListenerName");
		Receiver<String> receiver = SpringUtils.createBean(adapter);
		receiver.setName("ReceiverName");
		receiver.setListener(listener);
		adapter.addReceiver(receiver);
		PipeLine pipeline = SpringUtils.createBean(adapter);
		EchoPipe pipe = SpringUtils.createBean(configuration);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		adapter.configure();
		getConfiguration().addAdapter(adapter);

		return adapter;
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
		public RawMessageWrapper<String> changeProcessState(RawMessageWrapper<String> message, ProcessState toState, String reason) {
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
