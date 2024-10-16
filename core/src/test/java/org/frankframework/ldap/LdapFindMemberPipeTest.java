package org.frankframework.ldap;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;

import org.frankframework.stream.Message;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.naming.NamingException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Tag("mytag")
public class LdapFindMemberPipeTest extends PipeTestBase<LdapFindMemberPipe> {

	private static final String SUCCESS_FORWARD = "success";
	private static final String NOT_FOUND_FORWARD = "notFound";

	@Override
	public LdapFindMemberPipe createPipe() throws ConfigurationException {
		var pipe = spy(new LdapFindMemberPipe());
		pipe.registerForward(new PipeForward(SUCCESS_FORWARD, SUCCESS_FORWARD));
		pipe.registerForward(new PipeForward(NOT_FOUND_FORWARD, NOT_FOUND_FORWARD));

		return pipe;
	}

	@Test
	public void testFoundMember() throws NamingException, PipeRunException, IOException, ConfigurationException {
		doReturn(true).when(pipe).findMember(any(), anyInt(), any(), anyBoolean(), any(), anyBoolean());

		pipe.setDnSearchIn("-");
		pipe.setDnFind("-");
		pipe.setLdapProviderURL("url");

		pipe.configure();

		final String inputMessage = "input message";

		PipeRunResult result = pipe.doPipe(new Message(inputMessage), new PipeLineSession());

		assertEquals(SUCCESS_FORWARD, result.getPipeForward().getName());
		assertEquals(inputMessage, result.getResult().asString());
	}

	@Test
	public void testNotFoundMember() throws NamingException, PipeRunException, IOException, ConfigurationException {
		doReturn(false).when(pipe).findMember(any(), anyInt(), any(), anyBoolean(), any(), anyBoolean());

		pipe.setDnSearchIn("-");
		pipe.setDnFind("-");
		pipe.setLdapProviderURL("url");

		pipe.configure();

		final String inputMessage = "input message";

		PipeRunResult result = pipe.doPipe(new Message(inputMessage), new PipeLineSession());

		assertEquals(NOT_FOUND_FORWARD, result.getPipeForward().getName());
		assertEquals(inputMessage, result.getResult().asString());
	}

}
