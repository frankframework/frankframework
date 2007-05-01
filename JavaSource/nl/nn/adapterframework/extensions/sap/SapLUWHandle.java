/*
 * $Log: SapLUWHandle.java,v $
 * Revision 1.1  2007-05-01 14:21:31  europe\L190409
 * introduction of SAP LUW management
 *
 */
package nl.nn.adapterframework.extensions.sap;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.LogUtil;

import com.sap.mw.jco.JCO;

/**
 * Wrapper for SAP sessions, used to control Logical Units of Work (LUWs).
 * 
 * @author  Gerrit van Brakel
 * @since   4.6.0
 * @version Id
 */
public class SapLUWHandle {
	protected static Logger log = LogUtil.getLogger(SapLUWHandle.class);

	private SapSystem sapSystem;
	private JCO.Client client;
	private String tid;
	private boolean useTid=false;
	
	private SapLUWHandle(SapSystem sapSystem) {
		super();
		this.sapSystem = sapSystem;
		this.client = sapSystem.getClient();
	}

	public static SapLUWHandle createHandle(PipeLineSession session, String sessionKey, SapSystem sapSystem, boolean useTid) {
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

	public static SapLUWHandle retrieveHandle(PipeLineSession session, String sessionKey, boolean create, SapSystem sapSystem, boolean useTid) {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		if (result==null && create) {
			return createHandle(session, sessionKey, sapSystem, useTid);
		}
		return result;
	}

	public static void releaseHandle(PipeLineSession session, String sessionKey) {
		SapLUWHandle handle=(SapLUWHandle)session.get(sessionKey);
		if (handle==null) {
			log.debug("no handle found under session key ["+sessionKey+"]");
		} else {
			handle.release();
			session.remove(sessionKey);
		}
	}

	

	public void begin() {
		if (isUseTid()) {
			tid=client.createTID();
			log.debug("begin: created SAP TID ["+tid+"]");
		}
	}

	public void commit() {
		if (isUseTid()) {
			client.confirmTID(tid);
			log.debug("commit: confirmed SAP TID ["+tid+"]");
		} else {
			log.warn("Should Execute COMMIT by calling COMMIT BAPI");
		}
		
	}

	public void rollback() {
		client.reset();
		log.debug("rollback: reset connection, forget about SAP TID ["+tid+"]");
		tid=null;
	}
	
	public void release() {
		sapSystem.releaseClient(client);
		log.debug("release: releaseed connection to System");
	}

	public JCO.Client getClient() {
		return client;
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
