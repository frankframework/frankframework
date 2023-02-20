/*
   Copyright 2021, 2022 WeAreFrank!

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

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.SpringUtils;

public class ExecuteQueryJob extends JobDef {
	private FixedQuerySender qs = null;
	private @Getter String query;
	private @Getter String jmsRealm;
	private @Getter String datasourceName;
	private @Getter int queryTimeout;

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
	public void execute() throws JobExecutionException, TimeoutException {
		try {
			qs.open();
			Message result = qs.sendMessageOrThrow(Message.nullMessage(), null);
			log.info("result [" + result + "]");
		}
		catch (SenderException e) {
			throw new JobExecutionException("unable to execute query ["+getQuery()+"]", e);
		}
		finally {
			qs.close();
		}
	}

	/**
	 * The SQL query text to be executed
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	@Deprecated
	@ConfigurationWarning("Please configure a datasourceName instead")
	public void setJmsRealm(String jmsRealm) {
		this.jmsRealm = jmsRealm;
	}

	/**
	 * JNDI name of datasource to be used
	 * @ff.default {@value JndiDataSourceFactory#DEFAULT_DATASOURCE_NAME_PROPERTY}
	 */
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	/**
	 * The number of seconds the database driver will wait for a statement to execute. If the limit is exceeded, a TimeoutException is thrown. 0 means no timeout
	 * @ff.default 0
	 */
	public void setQueryTimeout(int i) {
		queryTimeout = i;
	}
}
