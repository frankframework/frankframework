/*
 * $Log: HttpConnectionManager.java,v $
 * Revision 1.3  2011-11-30 13:52:01  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/08/26 11:47:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * upgrade to HttpClient 3.0.1 - including idle connection cleanup
 *
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
 * @version Id
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
