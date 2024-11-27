package org.frankframework.scheduler.job;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.IbisLocalSender;
import org.frankframework.stream.Message;

class SendMessageJobTest {

	private final SendMessageJob jobDef = new SendMessageJob();
	private final IbisLocalSender localSenderMock = mock(IbisLocalSender.class);

	@BeforeEach
	void setup(){
		jobDef.setMessage("messageMock");
	}

	@Test
	void testHappyExecuteCall() throws JobExecutionException, TimeoutException, SenderException, IOException {
		Message messageMock = mock(Message.class);
		when(localSenderMock.sendMessageOrThrow(any(), any())).thenReturn(messageMock);
		jobDef.setLocalSender(localSenderMock);
		jobDef.execute();

		verify(localSenderMock).sendMessageOrThrow(any(), any());
		verify(localSenderMock).stop();
		verify(messageMock).close();
	}

	@Test
	void testFailingSenderStillGetsClosed() throws TimeoutException, SenderException {
		when(localSenderMock.sendMessageOrThrow(any(), any())).thenThrow(SenderException.class);
		jobDef.setLocalSender(localSenderMock);
		assertThrows(JobExecutionException.class, jobDef::execute);

		verify(localSenderMock).stop();
		verify(localSenderMock).sendMessageOrThrow(any(), any());
	}

	@Test
	void testConfigureShouldFailWithoutJavaListener() {
		assertThrows(ConfigurationException.class, jobDef::configure);
	}

}
