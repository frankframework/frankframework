/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;

/**
 * Pipe that checks if the calling user has a specified role. 
 * Uses the PipeLineSessions methods.
 * <p>
 * If the role is not specified by the role attribute, the input of
 * the pipe is used as role.
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>user may assume role</td></tr>
 * <tr><td>"notInRole" or value set by {@link #setNotInRoleForwardName(String) notInRoleForwardName}</td><td>user may not assume role</td></tr>
 * <tr><td><i></i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * N.B. The role itself must be specified by hand in the deployement descriptors web.xml and application.xml.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.3
 */
public class IsUserInRolePipe extends FixedForwardPipe {

	private String role=null;
	private String notInRoleForwardName="notInRole";
	protected PipeForward notInRoleForward;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getNotInRoleForwardName())) {
			notInRoleForward = findForward(getNotInRoleForwardName());
			if (notInRoleForward==null) {
				throw new ConfigurationException("notInRoleForwardName ["+getNotInRoleForwardName()+"] not found");
			}
		}
	}
	
	protected void assertUserIsInRole(PipeLineSession session, String role) throws SecurityException {
		if (!session.isUserInRole(role)) {
			throw new SecurityException(getLogPrefix(session)+"user is not in role ["+role+"]");
		}
	}
	
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			if (StringUtils.isEmpty(getRole())) {
				String inputString;
				try {
					inputString = message.asString();
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
				}
				if (StringUtils.isEmpty(inputString)) {
					throw new PipeRunException(this, "role cannot be empty");
				}
				assertUserIsInRole(session, inputString);
			} else {
				assertUserIsInRole(session, getRole());
			}
		} catch (SecurityException e) {
			if (notInRoleForward!=null) {
				return new PipeRunResult(notInRoleForward, message);
			} else {
				throw new PipeRunException(this,"",e);
			}
		}
		return new PipeRunResult(getForward(),message);
	}
	
	public String getRole() {
		return role;
	}

	@IbisDoc({"the j2ee role to check. ", ""})
	public void setRole(String string) {
		role = string;
	}

	public String getNotInRoleForwardName() {
		return notInRoleForwardName;
	}

	@IbisDoc({"name of forward returned if user is not allowed to assume the specified role", "notInRole"})
	public void setNotInRoleForwardName(String string) {
		notInRoleForwardName = string;
	}

}