/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Semaphore;

public class ParallelSenderExecutor extends RequestReplyExecutor {
	private Logger log = LogUtil.getLogger(this);
	private ISender sender;
	private IPipeLineSession session;
	private Semaphore semaphore; // supports to limit the number of threads processing in parallel, may be null
	private Guard guard;         // supports to wait for all threads to have ended
	private StatisticsKeeper sk;
	private ThreadConnector<?> threadConnector;

	public ParallelSenderExecutor(ISender sender, Message message, IPipeLineSession session, Guard guard, StatisticsKeeper sk, ThreadLifeCycleEventListener<?> threadLifeCycleEventListener) {
		this(sender, message, session, null, guard, sk, threadLifeCycleEventListener);
	}
	
	public ParallelSenderExecutor(ISender sender, Message message, IPipeLineSession session, Semaphore semaphore, Guard guard, StatisticsKeeper sk, ThreadLifeCycleEventListener<?> threadLifeCycleEventListener) {
		super();
		this.sender=sender;
		request=message;
		this.session=session;
		this.guard=guard;
		this.semaphore=semaphore;
		this.sk=sk;
		correlationID = session.getMessageId();
		threadConnector = new ThreadConnector<>(sender, threadLifeCycleEventListener, session);
	}

	@Override
	public void run() {
		request = threadConnector.startThread(request);
		try {
			long t1 = System.currentTimeMillis();
			try {
				reply = sender.sendMessage(request,session);
				reply = threadConnector.endThread(reply);
			} catch (Throwable tr) {
				threadConnector.abortThread(tr);
				throwable = tr;
				log.warn("SenderExecutor caught exception",tr);
			}
			long t2 = System.currentTimeMillis();
			sk.addValue(t2-t1);
		} finally {
			if (semaphore!=null) {
				semaphore.release();
			}
			guard.releaseResource();
		}
	}

}
