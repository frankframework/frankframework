/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import com.sap.conn.jco.JCoException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.extensions.sap.SapException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;

/**
 * Manager for SAP Logical Units of Work (LUWs).
 * Used to begin, commit or rollback LUWs. A SapLUWManager can be placed before a number
 * of SapSenders. The SapLUWManager and the SapSenders must each use the same value for
 * luwHandleSessionKey. By doing so, they use the same connection to SAP. This allows to
 * perform a commit on a number of actions.<br/>
 * The placement of the the first SapLUWManager is optionan: By specifying a new
 * luwHandleSessionKey a new handle is created implicitly.<br/>
 * To explicityly commit or rollback a set of actions, a SapLUWManager-pipe can be used, with
 * the action-attribute set apropriately.
 *
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public class SapLUWManager extends FixedForwardPipe {

	public static final String ACTION_BEGIN="begin";
	public static final String ACTION_COMMIT="commit";
	public static final String ACTION_ROLLBACK="rollback";
	public static final String ACTION_RELEASE="release";

	private @Getter String luwHandleSessionKey;
	private @Getter String action;
	private @Getter String sapSystemName;

	private @Getter SapSystemImpl sapSystem;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getAction())) {
			throw new ConfigurationException("action should be specified, it must be one of: "+
				ACTION_BEGIN+", "+ACTION_COMMIT+", "+ACTION_ROLLBACK+", "+ACTION_RELEASE+".");
		}
		if (!getAction().equalsIgnoreCase(ACTION_BEGIN) &&
			!getAction().equalsIgnoreCase(ACTION_COMMIT) &&
			!getAction().equalsIgnoreCase(ACTION_ROLLBACK) &&
			!getAction().equalsIgnoreCase(ACTION_RELEASE)) {
			throw new ConfigurationException("illegal action ["+getAction()+"] specified, it must be one of: "+
				ACTION_BEGIN+", "+ACTION_COMMIT+", "+ACTION_ROLLBACK+", "+ACTION_RELEASE+".");
		}
		if (StringUtils.isEmpty(getLuwHandleSessionKey())) {
			throw new ConfigurationException("action should be specified, it must be one of: "+
				ACTION_BEGIN+", "+ACTION_COMMIT+", "+ACTION_ROLLBACK+", "+ACTION_RELEASE+".");
		}
		sapSystem=SapSystemImpl.getSystem(getSapSystemName());
		if (sapSystem==null) {
			throw new ConfigurationException("cannot find SapSystem ["+getSapSystemName()+"]");
		}
	}

	@Override
	public void start() {
		try {
			sapSystem.openSystem();
		} catch (SapException e) {
			stop();
			throw new LifecycleException("exception starting SapSender", e);
		}
	}

	@Override
	public void stop() {
		sapSystem.closeSystem();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (getAction().equalsIgnoreCase(ACTION_BEGIN)) {
			try {
				SapLUWHandle.retrieveHandle(session,getLuwHandleSessionKey(),true,getSapSystem(),false).begin();
			} catch (JCoException e) {
				throw new PipeRunException(this, "begin: could not retrieve handle", e);
			}
		} else if (getAction().equalsIgnoreCase(ACTION_COMMIT)) {
			SapLUWHandle handle=SapLUWHandle.retrieveHandle(session,getLuwHandleSessionKey());
			if (handle==null) {
				throw new PipeRunException(this, "commit: cannot find handle under sessionKey ["+getLuwHandleSessionKey()+"]");
			}
			try {
				handle.commit();
			} catch (JCoException e) {
				throw new PipeRunException(this, "commit: could not commit handle", e);
			}
		} else if (getAction().equalsIgnoreCase(ACTION_ROLLBACK)) {
			SapLUWHandle handle=SapLUWHandle.retrieveHandle(session,getLuwHandleSessionKey());
			if (handle==null) {
				throw new PipeRunException(this, "rollback: cannot find handle under sessionKey ["+getLuwHandleSessionKey()+"]");
			}
			handle.rollback();
		} else if (getAction().equalsIgnoreCase(ACTION_RELEASE)) {
			try {
				SapLUWHandle.releaseHandle(session,getLuwHandleSessionKey());
			} catch (JCoException e) {
				throw new PipeRunException(this, "release: could not release handle", e);
			}
		}
		return new PipeRunResult(getSuccessForward(),message);
	}

	/** Name of the SapSystem used by this object */
	public void setSapSystemName(String string) {
		sapSystemName = string;
	}

	/** One of: begin, commit, rollback, release */
	public void setAction(String string) {
		action = string;
	}

	/** Session key under which information is stored */
	public void setLuwHandleSessionKey(String string) {
		luwHandleSessionKey = string;
	}
}
