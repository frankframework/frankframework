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

public class MockPullingListener implements IPullingListener<String>{
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	@Override
	public void configure() throws ConfigurationException {
		
	}

	@Override
	public void open() throws ListenerException {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void close() throws ListenerException {
		
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
	public String getName() {
		return null;
	}

	@Override
	public void setName(String name) {
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