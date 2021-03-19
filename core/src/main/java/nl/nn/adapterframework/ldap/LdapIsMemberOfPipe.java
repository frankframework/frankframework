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
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
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
		
	private boolean recursiveSearch = true;
	private String groupDN;
	private final static String PARAM_TARGET_GROUP_DN = "groupDN";
	
	private String thenForwardName = "then";
	private String elseForwardName = "else";
	
	private String exceptionOnInput;
	private String thenOnInput;
	
	@Override
	public void configure() throws ConfigurationException {
		if (!ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader())) {
			if (StringUtils.isNotEmpty(getThenOnInput())) {
				throw new ConfigurationException("thenOnInput only allowed in stub mode");
			}
			if (StringUtils.isNotEmpty(getExceptionOnInput())) {
				throw new ConfigurationException("exceptionOnInput only allowed in stub mode");
			}
		}
		
		super.configure();
	}
	
	@Override
	public PipeRunResult doPipeWithException(Message message, IPipeLineSession session) throws PipeRunException {
		if (message==null) {
			throw new PipeRunException(this, getLogPrefix(session) + "input is null");
		}
		
		String searchedDN;
		try {
			searchedDN = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "Failure converting input to string", e);
		}
		
		if (StringUtils.isNotEmpty(getThenOnInput())) {
			if (getThenOnInput().equals(searchedDN)) {
				return new PipeRunResult(findForward(getThenForwardName()), message);
			} else {
				return new PipeRunResult(findForward(getElseForwardName()), message);
			}
		}
		if (StringUtils.isNotEmpty(getExceptionOnInput()) && getExceptionOnInput().equals(searchedDN)) {
			throw new PipeRunException(this, getLogPrefix(session)+"exceptionOnInput ["+getExceptionOnInput()+"]");
		}
		
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

		Set<String> memberships;
		try {
			if (isRecursiveSearch()) {
				memberships= ldapClient.searchRecursivelyViaAttributes(searchedDN, getBaseDN(), "memberOf", groupDN_work);
			} else {
				memberships= ldapClient.searchObjectForMultiValuedAttribute(searchedDN, getBaseDN(), "memberOf");
			}
			
			if (memberships.contains(groupDN_work)) {
				return new PipeRunResult(findForward(getThenForwardName()), message);
			}
			return new PipeRunResult(findForward(getElseForwardName()), message);
		} catch (NamingException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "exception on ldap lookup", e);
		}
	}
	
	/**
	 * When stubbing, create empty ldapClient to prevent NPEs
	 */
	@Override
	protected void createLdapClient() {
		if (StringUtils.isNotEmpty(getThenOnInput())) {
			ldapClient = new LdapClient();
		} else {
			super.createLdapClient();
		}
	}
	
	@IbisDoc({"1", "The dn of the membership to search for when the parameter '" + PARAM_TARGET_GROUP_DN + "' is not set", ""})
	public void setGroupDN(String string) {
		groupDN = string;
	}
	public String getGroupDN() {
		return groupDN;
	}
	
	@IbisDoc({"2", "If <code>true</code>, the memberOf attribute is also searched in all the found memberships", "true"})
	public void setRecursiveSearch(boolean b) {
		recursiveSearch = b;
	}
	public boolean isRecursiveSearch() {
		return recursiveSearch;
	}

	@IbisDoc({"3","Forward returned when <code>'true'</code>", "then"})
	public void setThenForwardName(String thenForwardName){
		this.thenForwardName = thenForwardName;
	}
	public String getThenForwardName(){
		return thenForwardName;
	}

	@IbisDoc({"4","Forward returned when <code>'false'</code>", "else"})
	public void setElseForwardName(String elseForwardName){
		this.elseForwardName = elseForwardName;
	}
	public String getElseForwardName(){
		return elseForwardName;
	}
	
	@IbisDoc({"5", "When not empty, the message is routed to <code>'then'</code> if input equals this value (for testing purposes only)", ""})
	public void setThenOnInput(String string) {
		thenOnInput = string;
	}
	public String getThenOnInput() {
		return thenOnInput;
	}

	@IbisDoc({"6", "When not empty, a piperunexception is thrown when the input equals this value (for testing purposes only)", ""})
	public void setExceptionOnInput(String string) {
		exceptionOnInput = string;
	}
	public String getExceptionOnInput() {
		return exceptionOnInput;
	}
}
