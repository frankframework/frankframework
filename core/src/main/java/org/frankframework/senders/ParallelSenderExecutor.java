/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import java.util.concurrent.Semaphore;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.RequestReplyExecutor;
import org.frankframework.stream.Message;
import org.frankframework.util.Guard;
import org.frankframework.util.LogUtil;

public class ParallelSenderExecutor extends RequestReplyExecutor {
	private final Logger log = LogUtil.getLogger(this);
	private final ISender sender;
	@Getter private final PipeLineSession session;
	private final Semaphore semaphore; // support limiting the number of threads processing in parallel, may be null
	private final Guard guard;         // support waiting for all threads to end
	private final DistributionSummary summary;
	private @Getter long duration;

	public ParallelSenderExecutor(ISender sender, Message message, PipeLineSession session, Guard guard, DistributionSummary sk) {
		this(sender, message, session, null, guard, sk);
	}

	public ParallelSenderExecutor(ISender sender, Message message, PipeLineSession session, Semaphore semaphore, Guard guard, DistributionSummary sk) {
		super();
		this.sender=sender;
		request=message;
		this.session=session;
		this.guard=guard;
		this.semaphore=semaphore;
		this.summary=sk;
	}

	@Override
	public void run() {
		try {
			long t1 = System.currentTimeMillis();
			try {
				reply = sender.sendMessage(request,session);
				reply.getResult().preserve(); // consume the message immediately, to release any resources (like connections) associated with the sender execution
			} catch (Throwable tr) {
				throwable = tr;
				log.warn("SenderExecutor caught exception",tr);
			}
			long t2 = System.currentTimeMillis();
			duration = t2-t1;
			summary.record(duration);
		} finally {
			if (semaphore!=null) {
				semaphore.release();
			}
			if(guard != null) {
				guard.releaseResource();
			}
		}
	}

}
