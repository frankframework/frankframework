/*
   Copyright 2021, 2023 WeAreFrank!

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;

import nl.nn.adapterframework.core.ConfiguredTestBase;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.LogUtil;

/**
 * How to test listeners? The main focus is on what to do after a listener has received a 'raw' message.
 */
public abstract class ListenerTestBase<M extends Object, S extends IListener<M>> extends Mockito {

	protected static final String STUB_RESULT_KEY = "STUB_RESULT_KEY";
	protected Logger log = LogUtil.getLogger(this);
	protected S listener;

	@Mock
	protected Map<String,Object> threadContext;

	public abstract S createListener() throws Exception;

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		if(listener instanceof IPullingListener) {
			threadContext = ((IPullingListener<M>) listener).openThread();
		} else {
			threadContext = new HashMap<>();
		}

		PipeLineSession.updateListenerParameters(threadContext, ConfiguredTestBase.testMessageId, ConfiguredTestBase.testCorrelationId);
		listener = createListener();
	}

	protected RawMessageWrapper<M> getRawMessage(String mockedResult) throws ListenerException {
		threadContext.put(STUB_RESULT_KEY, mockedResult);
		if(listener instanceof IPullingListener) {
			@SuppressWarnings("unchecked")
			IPullingListener<M> pullingListener = (IPullingListener<M>) listener;
			return pullingListener.getRawMessage(threadContext);
		}
		return null;
	}

	@AfterEach
	public void tearDown() throws ListenerException {
		if (listener != null) {
			listener.close();
			listener = null;
		}
	}

}
