package nl.nn.adapterframework.receivers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.stream.Message;

public class SlowStartingPullingListener implements IPullingListener<String>{
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;
	private @Getter @Setter int startupDelay = 10000;

	@Override
	public void configure() throws ConfigurationException {
		//Nothing to configure
	}

	@Override
	public void open() throws ListenerException {
		try {
			Thread.sleep(getStartupDelay());
		} catch (InterruptedException e) {
			throw new ListenerException("InterruptedException while opening listener", e);
		}
	}

	@Override
	public void close() throws ListenerException {
		//Nothing to close
	}

	@Override
	public String getIdFromRawMessage(String rawMessage, Map<String, Object> context) throws ListenerException {
		return null;
	}

	@Override
	public Message extractMessage(String rawMessage, Map<String, Object> context) throws ListenerException {
		return Message.asMessage(rawMessage);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String, Object> context) throws ListenerException {
	}

	@Override
	public Map<String, Object> openThread() throws ListenerException {
		return new LinkedHashMap<String,Object>();
	}

	@Override
	public void closeThread(Map<String, Object> threadContext) throws ListenerException {
		
	}

	@Override
	public String getRawMessage(Map<String, Object> threadContext) throws ListenerException {
		return null;
	}

}