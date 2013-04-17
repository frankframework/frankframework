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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe that checks if the calling user has a specified role. 
 * Uses the PipeLineSessions methods.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned if user has role</td><td>"success"</td></tr>
 * <tr><td>{@link #setNotInRoleForwardName(String) notInRoleForwardName}</td><td>name of forward returned if user is not allowed to assume the specified role</td><td>"notInRole"</td></tr>
 * <tr><td>{@link #setRole(String) role}</td><td>the J2EE role to check. </td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success" or value set by {@link #setForwardName(String) forwardName}</td><td>user may assume role</td></tr>
 * <tr><td>"notInRole" or value set by {@link #setNotInRoleForwardName(String) notInRoleForwardName}</td><td>user may not assume role</td></tr>
 * <tr><td><i></i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * N.B. The role itself must be specified by hand in the deployement descriptors web.xml and application.xml.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.3
 * @version $Id$
 */
public class IsUserInRolePipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: IsUserInRolePipe.java,v $ $Revision: 1.6 $ $Date: 2012-06-01 10:52:49 $";

	private String role=null;
	private String notInRoleForwardName="notInRole";
	protected PipeForward notInRoleForward;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getRole())) {
			throw new ConfigurationException("must specify attribute role");
		}
		if (StringUtils.isNotEmpty(getNotInRoleForwardName())) {
			notInRoleForward = findForward(getNotInRoleForwardName());
			if  (notInRoleForward==null) {
				throw new ConfigurationException("notInRoleForwardName ["+getNotInRoleForwardName()+"] not found");
			}
		}
	}
	
	protected void assertUserIsInRole(IPipeLineSession session) throws SecurityException {
		if (!session.isUserInRole(getRole())) {
			throw new SecurityException(getLogPrefix(session)+"user is not in role ["+getRole()+"]");
		}
	}
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			assertUserIsInRole(session);
		} catch (SecurityException e) {
			if (notInRoleForward!=null) {
				return new PipeRunResult(notInRoleForward, input);
			} else {
				throw new PipeRunException(this,"",e);
			}
		}
		return new PipeRunResult(getForward(),input);
	}
	
	public String getRole() {
		return role;
	}
	public void setRole(String string) {
		role = string;
	}

	public String getNotInRoleForwardName() {
		return notInRoleForwardName;
	}
	public void setNotInRoleForwardName(String string) {
		notInRoleForwardName = string;
	}

}
