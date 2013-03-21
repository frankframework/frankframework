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
package nl.nn.adapterframework.http;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringResolver;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.util.IdleConnectionTimeoutThread;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public class HttpConnectionManager extends MultiThreadedHttpConnectionManager {
	protected Logger log = LogUtil.getLogger(this);
	
	private static String CONNECTION_TIMEOUT_KEY="http.connection.timeout";
	private static String TIMEOUT_INTERVAL_KEY="http.timeout.interval";
	private static String REUSE_CONNECTIONS_KEY="http.reuse.connections";

	private static int DEFAULT_CONNECTION_TIMEOUT=30; // timeout in seconds
	private static int DEFAULT_TIMEOUT_INTERVAL=30;   // interval in seconds
	private static boolean DEFAULT_REUSE_CONNECTIONS=true;   

	private IdleConnectionTimeoutThread idleConnectionTimeoutThread=null;

	private boolean reuseConnections=DEFAULT_REUSE_CONNECTIONS;
	

	private void initIdleConnectionsHandler(long connectionTimeout, long timeoutInterval, String ownerName) {
		AppConstants ac = AppConstants.getInstance();
		try {
			if (connectionTimeout==0) {
				connectionTimeout = ac.getInt(CONNECTION_TIMEOUT_KEY,DEFAULT_CONNECTION_TIMEOUT);
			}
		} catch (Exception e) {
			log.warn("could not parse connection timeout from property ["+CONNECTION_TIMEOUT_KEY+"]", e);
		}
		try {
			if (timeoutInterval==0) {
				timeoutInterval = ac.getInt(TIMEOUT_INTERVAL_KEY,DEFAULT_TIMEOUT_INTERVAL);
			}
		} catch (Exception e) {
			log.warn("could not parse timeout interval from property ["+TIMEOUT_INTERVAL_KEY+"]", e);
		}
		if (connectionTimeout>0 && timeoutInterval>0) {
			log.debug("starting idleConnectionTimeoutThread, timeout ["+connectionTimeout+"] s interval ["+timeoutInterval+"] s");
			idleConnectionTimeoutThread = new IdleConnectionTimeoutThread();
			idleConnectionTimeoutThread.setConnectionTimeout(connectionTimeout*1000);
			idleConnectionTimeoutThread.setTimeoutInterval(timeoutInterval*1000);
			idleConnectionTimeoutThread.addConnectionManager(this);
			idleConnectionTimeoutThread.setName("IdleConnectionManager-"+idleConnectionTimeoutThread.getName()+"-for-"+ownerName);
			idleConnectionTimeoutThread.start();
			log.info("started idleConnectionTimeoutThread ["+idleConnectionTimeoutThread.getName()+"], timeout ["+connectionTimeout+"] s interval ["+timeoutInterval+"] s");
		}
	}

	public HttpConnectionManager(long connectionTimeout, String ownerName) {
		super();
		String reuseConnectionsString = StringResolver.getSystemProperty(REUSE_CONNECTIONS_KEY, Boolean.toString(DEFAULT_REUSE_CONNECTIONS));
		try {
			reuseConnections="true".equalsIgnoreCase(reuseConnectionsString);
		} catch (Throwable t) {
			log.warn("could not parse reuseConnections ["+reuseConnectionsString+"]",t);
		}
		if (reuseConnections) {
			initIdleConnectionsHandler(connectionTimeout/2, connectionTimeout/2, ownerName);
		}
	}

	public synchronized void shutdown() {
		if (idleConnectionTimeoutThread!=null) {
			log.info("shutting down idleConnectionTimeoutThread ["+idleConnectionTimeoutThread.getName()+"]");
			idleConnectionTimeoutThread.shutdown();
			if (log.isDebugEnabled()) log.debug("idleConnectionTimeoutThread ["+idleConnectionTimeoutThread.getName()+"] shut down");
		}
		super.shutdown();
	}

	public void closeIdleConnections(long timeout) {
		int poolsizeBefore=this.getConnectionsInPool();
		if (poolsizeBefore>0) {
			super.closeIdleConnections(timeout);
			deleteClosedConnections();
			int poolsizeAfter=this.getConnectionsInPool();
			if (poolsizeAfter!=poolsizeBefore) {
				if (log.isInfoEnabled()) log.info("poolsize changed from ["+poolsizeBefore+"] to ["+poolsizeAfter+"] by deleting closed connections");
			} else {
				if (log.isDebugEnabled()) log.debug("poolsize ["+poolsizeBefore+"] did not change by deleting closed connections");
			}
		}
	}


	public void releaseConnection(HttpConnection connection) {
		if (connection!=null) {
			if (!reuseConnections) {
				connection.close();
				deleteClosedConnections();
			}
		}
		super.releaseConnection(connection);
	}


}
