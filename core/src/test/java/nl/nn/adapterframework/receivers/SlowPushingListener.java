package nl.nn.adapterframework.receivers;

import java.util.Map;

import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.stream.Message;

public class SlowPushingListener extends SlowListenerBase implements IPushingListener<javax.jms.Message> {


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
	public void setHandler(IMessageHandler<javax.jms.Message> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		// TODO Auto-generated method stub

	}
}
