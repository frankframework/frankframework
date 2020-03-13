/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Guard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParallelSenderExecutor extends RequestReplyExecutor {
	private Logger log = LogManager.getLogger(this);
	private ISender sender;
	private IPipeLineSession session;
	private Guard guard;
	private StatisticsKeeper sk;

	public ParallelSenderExecutor(ISender sender, Message message, IPipeLineSession session, Guard guard, StatisticsKeeper sk) {
		super();
		this.sender=sender;
		request=message;
		this.session=session;
		this.guard=guard;
		this.sk=sk;
	}

	@Override
	public void run() {
		try {
			long t1 = System.currentTimeMillis();
			try {
				reply = sender.sendMessage(request,session);
			} catch (Throwable tr) {
				throwable = tr;
				log.warn("SenderExecutor caught exception",tr);
			}
			long t2 = System.currentTimeMillis();
			sk.addValue(t2-t1);
		} finally {
			guard.releaseResource();
		}
	}

}
