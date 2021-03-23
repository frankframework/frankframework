/*
   Copyright 2019, 2020 WeAreFrank!

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
package nl.nn.adapterframework.ldap;

import java.io.IOException;

import javax.naming.NamingException;

import lombok.Getter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Selects an exitState, based on if the input DN is a member of another DN

 * 
 * @author Ricardo van Holst
 */
public class LdapIsMemberOfPipe extends LdapQueryPipeBase {
		
	private @Getter boolean recursiveSearch = true;
	private @Getter String groupDN;
	private final static String PARAM_TARGET_GROUP_DN = "groupDN";
	
	private @Getter String thenForwardName = "then";
	private @Getter String elseForwardName = "else";
	
	@Override
	public PipeRunResult doPipeWithException(Message message, IPipeLineSession session) throws PipeRunException {		
		String groupDN_work;
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception on extracting parameters", e);
			}
		}
		groupDN_work = getParameterValue(pvl, PARAM_TARGET_GROUP_DN);
		if (groupDN_work == null) {
			groupDN_work = getGroupDN();
		}
		
		if (message==null) {
			throw new PipeRunException(this, getLogPrefix(session) + "input is null");
		}
		
		String searchedDN;
		try {
			searchedDN = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "Failure converting input to string", e);
		}

		try {
			if (ldapClient.searchViaAttributesForTargetValue(searchedDN, getBaseDN(), "memberOf", groupDN_work, isRecursiveSearch())) {
				return new PipeRunResult(findForward(getThenForwardName()), message);
			} else {
				return new PipeRunResult(findForward(getElseForwardName()), message);
			}
		} catch (NamingException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "exception on ldap lookup", e);
		}
	}
	
	@IbisDoc({"1", "The dn of the membership to search for when the parameter '" + PARAM_TARGET_GROUP_DN + "' is not set", ""})
	public void setGroupDN(String string) {
		groupDN = string;
	}
	
	@IbisDoc({"2", "If <code>true</code>, the memberOf attribute is also searched in all the found memberships", "true"})
	public void setRecursiveSearch(boolean b) {
		recursiveSearch = b;
	}

	@IbisDoc({"3","Forward returned when <code>'true'</code>", "then"})
	public void setThenForwardName(String thenForwardName){
		this.thenForwardName = thenForwardName;
	}

	@IbisDoc({"4","Forward returned when <code>'false'</code>", "else"})
	public void setElseForwardName(String elseForwardName){
		this.elseForwardName = elseForwardName;
	}
}
