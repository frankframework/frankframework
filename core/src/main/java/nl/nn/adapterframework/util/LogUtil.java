/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Log4j can now be started from any LogManager.getLogger() call
 *
 */
public class LogUtil {

	public static final String MESSAGE_LOGGER="MSG";


	public static Logger getRootLogger() {
		return LogManager.getRootLogger();
	}

	public static Logger getLogger(String name) {
		return LogManager.getLogger(name);
	}

	public static Logger getLogger(Class<?> clazz) {
		return LogManager.getLogger(clazz);
	}

	public static Logger getLogger(Object owner) {
		return LogManager.getLogger(owner);
	}

	/**
	 * Must be called during configure after setName has been set!
	 */
	public static Logger getMsgLogger(IAdapter adapter) {
		if(StringUtils.isEmpty(adapter.getName())) {
			return getLogger(MESSAGE_LOGGER);
		}

		return LogManager.getLogger(String.format("%s.%S", MESSAGE_LOGGER, adapter.getName()));
	}

	/**
	 * Must be called during configure after setName has been set!
	 */
	public static Logger getMsgLogger(IAdapter adapter, INamedObject object) {
		if(adapter == null || StringUtils.isEmpty(adapter.getName()) || StringUtils.isEmpty(object.getName())) {
			return getLogger(MESSAGE_LOGGER);
		}

		return LogManager.getLogger(String.format("%s.%S.%S", MESSAGE_LOGGER, adapter.getName(), object.getName()));
	}

	public static CloseableThreadContext.Instance getThreadContext(IAdapter adapter, String messageId, PipeLineSession session) {
		String lastAdapter= ThreadContext.get("adapter");
		String currentAdapter= adapter.getName();
		CloseableThreadContext.Instance ctc = CloseableThreadContext.put("adapter", currentAdapter);
		if (lastAdapter!=null && !lastAdapter.equals(currentAdapter)) {
			ctc.push("caller="+lastAdapter);
		}
		setIdsToThreadContext(ctc, messageId, session!=null ? session.getCorrelationId() : null);
		return ctc;
	}

	public static void setIdsToThreadContext(CloseableThreadContext.Instance ctc, String messageId, String correlationId) {
		if (StringUtils.isNotEmpty(messageId)) {
			ctc.put("mid", messageId);
		}
		if (StringUtils.isNotEmpty(correlationId)) {
			ctc.put("cid", correlationId);
		}
	}
}
