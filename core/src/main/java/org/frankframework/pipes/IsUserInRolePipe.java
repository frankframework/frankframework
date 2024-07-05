/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2024 WeAreFrank!

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
import org.frankframework.core.ISecurityHandler;
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
				throw new ConfigurationException("could not find forward for notInRoleForwardName ["+ getNotInRoleForwardName()+"]");
			}
		}
	}

	protected String getFirstMatchingUserRole(PipeLineSession session, List<String> roles) {
		ISecurityHandler securityHandler = session.getSecurityHandler();
		return roles.stream()
				.filter(securityHandler::isUserInRole)
				.findFirst().orElse(null);
	}

	protected List<String> getRolesToCheck(Message message) throws PipeRunException {
		String roles = getRole();
		if (StringUtils.isEmpty(roles)) {
			try {
				roles = message.asString();
			} catch (IOException e) {
				throw new PipeRunException(this, "cannot open stream", e);
			}
		}
		return StringUtil.split(roles);
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			List<String> rolesToCheck = getRolesToCheck(message);
			if (rolesToCheck.isEmpty()) {
				throw new PipeRunException(this, "role cannot be empty");
			}

			String userRole = getFirstMatchingUserRole(session, rolesToCheck);
			if (userRole == null) {
				throw new SecurityException("user is not in role(s) [" + rolesToCheck + "]");
			}

			PipeForward relatedForward = findForward(userRole);
			if (relatedForward != null) {
				return new PipeRunResult(relatedForward, message);
			}
		} catch (SecurityException e) {
			return new PipeRunResult(notInRoleForward, message);
		}
		return new PipeRunResult(getSuccessForward(),message);
	}

	public String getRole() {
		return role;
	}

	/** the j2ee role(s) to check, if the user in multiple roles, the first specified role will be matched. */
	public void setRole(String string) {
		role = string;
	}

	/**
	 * name of forward returned if user is not allowed to assume the specified role
	 * @ff.default notInRole
	 */
	@Deprecated(forRemoval = true, since = "7.7.0")
	public void setNotInRoleForwardName(String string) {
		notInRoleForwardName = string;
	}

}
