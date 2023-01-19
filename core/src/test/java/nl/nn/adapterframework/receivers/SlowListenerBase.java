package nl.nn.adapterframework.receivers;

import javax.jms.Message;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListener;

public abstract class SlowListenerBase implements IListener<Message> {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;
	private @Setter int startupDelay = 10000;
	private @Setter int shutdownDelay = 0;
	private @Getter boolean closed = false;

	@Override
	public void configure() throws ConfigurationException {
		//Nothing to configure
	}

	@Override
	public void open() {
		if (startupDelay > 0) {
			try {
				Thread.sleep(startupDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void close() {
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
}
