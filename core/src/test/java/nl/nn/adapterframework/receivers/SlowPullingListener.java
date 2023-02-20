package nl.nn.adapterframework.receivers;

import java.util.LinkedHashMap;
import java.util.Map;

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.stream.Message;

public class SlowPullingListener extends SlowListenerBase implements IPullingListener<javax.jms.Message> {

	@Override
	public String getIdFromRawMessage(javax.jms.Message rawMessage, Map<String, Object> context) throws ListenerException {
		return null;
	}

	@Override
	public Message extractMessage(javax.jms.Message rawMessage, Map<String, Object> context) throws ListenerException {
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
		log.debug("closeThread called in slow pulling listener");
	}

	@Override
	public javax.jms.Message getRawMessage(Map<String, Object> threadContext) throws ListenerException {
		return null;
	}

}
