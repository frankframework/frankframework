package nl.nn.adapterframework.scheduler.job;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.IbisLocalSender;
import nl.nn.adapterframework.stream.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		when(localSenderMock.sendMessage(any(), any())).thenReturn(messageMock);
		jobDef.setLocalSender(localSenderMock);
		jobDef.execute();

		verify(localSenderMock).sendMessage(any(), any());
		verify(localSenderMock).close();
		verify(messageMock).close();
	}

	@Test
	void testFailingSenderStillGetsClosed() throws TimeoutException, SenderException {
		when(localSenderMock.sendMessage(any(), any())).thenThrow(SenderException.class);
		jobDef.setLocalSender(localSenderMock);
		assertThrows(JobExecutionException.class, jobDef::execute);

		verify(localSenderMock).close();
		verify(localSenderMock).sendMessage(any(), any());
	}

	@Test
	void testConfigureShouldFailWithoutJavaListener() {
		assertThrows(ConfigurationException.class, jobDef::configure);
	}

}
