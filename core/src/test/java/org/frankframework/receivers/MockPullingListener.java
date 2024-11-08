package org.frankframework.receivers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;

public class MockPullingListener extends MockListenerBase implements IPullingListener<String> {
	private final BlockingQueue<String> value = new ArrayBlockingQueue<>(5);

	@Override
	public Map<String, Object> openThread() throws ListenerException {
		return new HashMap<>();
	}

	@Override
	public void closeThread(Map<String, Object> threadContext) throws ListenerException {
		//No-op
	}

	@Override
	void offerMessage(String text) {
		value.add(text);
	}

	@Override
	public RawMessageWrapper<String> getRawMessage(Map<String, Object> threadContext) throws ListenerException {
		String message = value.poll();
		if(message != null) {
			if("getRawMessageException".equals(message)) {
				throw new ListenerException(message);
			}
			return new RawMessageWrapper<>(message, Integer.toString(message.hashCode()), Integer.toString(message.hashCode()));
		}
		return null;
	}

}
