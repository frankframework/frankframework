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
package org.frankframework.ldap;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IPipe;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.ldap.LdapSender.Operation;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;

/**
 * Pipe to check if a username and password are valid in LDAP.
 *
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>ldapProviderURL</td><td>URL to the LDAP server. <br/>Example: ldap://su05b9.itc.intranet</td><td>Required only if attribute ldapProviderURL is not set</td></tr>
 * <tr><td>principal</td><td>The LDAP DN for the username. <br/>Example: UID=SRP,OU=DI-IUF-EP,OU=SERVICES,O=ING</td><td>Required and must be filled</td></tr>
 * <tr><td>credentials</td><td>The LDAP password. <br/> Example: welkom01</td><td>Required and must be filled</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th><th>remarks</th></tr>
 * <tr><td>success</td><td>Successful login to LDAP</td><td>should be defined in configuration</td></tr>
 * <tr><td>invalid</td><td>Unsuccessful login to LDAP</td><td>should be defined in configuration</td></tr>
 * </table>
 * </p>
 *
 * @deprecated
 * @author  Milan Tomc
 */
@Deprecated(forRemoval = true, since = "7.6.0")
@ConfigurationWarning("please use LdapSender with operation challenge and check for returned message <LdapResult>Success</LdapResult>")
public class LdapChallengePipe extends FixedForwardPipe {

	private String ldapProviderURL=null;
	private String initialContextFactoryName=null;
	private String errorSessionKey=null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(ldapProviderURL) && !getParameterList().hasParameter("ldapProviderURL")) {
			throw new ConfigurationException("ldapProviderURL must be specified, either as attribute or as parameter");
		}
		if (StringUtils.isNotEmpty(ldapProviderURL) && getParameterList().hasParameter("ldapProviderURL")) {
			throw new ConfigurationException("ldapProviderURL can only be specified once, either as attribute or as parameter");
		}
		if (!getParameterList().hasParameter("principal")) {
			throw new ConfigurationException("Parameter 'principal' must be specified");
		}
		if (!getParameterList().hasParameter("credentials")) {
			throw new ConfigurationException("Parameter 'credentials' must be specified");
		}
	}

	/**
	 * Checks to see if the supplied parameteres of the pipe can login to LDAP
	 * @see IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message msg, PipeLineSession pls) throws PipeRunException {

		LdapSender ldapSender = new LdapSender();

		String ldapProviderURL;
		String credentials;
		String principal;

		try {
			ParameterValueList pvl = getParameterList().getValues(msg, pls);

			if (StringUtils.isNotEmpty(getLdapProviderURL())) {
				ldapProviderURL = getLdapProviderURL();
			} else {
				ldapProviderURL = pvl.get("ldapProviderURL").asStringValue();
			}
			credentials = pvl.get("credentials").asStringValue();
			principal = pvl.get("principal").asStringValue();
		} catch (ParameterException e) {
			throw new PipeRunException(this, "Invalid parameter", e);
		}

		ldapSender.setErrorSessionKey(getErrorSessionKey());
		if (StringUtils.isEmpty(ldapProviderURL)) {
			throw new PipeRunException(this, "ldapProviderURL is empty");
		}
		if (StringUtils.isEmpty(principal)) {
//			throw new PipeRunException(this, "principal is empty");
			handleError(ldapSender,pls,34,"Principal is Empty");
			return new PipeRunResult(findForward("invalid"), msg);
		}
		if (StringUtils.isEmpty(credentials)) {
//			throw new PipeRunException(this, "credentials are empty");
			handleError(ldapSender,pls,49,"Credentials are Empty");
			return new PipeRunResult(findForward("invalid"), msg);
		}

		ldapSender.addParameter(new Parameter("entryName", principal));

		ldapSender.setUsePooling(false);
		ldapSender.setLdapProviderURL(ldapProviderURL);
		if (StringUtils.isNotEmpty(getInitialContextFactoryName())) {
			ldapSender.setInitialContextFactoryName(getInitialContextFactoryName());
		}
		ldapSender.setPrincipal(principal);
		ldapSender.setCredentials(credentials);
		ldapSender.setOperation(Operation.READ);
		try {
			log.debug("Looking up context for principal [{}]", principal);
			ldapSender.configure();
			log.debug("Successfully looked up context for principal [{}]", principal);
		} catch (Exception e) {
			if (StringUtils.isNotEmpty(getErrorSessionKey())) {
				ldapSender.storeLdapException(e, pls);
			} else {
				log.warn("LDAP error looking up context for principal [{}]", principal, e);
			}
			return new PipeRunResult(findForward("invalid"), msg);
		}

		return new PipeRunResult(getSuccessForward(), msg);
	}

	protected void handleError(LdapSender ldapSender, PipeLineSession session, int code, String message) {
		Throwable t = new ConfigurationException(LdapSender.LDAP_ERROR_MAGIC_STRING+code+"-"+message+"]");
		ldapSender.storeLdapException(t, session);
	}

	/** url to the ldap server. <br/>example: ldap://su05b9.itc.intranet */
	public void setLdapProviderURL(String string) {
		ldapProviderURL = string;
	}
	public String getLdapProviderURL() {
		return ldapProviderURL;
	}

	/**
	 * class to use as initial context factory
	 * @ff.default com.sun.jndi.ldap.ldapctxfactory
	 */
	public void setInitialContextFactoryName(String value) {
		initialContextFactoryName = value;
	}
	public String getInitialContextFactoryName() {
		return initialContextFactoryName;
	}

	/** key of session variable used to store cause of errors */
	public void setErrorSessionKey(String string) {
		errorSessionKey = string;
	}
	public String getErrorSessionKey() {
		return errorSessionKey;
	}

}
