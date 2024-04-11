package org.frankframework.extensions.idin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.extensions.idin.IdinSender.Action;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import net.bankid.merchant.library.CommunicatorException;
import net.bankid.merchant.library.Configuration;
import net.bankid.merchant.library.IMessenger;

/**
 * Initially I thought, hey lets add some unittests...
 * Let's just skip them for now shall we? :)
 *
 */
public class IdinSenderTest extends Mockito {

	private IdinSender sender = null;
	private PipeLineSession session = null;

	@BeforeEach
	public void initializeIdinSender() throws Exception {
		sender = spy(new IdinSender());
		sender.setConfigurationXML("configs/default-config.xml");

		IMessenger messenger = new DummyMessenger();
		when(sender.getConfiguration()).thenAnswer(new Answer<Configuration>() {

			@Override
			public Configuration answer(InvocationOnMock invocation) throws Throwable {
				Configuration config = (Configuration) spy(invocation.callRealMethod());
				when(config.getMessenger()).thenReturn(messenger);
				return config;
			}

		});
		sender.configure();
		session = new PipeLineSession();
	}

	private class DummyMessenger implements IMessenger {

		@Override
		public String sendMessage(Configuration config, String message, URI url) throws CommunicatorException {
			try {
				URL response = ClassUtils.getResourceURL("/messages/StatusResponse-Sample-015.xml");
				assertNotNull(response);
				return StreamUtil.resourceToString(response);
			} catch(IOException e) {
				throw new CommunicatorException(e);
			}
		}
	}

	@Test
	public void testSimpleRequestResponse() throws SenderException, TimeoutException, IOException {
		String message = "<idin><transactionID>1111111111111111</transactionID></idin>";

		sender.setAction(Action.RESPONSE);
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();

		assertEquals("<result>\n" + "	<status>Success</status>\n" + "</result>", result);
	}

	@Test
	@Disabled //don't have dummy data yet
	public void getIssuersByCountry() throws SenderException, TimeoutException, IOException {
		String message = "<idin><issuersByCountry>true</issuersByCountry></idin>";

		sender.setAction(Action.DIRECTORY);
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		assertEquals("result", result);
	}
}
