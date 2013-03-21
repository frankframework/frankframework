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
/*
 * $Log: SapLUWHandle.java,v $
 * Revision 1.2  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.1  2012/02/06 14:33:05  Jaco de Groot <jaco.de.groot@ibissource.org>
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
package nl.nn.adapterframework.extensions.sap.jco2;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

import com.sap.mw.jco.JCO;

/**
 * Wrapper for SAP sessions, used to control Logical Units of Work (LUWs).
 * 
 * @author  Gerrit van Brakel
 * @since   4.6.0
 * @version $Id$
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

	public static SapLUWHandle createHandle(IPipeLineSession session, String sessionKey, SapSystem sapSystem, boolean useTid) {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		if (result!=null) {
			log.warn("LUWHandle already exists under key ["+sessionKey+"]");
		}
		result = new SapLUWHandle(sapSystem);
		result.setUseTid(useTid);
		session.put(sessionKey,result);
		return result;
	}

	public static SapLUWHandle retrieveHandle(IPipeLineSession session, String sessionKey) {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		return result;
	}

	public static SapLUWHandle retrieveHandle(IPipeLineSession session, String sessionKey, boolean create, SapSystem sapSystem, boolean useTid) {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		if (result==null && create) {
			return createHandle(session, sessionKey, sapSystem, useTid);
		}
		return result;
	}

	public static void releaseHandle(IPipeLineSession session, String sessionKey) {
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
