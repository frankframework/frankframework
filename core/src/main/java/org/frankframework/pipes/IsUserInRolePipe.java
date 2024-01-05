/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.stream.Message;
import org.frankframework.util.StringUtil;

import lombok.Getter;

/**
 * Pipe that checks if the calling user has a specified role.
 * Uses the PipeLineSessions methods.
 * <p>
 * If the role is not specified by the role attribute, the input of
 * the pipe is used as role.
 *
 * N.B. The role itself must be specified by hand in the deployment descriptors web.xml and application.xml.
 * </p>
 * @ff.forward notInRole user may not assume role
 *
 * @author  Gerrit van Brakel
 * @since   4.4.3
 */
@Category("Advanced")
@ElementType(ElementTypes.ROUTER)
public class IsUserInRolePipe extends FixedForwardPipe {

	private String role=null;
	private @Getter String notInRoleForwardName="notInRole";
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

	protected void assertUserIsInAnyRole(PipeLineSession session, List<String> roles) throws SecurityException {
		if (!session.isUserInAnyRole(roles)) {
			throw new SecurityException("User not in any of the provided roles");
		}
	}

	protected String inWhichRoleIsUser(PipeLineSession session, String roles) {
		List<String> splitRoles = StringUtil.split(roles);
		return session.inWhichRoleIsUser(splitRoles);
	}

	protected String getRolesToCheck(Message message) throws PipeRunException {
		if (StringUtils.isEmpty(getRole())) {
			try {
				return message.asString();
			} catch (IOException e) {
				throw new PipeRunException(this, "cannot open stream", e);
			}
		}
		return getRole();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			String rolesToCheck = getRolesToCheck(message);
			if (StringUtils.isEmpty(rolesToCheck)) {
				throw new PipeRunException(this, "role cannot be empty");
			}

			String userRole = inWhichRoleIsUser(session, rolesToCheck);
			if (userRole == null) {
				throw new SecurityException("user is not in role(s) [" + rolesToCheck + "]");
			}

			PipeForward relatedForward = findForward(userRole);
			if (relatedForward != null) {
				return new PipeRunResult(relatedForward, message);
			}
		} catch (SecurityException e) {
			if (notInRoleForward!=null) {
				return new PipeRunResult(notInRoleForward, message);
			}
			throw new PipeRunException(this,"",e);
		}
		return new PipeRunResult(getSuccessForward(),message);
	}

	public String getRole() {
		return role;
	}

	/** the j2ee role to check.  */
	public void setRole(String string) {
		role = string;
	}

	@Deprecated
	/**
	 * name of forward returned if user is not allowed to assume the specified role
	 * @ff.default notInRole
	 */
	public void setNotInRoleForwardName(String string) {
		notInRoleForwardName = string;
	}

}
