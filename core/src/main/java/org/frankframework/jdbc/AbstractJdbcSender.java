/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.builder.ToStringBuilder;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IBlockEnabledSender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.HasApplicationContext;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.util.JdbcUtil;

/**
 * Base class for building JDBC-senders.
 *
 * @author  Gerrit van Brakel
 * @since 	4.2.h
 */
public abstract class AbstractJdbcSender<H> extends JdbcFacade implements IBlockEnabledSender<H>, HasApplicationContext {
	private DistributionSummary connectionStatistics;
	private @Setter MetricsInitializer configurationMetrics;

	@Getter private int timeout = 0;

	protected Connection connection=null;
	protected ParameterList paramList = null;

	public AbstractJdbcSender() {
		super();
	}

	@Override
	public void addParameter(IParameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if(configurationMetrics != null) {
			// The metrics are not always autowired, as this class is also (ab)used by ConfigurationUtils.
			connectionStatistics = configurationMetrics.createSubDistributionSummary(this, "getConnection", FrankMeterType.PIPE_DURATION);
		}

		if (paramList!=null) {
			paramList.configure();
		}
	}

	@Override
	public void start() {
		try {
			connection = getConnection();
			connection.getMetaData(); // We have to perform some DB action, it could be stale or not present (yet)
		} catch (Exception e) {
			JdbcUtil.close(connection);
			connection = null;

			throw new LifecycleException(e);
		}

		// When we use pooling connections we need to ask for a new connection every time we want to use it
		if (isConnectionsArePooled()) {
			this.stop();
		}
	}

	@Override
	public void stop() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			log.warn("caught exception stopping sender", e);
		} finally {
			connection = null;
			super.stop();
		}
	}

	/* Only here for statistic purposes */
	@Override
	public Connection getConnection() throws JdbcException {
		long t0 = System.currentTimeMillis();
		try {
			return super.getConnection();
		} finally {
			if(connectionStatistics != null) {
				connectionStatistics.record((double) System.currentTimeMillis() - t0);
			}
		}
	}

	@Override
	public final @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		H blockHandle = openBlock(session);
		try {
			return sendMessage(blockHandle, message, session);
		} finally {
			closeBlock(blockHandle, session);
		}
	}

	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("name", getName());
		result += ts.toString();
		return result;
	}

	/**
	 * The number of seconds the JDBC driver will wait for a statement object to execute. If the limit is exceeded, a TimeoutException is thrown. A value of 0 means execution time is not limited
	 * @ff.default 0
	 */
	public void setTimeout(int i) {
		timeout = i;
	}

}
