/*
   Copyright 2019, 2020 Integration Partners

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

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Forward;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;

/**
 * Base pipe for querying LDAP.
 *
 * @author Gerrit van Brakel
 */
@Forward(name = "*", description = "When {@literal notFoundForwardName} or {@literal exceptionForwardName} is used")
public abstract class AbstractLdapQueryPipe extends FixedForwardPipe {

	private @Getter String ldapProviderURL;
	private @Getter String host;
	private @Getter int port = -1;
	private @Getter boolean useSsl = false;
	private @Getter String baseDN;

	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;

	private @Getter String notFoundForwardName = "notFound";
	private @Getter String exceptionForwardName = null;

	protected CredentialFactory cf;
	protected PipeForward notFoundForward;
	protected PipeForward exceptionForward;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getLdapProviderURL())) {
			if (StringUtils.isNotEmpty(getHost()) || getPort()>0) {
				throw new ConfigurationException("attributes 'host', 'port' and 'useSsl' cannot be used together with ldapProviderUrl");
			}
		} else {
			if (StringUtils.isEmpty(getHost())) {
				throw new ConfigurationException("either 'ldapProviderUrl' or 'host' (and possibly 'port' and 'useSsl') must be specified");
			}
		}
		cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(getNotFoundForwardName())) {
			notFoundForward = findForward(getNotFoundForwardName());
		}
		if (StringUtils.isNotEmpty(getExceptionForwardName())) {
			exceptionForward = findForward(getExceptionForwardName());
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (exceptionForward != null) {
			try {
				return doPipeWithException(message, session);
			} catch (Throwable t) {
				log.warn("exception occurred, forwarding to exception-forward [{}]", exceptionForward.getPath(), t);
				return new PipeRunResult(exceptionForward, message);
			}
		}
		return doPipeWithException(message, session);
	}

	public abstract PipeRunResult doPipeWithException(Message message, PipeLineSession session) throws PipeRunException;

	protected String retrieveUrl(String host, int port, String baseDN, boolean useSsl) {
		String url;
		if (StringUtils.isNotEmpty(getLdapProviderURL())) {
			url=getLdapProviderURL();
		} else {
			String s = useSsl ? "ldaps://" : "ldap://";
			String h = host != null ? host : "";
			String p = port != -1 ? (":" + port) : "";
			url=s + h + p;
		}
		String d = baseDN != null ? ("/" + baseDN.replaceAll("\\s", "%20")) : "";
		return url + d;
	}

	/** Url to context to search in, e.g. 'ldaps://DOMAIN.EXT'. */
	public void setLdapProviderURL(String string) {
		ldapProviderURL = string;
	}

	/** Host part of ldapProviderUrl. Only used when ldapProviderUrl not specified */
	public void setHost(String string) {
		host = string;
	}

	/** Port of ldapProviderUrl. Only used when ldapProviderUrl not specified */
	public void setPort(int i) {
		port = i;
	}

	/**
	 * Indication to use ldap or ldaps in ldapProviderUrl. Only used when ldapProviderUrl not specified
	 * @ff.default false
	 */
	public void setUseSsl(boolean b) {
		useSsl = b;
	}

	/**
	 * BaseDN, e.g. CN=USERS,DC=DOMAIN,DC=EXT
	 * @ff.default false
	 */
	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}

	/** Alias used to obtain credentials to connect to ldap server */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	/** Username used to obtain credentials to connect to ldap server */
	public void setUsername(String string) {
		username = string;
	}

	/** Password used to obtain credentials to connect to ldap server */
	public void setPassword(String string) {
		password = string;
	}

	public void setNotFoundForwardName(String string) {
		notFoundForwardName = string;
	}

	/** PipeForward used when an exception is caught */
	public void setExceptionForwardName(String string) {
		exceptionForwardName = string;
	}

}
