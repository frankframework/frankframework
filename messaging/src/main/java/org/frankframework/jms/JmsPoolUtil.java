/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.jms;

import jakarta.annotation.Nullable;
import jakarta.jms.ConnectionFactory;

import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.jms.connection.DelegatingConnectionFactory;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;

@Log4j2
public class JmsPoolUtil {

	private static final String CLOSE = "], ";

	/** Returns pool info or NULL when it's not able to do so. */
	public static @Nullable String getConnectionPoolInfo(@Nullable ConnectionFactory qcf) {
		StringBuilder info = new StringBuilder();

		if (qcf instanceof JmsPoolConnectionFactory targetQcf) {
			getJmsPoolInfo(targetQcf, info);
		} else if (qcf instanceof DelegatingConnectionFactory source) { // Perhaps it's wrapped?
			return getConnectionPoolInfo(source.getTargetConnectionFactory());
		} else {
			return null;
		}

		return info.toString();
	}

	/** Retrieve the 'original' ConnectionFactory, used by the console (to get the Tibco QCF) in order to display queue message count. */
	@Nullable
	public static Object getManagedConnectionFactory(ConnectionFactory qcf) {
		if (qcf instanceof DelegatingConnectionFactory source) { // Perhaps it's wrapped?
			return getManagedConnectionFactory(source.getTargetConnectionFactory());
		}
		if (qcf instanceof JmsPoolConnectionFactory factory) { // Narayana with pooling
			return factory.getConnectionFactory();
		}
		try {
			if (qcf instanceof ConnectionFactoryProxy) { // Narayana without pooling
				return ClassUtils.getDeclaredFieldValue(qcf, ConnectionFactoryProxy.class, "xaConnectionFactory");
			}
			return ClassUtils.invokeGetter(qcf, "getManagedConnectionFactory", true);
		} catch (Throwable e) {
			log.warn("could not determine managed connection factory", e);
			return null;
		}
	}

	/** Return pooling info if present 
	 * @param info */
	private static void getJmsPoolInfo(JmsPoolConnectionFactory poolcf, StringBuilder info) {
		info.append(ClassUtils.classNameOf(poolcf)).append(" Pool Info: ");
		info.append("current pool size [").append(poolcf.getNumConnections()).append(CLOSE);
		info.append("max pool size [").append(poolcf.getMaxConnections()).append(CLOSE);
		info.append("max sessions per connection [").append(poolcf.getMaxSessionsPerConnection()).append(CLOSE);
		info.append("block if session pool is full [").append(poolcf.isBlockIfSessionPoolIsFull()).append(CLOSE);
		info.append("block if session pool is full timeout [").append(poolcf.getBlockIfSessionPoolIsFullTimeout()).append(CLOSE);
		info.append("connection check interval (ms) [").append(poolcf.getConnectionCheckInterval()).append(CLOSE);
		info.append("connection idle timeout (s) [").append(poolcf.getConnectionIdleTimeout() / 1000).append("]");
	}
}
