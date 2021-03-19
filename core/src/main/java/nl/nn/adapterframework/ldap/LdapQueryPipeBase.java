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
package nl.nn.adapterframework.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.cache.ICacheEnabled;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 * Base pipe for querying LDAP.
 * 
 * @author Gerrit van Brakel
 */
public abstract class LdapQueryPipeBase extends FixedForwardPipe implements ICacheEnabled<String,Set<String>> {
	
	private String ldapProviderURL;
	private String host;
	private int port = -1;
	private boolean useSsl = false;
	private String baseDN;

	private String authAlias;
	private String userName;
	private String password;

	private String exceptionForwardName = null;

	protected CredentialFactory cf;
	protected PipeForward exceptionForward;
	
	protected LdapClient ldapClient;
	protected ICacheAdapter<String, Set<String>> cache;

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
		cf = new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
		if (StringUtils.isNotEmpty(getExceptionForwardName())) {
			exceptionForward = findForward(getExceptionForwardName());
		}
		
		createLdapClient();
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		try {
			ldapClient.open();
		} catch (SenderException e) {
			throw new PipeStartException(e);
		}
	}

	@Override
	public void stop() {
		try {
			ldapClient.close();
		} catch (SenderException e) {
			log.warn(getLogPrefix(null)+"cannot close ldapClient",e);
		} finally {
			super.stop();
		}
	}
	
	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		if (exceptionForward != null) {
			try {
				return doPipeWithException(message, session);
			} catch (Throwable t) {
				log.warn(getLogPrefix(session) + "exception occured, forwarding to exception-forward [" + exceptionForward.getPath() +"], exception:\n", t);
				return new PipeRunResult(exceptionForward, message);
			}
		} else {
			return doPipeWithException(message, session);
		}
	}

	public abstract PipeRunResult doPipeWithException(Message message, IPipeLineSession session) throws PipeRunException;

	protected void createLdapClient() {
		Map<String,Object> options = new HashMap<String,Object>();
		options.put("java.naming.provider.url",retrieveUrl(getHost(), getPort(), getBaseDN(), isUseSsl()));
		options.put(Context.SECURITY_AUTHENTICATION, "simple");
		options.put(Context.SECURITY_PRINCIPAL, cf.getUsername());
		options.put(Context.SECURITY_CREDENTIALS, cf.getPassword());
		ldapClient= new LdapClient(options);
	}
	
	protected String retrieveUrl(String host, int port, String baseDN, boolean useSsl) {
		String url; 
		if (StringUtils.isNotEmpty(getLdapProviderURL())) {
			url=getLdapProviderURL();
		} else {
			String s = useSsl ? "ldaps://" : "ldap://";
			String h = (host != null) ? host : "";
			String p = (port != -1) ? (":" + port) : "";
			url=s + h + p;
		}
		String d = (baseDN != null) ? ("/" + baseDN.replaceAll("\\s", "%20")) : "";
		return url + d;
	}

	@Override
	public void setCache(ICacheAdapter<String, Set<String>> cache) {
		this.cache=cache;
	}
	@Override
	public ICacheAdapter<String, Set<String>> getCache() {
		return cache;
	}
	
	@IbisDoc({"1", "Url to context to search in, e.g. 'ldaps://insim.biz'.", ""})
	public void setLdapProviderURL(String string) {
		ldapProviderURL = string;
	}
	public String getLdapProviderURL() {
		return ldapProviderURL;
	}

	@IbisDoc({"2", "Host part of ldapProviderUrl. Only used when ldapProviderUrl not specified", ""})
	public void setHost(String string) {
		host = string;
	}
	public String getHost() {
		return host;
	}

	@IbisDoc({"3", "Port of ldapProviderUrl. Only used when ldapProviderUrl not specified", ""})
	public void setPort(int i) {
		port = i;
	}
	public int getPort() {
		return port;
	}

	@IbisDoc({"4", "Indication to use ldap or ldaps in ldapProviderUrl. Only used when ldapProviderUrl not specified", "false"})
	public void setUseSsl(boolean b) {
		useSsl = b;
	}
	public boolean isUseSsl() {
		return useSsl;
	}

	@IbisDoc({"5", "BaseDN, e.g. OU=Tenants,DC=INSIM,DC=BIZ", "false"})
	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}
	public String getBaseDN() {
		return baseDN;
	}



	@IbisDoc({"6", "Alias used to obtain credentials to connect to ldap server", ""})
	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"7", "Username used to obtain credentials to connect to ldap server", ""})
	public void setUserName(String string) {
		userName = string;
	}
	public String getUserName() {
		return userName;
	}

	@IbisDoc({"8", "Password used to obtain credentials to connect to ldap server", ""})
	public void setPassword(String string) {
		password = string;
	}
	public String getPassword() {
		return password;
	}

	@IbisDoc({"9", "PipeForward used when an exception is caught", ""})
	public void setExceptionForwardName(String string) {
		exceptionForwardName = string;
	}
	public String getExceptionForwardName() {
		return exceptionForwardName;
	}
}
