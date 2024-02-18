/*
   Copyright 2022-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.receivers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.annotation.Nonnull;
import javax.jms.Message;

import org.frankframework.core.IPullingListener;
import org.frankframework.testutil.mock.ConnectionFactoryFactoryMock.TextMessageMock;

public class SlowPullingListener extends SlowListenerBase implements IPullingListener<javax.jms.Message> {

	private final BlockingQueue<String> value = new ArrayBlockingQueue<>(5);

	@Nonnull
	@Override
	public Map<String, Object> openThread() {
		return new LinkedHashMap<>();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) {
		log.debug("closeThread called in slow pulling listener");
	}

	@Override
	public RawMessageWrapper<Message> getRawMessage(@Nonnull Map<String, Object> threadContext) {
		String message = value.poll();
		if(message != null) {
			if(message.equals("getRawMessageException")) {
				throw new RuntimeException(message);
			}
			TextMessageMock mock = TextMessageMock.newInstance();
			mock.setText(message);
			return new RawMessageWrapper<>(mock);
		}
		return null;
	}

	/**
	 * If text equals <code>getRawMessageException</code> it throws an exception during the availability check.
	 * If text equals <code>extractMessageException</code> it throws an exception during unwrapping.
	 * If text equals <code>processMessageException</code> it throws an exception during message processing (in adapter).
	 */
	public void offerMessage(String text) {
		value.add(text);
	}
}
