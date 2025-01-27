package org.frankframework.receivers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.jms.Message;

import org.springframework.jms.listener.DefaultMessageListenerContainer;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPortConnectedListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.unmanaged.PollGuard;
import org.frankframework.unmanaged.SpringJmsConnector;

public class SlowListenerWithPollGuard extends SlowPushingListener implements IPortConnectedListener<Message> {

    private @Getter @Setter int pollGuardInterval = 0;
    private PollGuard pollGuard = null;
    private Timer pollGuardTimer = null;
    private @Getter @Setter int mockLastPollDelayMs = 10;
    private @Setter Receiver<Message> receiver;
	private SpringJmsConnector mockConnector;

	@Override
	public void configure() throws ConfigurationException {
		DefaultMessageListenerContainer mockContainer = mock(DefaultMessageListenerContainer.class);
		mockConnector = mock(SpringJmsConnector.class);
		when(mockConnector.getLastPollFinishedTime()).thenAnswer(invocationOnMock -> System.currentTimeMillis() - mockLastPollDelayMs);
		when(mockConnector.getThreadsProcessing()).thenReturn(new AtomicInteger());
		when(mockConnector.getReceiver()).thenReturn(receiver);
		when(mockConnector.getListener()).thenReturn(this);
		when(mockConnector.getJmsContainer()).thenReturn(mockContainer);
		when(mockConnector.getLogPrefix()).thenReturn("MockJmsConnector ");
	}

	@Override
    public void start() {
        super.start();

        if (pollGuardInterval > 0) {
            log.debug("Creating poll-guard timer with interval [" + pollGuardInterval + "ms] while starting SpringJmsConnector");
            pollGuard = new PollGuard();
            pollGuard.setSpringJmsConnector(mockConnector);
            pollGuardTimer = new Timer(true);
            pollGuardTimer.schedule(pollGuard, pollGuardInterval, pollGuardInterval);
        }
    }

    @Override
    public void stop() {
        if (pollGuardTimer != null) {
            log.debug("Cancelling previous poll-guard timer while stopping SpringJmsConnector");
            pollGuardTimer.cancel();
            pollGuardTimer = null;
        }
        super.stop();
    }

    @Override
    public IbisExceptionListener getExceptionListener() {
        return null;
    }

    @Override
    public IMessageHandler<Message> getHandler() {
        return null;
    }

    @Override
    public Receiver<Message> getReceiver() {
        return receiver;
    }
}
