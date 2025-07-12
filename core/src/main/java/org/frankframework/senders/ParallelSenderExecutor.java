/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.util.concurrent.Phaser;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.AbstractRequestReplyExecutor;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.ResourceLimiter;
import org.frankframework.stream.Message;

@Log4j2
public class ParallelSenderExecutor extends AbstractRequestReplyExecutor {
	private final ISender sender;
	@Getter private final PipeLineSession session;
	@Setter private boolean shouldCloseSession = false;
	@Setter private ResourceLimiter threadLimiter; // support limiting the number of threads processing in parallel
	@Setter private Phaser guard; // support waiting for all threads to end
	private final DistributionSummary summary;
	private @Getter long duration;

	public ParallelSenderExecutor(ISender sender, Message message, PipeLineSession session, DistributionSummary summary) {
		super();
		this.sender = sender;
		request = message;
		this.session = session;
		this.summary = summary;
	}

	@Override
	public void run() {
		try {
			long startTime = System.currentTimeMillis();
			try {
				reply = sender.sendMessage(request, session);

				// To make sure that message is fully read and any potential resources held by open connections
				// are closed, create a copy of the result-message but don't use it.
				// (We used to achieve this with message.preserve() but that method is no longer exposed)
				//
				// NB: If multiple threads get the same result-message then they can try to copy/preserve it
				// at the same time, causing potential race conditions. Therefore have to synchronize on the
				// result-message.
				Message result = reply.getResult();
				synchronized (result) {
					reply.getResult().copyMessage().close();
				}
			} catch (Throwable tr) {
				throwable = tr;
				log.warn("SenderExecutor caught exception", tr);
			}
			long endTime = System.currentTimeMillis();
			duration = endTime - startTime;
			summary.record(duration);
		} finally {
			if (shouldCloseSession) {
				session.close();
			}
			if (threadLimiter != null) {
				threadLimiter.release();
				log.debug("Released this limiter, available permits: {}", threadLimiter.availablePermits());
			}
			if (guard != null) {
				guard.arrive();
				log.debug("Arrived sender, remaining senders: {}", guard.getUnarrivedParties());
			}
		}
	}

}
