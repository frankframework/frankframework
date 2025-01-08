/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2021 WeareFrank!

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
package org.frankframework.jndi;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.jms.JmsRealm;
import org.frankframework.statistics.HasApplicationContext;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;

/**
 * Provides all JNDI functions and is meant to act as a base class.
 *
 * <br/>
 * @author Johan Verrips IOS
 */
public class JndiBase implements IConfigurable, HasApplicationContext {
	protected Logger log = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;
	// JNDI
	private @Getter String providerURL = null;
	private @Getter String initialContextFactoryName = null;
	private @Getter String authentication = null;
	private @Getter String principal = null;
	private @Getter String credentials = null;
	private @Getter String jndiAuthAlias = null;
	private @Getter String jmsRealmName = null;
	private @Getter String urlPkgPrefixes = null;
	private @Getter String securityProtocol = null;
	private @Getter String jndiContextPrefix = "";
	private @Getter String jndiProperties = null;

	private Context context = null;

	@Override
	public void configure() throws ConfigurationException {
		// somewhere a sender is being initialized without setting the property
		// TODO get rid of the workaround and find out why spring does not set the prefix
		if (StringUtils.isEmpty(jndiContextPrefix)) {
			jndiContextPrefix = AppConstants.getInstance(configurationClassLoader).getString("jndiContextPrefix","");
		}
	}

	public void stop() {
		if (null != context) {
			log.debug("closing JNDI-context");
			try {
				context.close();
			} catch (NamingException e) {
				log.warn("could not close JNDI-context", e);
			} finally {
				context = null;
			}
		}
	}

	public Properties getJndiEnv() throws NamingException {
		Properties jndiEnv = new Properties();

		if (StringUtils.isNotEmpty(getJndiProperties())) {
			URL url = ClassLoaderUtils.getResourceURL(this, getJndiProperties());
			if (url==null) {
				throw new NamingException("cannot find jndiProperties from ["+getJndiProperties()+"]");
			}
			try {
				jndiEnv.load(url.openStream());
			} catch (IOException e) {
				throw new NamingException("cannot load jndiProperties ["+getJndiProperties()+"] from url ["+ url +"]");
			}
		}
		if (StringUtils.isNotEmpty(getInitialContextFactoryName())) {
			jndiEnv.put(Context.INITIAL_CONTEXT_FACTORY, getInitialContextFactoryName());
		}
		if (StringUtils.isNotEmpty(getProviderURL())) {
			jndiEnv.put(Context.PROVIDER_URL, getProviderURL());
		}
		if (StringUtils.isNotEmpty(getAuthentication())) {
			jndiEnv.put(Context.SECURITY_AUTHENTICATION, getAuthentication());
		}
		if (StringUtils.isNotEmpty(getPrincipal()) || StringUtils.isNotEmpty(getCredentials()) || StringUtils.isNotEmpty(getJndiAuthAlias())) {
			CredentialFactory jndiCf = new CredentialFactory(getJndiAuthAlias(), getPrincipal(), getCredentials());
			if (StringUtils.isNotEmpty(jndiCf.getUsername()))
				jndiEnv.put(Context.SECURITY_PRINCIPAL, jndiCf.getUsername());
			if (StringUtils.isNotEmpty(jndiCf.getPassword()))
				jndiEnv.put(Context.SECURITY_CREDENTIALS, jndiCf.getPassword());
		}
		if (StringUtils.isNotEmpty(getUrlPkgPrefixes())) {
			jndiEnv.put(Context.URL_PKG_PREFIXES, getUrlPkgPrefixes());
		}
		if (StringUtils.isNotEmpty(getSecurityProtocol())) {
			jndiEnv.put(Context.SECURITY_PROTOCOL, getSecurityProtocol());
		}

		if (log.isDebugEnabled()) {
			for(Iterator it=jndiEnv.keySet().iterator(); it.hasNext();) {
				String key=(String) it.next();
				String value=jndiEnv.getProperty(key);
				log.debug("jndiEnv [{}] = [{}]", key, value);
			}
		}
		return jndiEnv;
	}

	/**
	 * When InitialContextFactory and ProviderURL are set, these are used to get the
	 * <code>Context</code>. Otherwise, the InitialContext is retrieved without
	 * parameters.<br/>
	 * <b>Notice:</b> you can set the parameters on the commandline with <br/>
	 * java -Djava.naming.factory.initial=xxx -Djava.naming.provider.url=xxx
	 */
	public Context getContext() throws NamingException {
		if (null == context) {
			Properties jndiEnv = getJndiEnv();
			if (!jndiEnv.isEmpty()) {
				log.debug("creating initial JNDI-context using specified environment");
				context = new InitialContext(jndiEnv);
			} else {
				log.debug("creating initial JNDI-context");
				context = new InitialContext();
			}
		}
		return context;
	}

	/** maps to the field context.security_authentication */
	public void setAuthentication(String newAuthentication) {
		authentication = newAuthentication;
	}

	/** username to connect to context, maps to context.security_credentials */
	public void setCredentials(String newCredentials) {
		credentials = newCredentials;
	}

	/**
	 * Sets the value of initialContextFactoryName
	 */
	/** class to use as initial context factory */
	public void setInitialContextFactoryName(String value) {
		initialContextFactoryName = value;
	}

	/**
	 * Sets the value of providerURL
	 */
	public void setProviderURL(String value) {
		providerURL = value;
	}

	/** maps to the field context.security_protocol */
	public void setSecurityProtocol(String securityProtocol) {
		this.securityProtocol = securityProtocol;
	}

	/**
	 * Setter for <code>Context.URL_PKG_PREFIXES</code><br/>
	 * Creation date: (03-04-2003 8:50:36)
	 */
	/** maps to the field context.url_pkg_prefixes */
	public void setUrlPkgPrefixes(String newUrlPkgPrefixes) {
		urlPkgPrefixes = newUrlPkgPrefixes;
	}

	@Override
	public String toString() {
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("context", context);
		ts.append("authentication", authentication);
		ts.append("credentials", credentials);
		ts.append("providerURL", providerURL);
		ts.append("urlPkgPrefixes", urlPkgPrefixes);
		ts.append("securityProtocol", securityProtocol);
		ts.append("initialContextFactoryName", initialContextFactoryName);

		return ts.toString();
	}

	/**
	 * loads JNDI (and other) properties from a JmsRealm
	 * @see JmsRealm
	 */
	public void setJmsRealm(String jmsRealmName) {
		try {
			JmsRealm.copyRealm(this, jmsRealmName);
			this.jmsRealmName = jmsRealmName;
		} catch (ConfigurationException e) {
			log.error("cannot copy data from realm", e);
		}
	}


	/** username to connect to context, maps to context.security_principal */
	public void setPrincipal(String string) {
		principal = string;
	}

	/** authentication alias, may be used to override principal and credential-settings */
	public void setJndiAuthAlias(String string) {
		jndiAuthAlias = string;
	}

	public void setJndiContextPrefix(String string) {
		jndiContextPrefix = string;
	}

	public void setJndiProperties(String jndiProperties) {
		this.jndiProperties = jndiProperties;
	}

	/** Name of the sender or the listener */
	@Override
	public void setName(String name) {
		this.name = name;
	}
}
