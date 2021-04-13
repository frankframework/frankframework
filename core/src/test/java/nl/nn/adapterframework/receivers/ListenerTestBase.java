/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.LogUtil;

/**
 * How to test listeners? The main focus is on what to do after a listener has received a 'raw' message.
 */
public abstract class ListenerTestBase<S extends IListener<Object>> extends Mockito {

	protected static final String STUB_RESULT_KEY = "STUB_RESULT_KEY";
	protected Logger log = LogUtil.getLogger(this);
	protected S listener;

	@Mock
	protected Map<String,Object> threadContext;

	public abstract S createListener() throws Exception;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		String messageId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb1";
		String technicalCorrelationId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb2";
		if(listener instanceof IPullingListener) {
			threadContext = ((IPullingListener<Object>) listener).openThread();
		} else {
			threadContext = new HashMap<>();
		}

		PipeLineSession.setListenerParameters(threadContext, messageId, technicalCorrelationId, null, null);
		listener = createListener();
	}

	protected Object getRawMessage(String mockedResult) throws ListenerException {
		threadContext.put(STUB_RESULT_KEY, mockedResult);
		if(listener instanceof IPullingListener) {
			@SuppressWarnings("unchecked")
			IPullingListener<Object> pullingListener = (IPullingListener<Object>) listener;
			return pullingListener.getRawMessage(threadContext);
		}
		return null;
	}

	@After
	public void tearDown() throws ListenerException {
		if (listener != null) {
			listener.close();
			listener = null;
		}
	}

}
