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

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

public class ParallelSenderExecutor extends RequestReplyExecutor {
	private Logger log = LogUtil.getLogger(this);
	private ISender sender;
	private ParameterResolutionContext prc;
	private Guard guard;
	private StatisticsKeeper sk;

	public ParallelSenderExecutor(ISender sender, String correlationID,
			String message, ParameterResolutionContext prc, Guard guard,
			StatisticsKeeper sk) {
		super();
		this.sender=sender;
		this.correlationID=correlationID;
		request=message;
		this.prc=prc;
		this.guard=guard;
		this.sk=sk;
	}

	public void run() {
		try {
			long t1 = System.currentTimeMillis();
			try {
				if (sender instanceof ISenderWithParameters) {
					reply = ((ISenderWithParameters)sender).sendMessage(correlationID,request,prc);
				} else {
					reply = sender.sendMessage(correlationID,request);
				}
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
