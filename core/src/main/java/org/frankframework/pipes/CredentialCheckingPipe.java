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
package org.frankframework.pipes;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;

/**
 * Pipe to check the the CredentialFactory (for testing only).
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class CredentialCheckingPipe extends FixedForwardPipe {

	private String targetUserid;
	private String targetPassword;
	private String defaultUserid;
	private String defaultPassword;
	private String authAlias;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getTargetUserid() == null) {
			throw new ConfigurationException("targetUserid must be specified");
		}
		if (getTargetPassword() == null) {
			throw new ConfigurationException("targetPassword must be specified");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getDefaultUserid(), getDefaultPassword());
		String result = "";

		if (!getTargetUserid().equals(cf.getUsername())) {
			result += "username does not match target";
		}
		if (!getTargetPassword().equals(cf.getPassword())) {
			if (!StringUtils.isEmpty(result)) {
				result += ", ";
			}
			result += "password does not match target";
		}
		if (StringUtils.isEmpty(result)) {
			result = "OK";
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	public void setTargetPassword(String string) {
		targetPassword = string;
	}
	public String getTargetPassword() {
		return targetPassword;
	}

	public void setTargetUserid(String string) {
		targetUserid = string;
	}
	public String getTargetUserid() {
		return targetUserid;
	}

	public void setDefaultPassword(String string) {
		defaultPassword = string;
	}
	public String getDefaultPassword() {
		return defaultPassword;
	}

	public void setDefaultUserid(String string) {
		defaultUserid = string;
	}
	public String getDefaultUserid() {
		return defaultUserid;
	}


}
