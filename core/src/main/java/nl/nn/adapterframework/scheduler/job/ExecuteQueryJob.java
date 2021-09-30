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
package nl.nn.adapterframework.scheduler.job;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.SpringUtils;

public class ExecuteQueryJob extends JobDef implements IJob {
	private FixedQuerySender qs = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		qs = SpringUtils.createBean(getApplicationContext(), FixedQuerySender.class);
		qs.setQuery(getQuery());
		qs.setName("executeQueryJob");
		if(StringUtils.isNotEmpty(getJmsRealm())) {
			qs.setJmsRealm(getJmsRealm());
		} else {
			qs.setDatasourceName(getDatasourceName());
		}
		qs.setQueryType("other");
		qs.setTimeout(getQueryTimeout());
		qs.configure();
	}

	@Override
	public void execute(IbisManager ibisManager) throws SenderException, TimeOutException {
		try {
			qs.open();
			Message result = qs.sendMessage(Message.nullMessage(), null);
			log.info("result [" + result + "]");
		} finally {
			qs.close();
		}
	}
}
