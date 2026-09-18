/*
   Copyright 2025-2026 WeAreFrank!

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

import jakarta.jms.ConnectionFactory;

import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.jms.connection.DelegatingConnectionFactory;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.StringUtil;

@Log4j2
public class JmsPoolUtil {

	private static final String CLOSE = "], ";

	private JmsPoolUtil() {
		// Hide implicit public constructor
	}

	/** Returns pool info or NULL when it's not able to do so. */
	public static @Nullable String getConnectionPoolInfo(@Nullable ConnectionFactory qcf) {
		return switch (qcf) {
			case JmsPoolConnectionFactory targetQcf -> getJmsPoolInfo(targetQcf);
			case DelegatingConnectionFactory source -> getConnectionPoolInfo(source.getTargetConnectionFactory()); // Perhaps it's wrapped?
			case null, default -> null;
		};
	}

	@NonNull
	public static String reflectionToString(@NonNull ConnectionFactory qcf) {
		Object factory = getManagedConnectionFactory(qcf);
		return StringUtil.reflectionToString(factory);
	}

	/** Retrieve the 'original' ConnectionFactory, used by the console (to get the Tibco QCF) in order to display queue message count. */
	@Nullable
	private static Object getManagedConnectionFactory(@NonNull ConnectionFactory qcf) {
		if (qcf instanceof DelegatingConnectionFactory source) { // Perhaps it's wrapped?
			ConnectionFactory targetConnectionFactory = source.getTargetConnectionFactory();
			if (targetConnectionFactory == null) {
				return null;
			}
			return getManagedConnectionFactory(targetConnectionFactory);
		}
		if (qcf instanceof JmsPoolConnectionFactory factory) { // Narayana with pooling
			return factory.getConnectionFactory();
		}
		try {
			if (qcf instanceof ConnectionFactoryProxy) { // Narayana without pooling
				return ClassUtils.getDeclaredFieldValue(qcf, ConnectionFactoryProxy.class, "xaConnectionFactory");
			}

			// JCA ManagedConnectionFactory, but unsure who would be the owner
			return ClassUtils.invokeGetter(qcf, "getManagedConnectionFactory", true);
		} catch (NoSuchMethodException | NoSuchFieldException e) {
			// Either the field or method does not exist. Unsure if this is the most outer factory, but let's use it!
			return qcf;
		} catch (Exception e) {
			// Unsure what went wrong here, return null.
			log.warn("could not determine managed connection factory", e);
			return null;
		}
	}

	/** Return pooling info if present */
	private static @NonNull String getJmsPoolInfo(@NonNull JmsPoolConnectionFactory poolcf) {
		return ClassUtils.classNameOf(poolcf) + " Pool Info: " +
				"current pool size [" + poolcf.getNumConnections() + CLOSE +
				"max pool size [" + poolcf.getMaxConnections() + CLOSE +
				"max sessions per connection [" + poolcf.getMaxSessionsPerConnection() + CLOSE +
				"block if session pool is full [" + poolcf.isBlockIfSessionPoolIsFull() + CLOSE +
				"block if session pool is full timeout [" + poolcf.getBlockIfSessionPoolIsFullTimeout() + CLOSE +
				"connection check interval (ms) [" + poolcf.getConnectionCheckInterval() + CLOSE +
				"connection idle timeout (s) [" + poolcf.getConnectionIdleTimeout() / 1000 + "]";
	}
}
