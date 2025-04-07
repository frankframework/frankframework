/*
   Copyright 2013 Nationale-Nederlanden, 2022, 2025 WeAreFrank!

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
package org.frankframework.extensions.sap.jco3;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.extensions.sap.jco3.tx.RollbackException;

/**
 * Wrapper for SAP sessions, used to control Logical Units of Work (LUWs).
 *
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
@Log4j2
public class SapLUWHandle implements AutoCloseable {
	private final @Getter JCoDestination destination;
	private @Getter String tid;
	private @Getter @Setter boolean useTid=false;
	private boolean released = false;

	private SapLUWHandle(SapSystemImpl sapSystem) throws JCoException {
		super();
		this.destination = sapSystem.getDestination();
	}

	public static SapLUWHandle createHandle(PipeLineSession session, String sessionKey, SapSystemImpl sapSystem, boolean useTid) throws JCoException {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		if (result!=null) {
			log.warn("LUWHandle already exists under key [{}]", sessionKey);
		}
		result = new SapLUWHandle(sapSystem);
		result.setUseTid(useTid);
		session.put(sessionKey,result);
		return result;
	}

	public static SapLUWHandle retrieveHandle(PipeLineSession session, String sessionKey) {
		return (SapLUWHandle)session.get(sessionKey);
	}

	public static SapLUWHandle retrieveHandle(PipeLineSession session, String sessionKey, boolean create, SapSystemImpl sapSystem, boolean useTid) throws JCoException {
		SapLUWHandle result=(SapLUWHandle)session.get(sessionKey);
		if (result==null && create) {
			return createHandle(session, sessionKey, sapSystem, useTid);
		}
		return result;
	}

	public static void releaseHandle(PipeLineSession session, String sessionKey) throws JCoException {
		SapLUWHandle handle=(SapLUWHandle)session.get(sessionKey);
		if (handle==null) {
			log.debug("no handle found under session key [{}]", sessionKey);
		} else {
			handle.release();
			session.remove(sessionKey);
		}
	}

	public void begin() throws JCoException {
		if (isUseTid()) {
			tid=destination.createTID();
			log.debug("begin: created SAP TID [{}]", tid);
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
			log.debug("commit: confirmed SAP TID [{}]", tid);
		} else {
			log.warn("Should Execute COMMIT by calling COMMIT BAPI");
		}
	}

	public void rollback() {
		log.debug("rollback: forget about SAP TID [{}], throw exception to signal SAP", tid);
		tid=null;
		throw new RollbackException();
	}

	public void release() throws JCoException {
		if (!isUseTid()) {
			if (!released) {
				// End the stateful connection
				JCoContext.end(destination);
				released = true;
				log.debug("release: stateful connection");
			}
		}
	}

	@Override
	public void close() throws PipeRunException {
		try {
			release();
		} catch (JCoException e) {
			throw new PipeRunException(null, "could not release handle", e);
		}
	}
}
