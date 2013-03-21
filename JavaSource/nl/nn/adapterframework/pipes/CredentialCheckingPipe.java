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
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.CredentialFactory;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe to check the the CredentialFactory (for testing only).
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public class CredentialCheckingPipe extends FixedForwardPipe {

	private String targetUserid;
	private String targetPassword;
	private String defaultUserid;
	private String defaultPassword;
	private String authAlias;

	public void configure() throws ConfigurationException {
		super.configure();
		if (getTargetUserid()==null) {
			throw new ConfigurationException("targetUserid must be specified");
		} 
		if (getTargetPassword()==null) {
			throw new ConfigurationException("targetPassword must be specified");
		} 
	}


	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		CredentialFactory cf=new CredentialFactory(getAuthAlias(),getDefaultUserid(),getDefaultPassword());
		String result="";
		if (!getTargetUserid().equals(cf.getUsername())) {
			result+="username ["+cf.getUsername()+"] does not match target ["+getTargetUserid()+"]";
		}
		if (!getTargetPassword().equals(cf.getPassword())) {
			result+="password ["+cf.getPassword()+"] does not match target ["+getTargetPassword()+"]";
		}
 		if (StringUtils.isEmpty(result)) {
 			result="OK";
 		}
 		return new PipeRunResult(getForward(),result);
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
