package org.frankframework.jms;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;
import jakarta.jms.Message;

import org.springframework.context.ApplicationContext;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPortConnectedListener;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.unmanaged.PollGuard;
import org.frankframework.unmanaged.SpringJmsConnector;

@Log4j2
public class SlowListenerWithPollGuard implements IPushingListener<Message>, IPortConnectedListener<Message> {

	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;
	private @Setter int startupDelay = 10000;
	private @Setter int shutdownDelay = 0;
	private @Getter boolean closed = false;

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
		if (startupDelay > 0) {
			try {
				Thread.sleep(startupDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

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
		if (shutdownDelay > 0) {
			try {
				Thread.sleep(shutdownDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.debug("Closed after delay");
		}
		closed = true;
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


	@Override
	public void setHandler(IMessageHandler<Message> handler) {
		// No-op
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		// No-op
	}

	@Override
	public RawMessageWrapper<Message> wrapRawMessage(Message rawMessage, PipeLineSession session) {
		return null;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<Message> rawMessage, PipeLineSession pipeLineSession) {
		// No-op
	}

	@Override
	public org.frankframework.stream.Message extractMessage(@Nonnull RawMessageWrapper<Message> rawMessage, @Nonnull Map<String, Object> context) {
		return org.frankframework.stream.Message.asMessage(rawMessage.getRawMessage());
	}
}
