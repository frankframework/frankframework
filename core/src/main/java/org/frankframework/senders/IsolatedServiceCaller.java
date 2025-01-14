/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package org.frankframework.senders;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;
import org.springframework.core.task.TaskExecutor;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.stream.Message;
import org.frankframework.threading.ThreadLifeCycleEventListener;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;

/**
 * Helper class for {@link IbisLocalSender} that wraps around {@link ServiceClient} implementation to make calls to a local Ibis adapter in a separate thread.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class IsolatedServiceCaller {
	protected Logger log = LogUtil.getLogger(this);

	/**
	 * The thread-pool for spawning threads, injected by Spring
	 */
	@Setter @Getter private TaskExecutor taskExecutor;

	public void callServiceAsynchronous(ServiceClient service, Message message, PipeLineSession session, ThreadLifeCycleEventListener<?> threadLifeCycleEventListener) throws IOException {
		IsolatedServiceExecutor ise = new IsolatedServiceExecutor(service, message, session, null, threadLifeCycleEventListener, true);
		getTaskExecutor().execute(ise);
	}

	public SenderResult callServiceIsolated(ServiceClient service, Message message, PipeLineSession session, ThreadLifeCycleEventListener<?> threadLifeCycleEventListener) throws ListenerException, IOException {
		CountDownLatch guard = new CountDownLatch(1);
		IsolatedServiceExecutor ise = new IsolatedServiceExecutor(service, message, session, guard, threadLifeCycleEventListener, false);
		getTaskExecutor().execute(ise);
		try {
			guard.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ListenerException(ClassUtils.nameOf(this)+" was interrupted",e);
		}
		Throwable throwable = ise.getThrowable();
		if (throwable != null) {
			if (throwable instanceof ListenerException listenerException) {
				throw listenerException;
			}
			throw new ListenerException(throwable);
		}
		return ise.getReply();
	}
}
