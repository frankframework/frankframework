package nl.nn.adapterframework.receivers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Timer;
import javax.jms.Message;

import org.springframework.jms.listener.DefaultMessageListenerContainer;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.unmanaged.PollGuard;
import nl.nn.adapterframework.unmanaged.SpringJmsConnector;
import nl.nn.adapterframework.util.Counter;

public class SlowListenerWithPollGuard extends SlowPushingListener implements IPortConnectedListener<Message> {

    private @Getter @Setter int pollGuardInterval = 0;
    private PollGuard pollGuard = null;
    private Timer pollGuardTimer = null;
    private @Getter @Setter int mockLastPollDelayMs = 10;
    private @Setter Receiver<Message> receiver;
    @Override
    public void open() {
        super.open();

        if (pollGuardInterval > 0) {
            log.debug("Creating poll-guard timer with interval [" + pollGuardInterval + "ms] while starting SpringJmsConnector");
            DefaultMessageListenerContainer mockContainer = mock(DefaultMessageListenerContainer.class);
            SpringJmsConnector mockConnector = mock(SpringJmsConnector.class);
            when(mockConnector.getLastPollFinishedTime()).thenAnswer(invocationOnMock -> System.currentTimeMillis() - mockLastPollDelayMs);
            when(mockConnector.getThreadsProcessing()).thenReturn(new Counter(0));
            when(mockConnector.getReceiver()).thenReturn(receiver);
            when(mockConnector.getListener()).thenReturn(this);
            when(mockConnector.getJmsContainer()).thenReturn(mockContainer);
            when(mockConnector.getLogPrefix()).thenReturn("MockJmsConnector ");

            pollGuard = new PollGuard();
            pollGuard.setSpringJmsConnector(mockConnector);
            pollGuardTimer = new Timer(true);
            pollGuardTimer.schedule(pollGuard, pollGuardInterval, pollGuardInterval);
        }
    }

    @Override
    public void close() {
        if (pollGuardTimer != null) {
            log.debug("Cancelling previous poll-guard timer while stopping SpringJmsConnector");
            pollGuardTimer.cancel();
            pollGuardTimer = null;
        }
        super.close();
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
    public IListenerConnector<Message> getListenerPortConnector() {
        return null;
    }
}
