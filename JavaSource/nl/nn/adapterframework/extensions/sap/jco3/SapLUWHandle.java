/*
 * $Log: SapLUWHandle.java,v $
 * Revision 1.1  2012-02-06 14:33:04  m00f069
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 * Revision 1.3  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2007/05/01 14:21:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SAP LUW management
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.extensions.sap.jco3.tx.RollbackException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

/**
 * Wrapper for SAP sessions, used to control Logical Units of Work (LUWs).
 * 
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class SapLUWHandle {
	protected static Logger log = LogUtil.getLogger(SapLUWHandle.class);

	private SapSystem sapSystem;
	private JCoDestination destination;
	private String tid;
	private boolean useTid=false;
	
	private SapLUWHandle(SapSystem sapSystem) throws JCoException {
		super();
		this.sapSystem = sapSystem;
		this.destination = sapSystem.getDestination();
	}

	public static SapLUWHandle createHandle(PipeLineSession session, String sessionKey, SapSystem sapSystem, boolean useTid) throws JCoException {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		if (result!=null) {
			log.warn("LUWHandle already exists under key ["+sessionKey+"]");
		}
		result = new SapLUWHandle(sapSystem);
		result.setUseTid(useTid);
		session.put(sessionKey,result);
		return result;
	}

	public static SapLUWHandle retrieveHandle(PipeLineSession session, String sessionKey) {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		return result;
	}

	public static SapLUWHandle retrieveHandle(PipeLineSession session, String sessionKey, boolean create, SapSystem sapSystem, boolean useTid) throws JCoException {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		if (result==null && create) {
			return createHandle(session, sessionKey, sapSystem, useTid);
		}
		return result;
	}

	public static void releaseHandle(PipeLineSession session, String sessionKey) throws JCoException {
		SapLUWHandle handle=(SapLUWHandle)session.get(sessionKey);
		if (handle==null) {
			log.debug("no handle found under session key ["+sessionKey+"]");
		} else {
			handle.release();
			session.remove(sessionKey);
		}
	}

	

	public void begin() throws JCoException {
		if (isUseTid()) {
			tid=destination.createTID();
			log.debug("begin: created SAP TID ["+tid+"]");
		} else {
			// Use a stateful connection to make commit through BAPI work, this is
			// probably not needed when using tid, but haven't found
			// documentation on it yet.
			JCoContext.begin(destination);
			log.debug("begin: stateful connection");
		}
	}

	public void commit() throws JCoException {
		if (isUseTid()) {
			destination.confirmTID(tid);
			log.debug("commit: confirmed SAP TID ["+tid+"]");
		} else {
			log.warn("Should Execute COMMIT by calling COMMIT BAPI");
		}
	}

	public void rollback() {
		log.debug("rollback: forget about SAP TID ["+tid+"], throw exception to signal SAP");
		tid=null;
		throw new RollbackException();
	}
	
	public void release() throws JCoException {
		if (!isUseTid()) {
			// End the stateful connection
			JCoContext.end(destination);
			log.debug("release: stateful connection");
		}
	}

	public JCoDestination getDestination() {
		return destination;
	}
	public String getTid() {
		return tid;
	}

	public void setUseTid(boolean b) {
		useTid = b;
	}
	public boolean isUseTid() {
		return useTid;
	}


}
