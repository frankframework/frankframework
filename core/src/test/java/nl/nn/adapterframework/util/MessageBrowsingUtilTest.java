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
package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;

public class MessageBrowsingUtilTest {

	@Test
	public void testGetMessageTextWithMessage() throws Exception {
		String contents = "fakeMessage";
		Message message = new Message(contents);

		assertEquals(contents, MessageBrowsingUtil.getMessageText(message, new TestListener()));
	}

	@Test
	public void testGetMessageTextWithString() throws Exception {
		String contents = "fakeMessage";

		assertEquals(contents, MessageBrowsingUtil.getMessageText(contents, new TestListener()));
	}

	@Test
	public void testGetMessageTextWithMessageWrapper() throws Exception {
		String contents = "fakeMessage";
		Message message = new Message(contents);
		MessageWrapper messageWrapper = new MessageWrapper(message,"fakeId");

		assertEquals(contents, MessageBrowsingUtil.getMessageText(messageWrapper, new TestListener()));
	}

	@Test
	public void testGetMessageTextWithListenerMessage() throws Exception {
		String contents = "fakeMessage";
		TestListenerMessage listenerMessage = new TestListenerMessage();
		listenerMessage.setText(contents);

		assertEquals(contents, MessageBrowsingUtil.getMessageText(listenerMessage, new TestListener()));
	}

	@Test //This is a strange test the causes a ClassCastException when converting a byte[] to TestListenerMessage.
	public void testByteArrayMessageThatIsNotCompatibleWithTheListenerType() throws Exception {
		String contents = "fakeMessage";
		byte[] bytes = contents.getBytes();

		assertEquals(contents, MessageBrowsingUtil.getMessageText(bytes, new TestListener()));
	}


	private class TestListenerMessage {
		private @Getter @Setter String text;
	}

	private class TestListener implements IListener<TestListenerMessage> {

		@Override
		public void setName(String name) {
			// TODO Auto-generated method stub
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ApplicationContext getApplicationContext() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ClassLoader getConfigurationClassLoader() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			// TODO Auto-generated method stub
		}

		@Override
		public void configure() throws ConfigurationException {
			// TODO Auto-generated method stub
		}

		@Override
		public void open() throws ListenerException {
			// TODO Auto-generated method stub
		}

		@Override
		public void close() throws ListenerException {
			// TODO Auto-generated method stub
		}

		@Override
		public Message extractMessage(RawMessageWrapper<TestListenerMessage> rawMessage, Map<String, Object> context) throws ListenerException {
			return new Message(rawMessage.getRawMessage().text);
		}

		@Override
		public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<TestListenerMessage> rawMessage,
										  Map<String, Object> context) throws ListenerException {
			// TODO Auto-generated method stub
		}

	}
}
