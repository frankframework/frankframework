/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.jms;

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Factory for {@link MessagingSource}s, to share them for JMS Objects that can use the same.
 *
 * @author Gerrit van Brakel
 */
public abstract class MessagingSourceFactory {
	protected Logger log = LogUtil.getLogger(this);

	protected abstract Map<String,MessagingSource> getMessagingSourceMap();
	protected abstract Context createContext() throws NamingException;
	protected abstract ConnectionFactory createConnectionFactory(Context context, String id, boolean createDestination, boolean useJms102) throws IbisException, NamingException;

	protected MessagingSource createMessagingSource(String id, String authAlias, boolean createDestination, boolean useJms102) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(context, id, createDestination, useJms102);
		return new MessagingSource(id, context, connectionFactory, getMessagingSourceMap(), authAlias, createDestination, useJms102);
	}

	public synchronized MessagingSource getMessagingSource(String id, String authAlias, boolean createDestination, boolean useJms102) throws IbisException {
		Map<String, MessagingSource> messagingSourceMap = getMessagingSourceMap();

		MessagingSource result = messagingSourceMap.get(id);
		if (result == null) {
			result = createMessagingSource(id, authAlias, createDestination, useJms102);
			log.debug("created new MessagingSource-object for [{}]", id);
		}
		result.increaseReferences();
		return result;
	}

	protected Context getContext() throws IbisException {
		try {
			return createContext();
		} catch (Throwable t) {
			throw new IbisException("could not obtain context", t);
		}
	}

	protected ConnectionFactory getConnectionFactory(Context context, String id, boolean createDestination, boolean useJms102) throws IbisException {
		try {
			return createConnectionFactory(context, id, createDestination, useJms102);
		} catch (Throwable t) {
			throw new IbisException("could not obtain connectionFactory ["+id+"]", t);
		}
	}
}
