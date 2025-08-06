/*
   Copyright 2021 - 2024 WeAreFrank!

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
package org.frankframework.scheduler.job;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Mandatory;
import org.frankframework.jdbc.AbstractJdbcQuerySender;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;

/**
 * Scheduled job to execute JDBC Queries using a {@link FixedQuerySender}.
 *
 * {@inheritClassDoc}
 */
public class ExecuteQueryJob extends AbstractJobDef {
	private FixedQuerySender qs = null;
	private @Getter String query;
	private @Getter String datasourceName;
	private @Getter int queryTimeout;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		qs = SpringUtils.createBean(getApplicationContext());
		qs.setQuery(getQuery());
		qs.setName("executeQueryJob");
		qs.setDatasourceName(getDatasourceName());
		qs.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		qs.setTimeout(getQueryTimeout());
		qs.configure();
	}

	@Override
	public void execute() throws JobExecutionException, TimeoutException {
		try(PipeLineSession session = new PipeLineSession()) {
			qs.start();

			try (Message result = qs.sendMessageOrThrow(Message.nullMessage(), session)) {
				log.info("result [{}]", result);
			}
		} catch (LifecycleException | SenderException e) {
			throw new JobExecutionException("unable to execute query [" + getQuery() + "]", e);
		} finally {
			qs.stop();
		}
	}

	/**
	 * The SQL query text to be executed.
	 */
	@Mandatory
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * JNDI name of datasource to be used.
	 * @ff.default {@value IDataSourceFactory#DEFAULT_DATASOURCE_NAME_PROPERTY}
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
