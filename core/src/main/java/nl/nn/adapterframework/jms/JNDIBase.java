/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.jms;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;

/**
 * Provides all JNDI functions and is meant to act as a base class.
 * 
 * <br/>
 * @author Johan Verrips IOS
 */
public class JNDIBase {
	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // JNDI
    private String providerURL = null;
    private String initialContextFactoryName = null;
    private String authentication = null;
	private String principal = null;
    private String credentials = null;
	private String jndiAuthAlias = null;
    private String jmsRealmName = null;
    private String urlPkgPrefixes = null;
    private String securityProtocol = null;
	private String jndiContextPrefix = "";
	private String jndiProperties = null;

	private Context context = null;

	public void close() {
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

	protected Hashtable getJndiEnv() throws NamingException {
		Properties jndiEnv = new Properties();

		if (StringUtils.isNotEmpty(getJndiProperties())) {
			URL url = ClassUtils.getResourceURL(classLoader, getJndiProperties());
			if (url==null) {
				throw new NamingException("cannot find jndiProperties from ["+getJndiProperties()+"]");
			}
			try {
				jndiEnv.load(url.openStream());
			} catch (IOException e) {
				throw new NamingException("cannot load jndiProperties ["+getJndiProperties()+"] from url ["+url.toString()+"]");
			}
		}
		if (getInitialContextFactoryName() != null)
			jndiEnv.put(Context.INITIAL_CONTEXT_FACTORY, getInitialContextFactoryName());
		if (getProviderURL() != null)
			jndiEnv.put(Context.PROVIDER_URL, getProviderURL());
		if (getAuthentication() != null)
			jndiEnv.put(Context.SECURITY_AUTHENTICATION, getAuthentication());
		if (getPrincipal() != null || getCredentials() != null || getJndiAuthAlias()!=null) {
			CredentialFactory jndiCf = new CredentialFactory(getJndiAuthAlias(), getPrincipal(), getCredentials());
			if (StringUtils.isNotEmpty(jndiCf.getUsername()))
				jndiEnv.put(Context.SECURITY_PRINCIPAL, jndiCf.getUsername());
			if (StringUtils.isNotEmpty(jndiCf.getPassword()))
				jndiEnv.put(Context.SECURITY_CREDENTIALS, jndiCf.getPassword());
		}
		if (getUrlPkgPrefixes() != null)
			jndiEnv.put(Context.URL_PKG_PREFIXES, getUrlPkgPrefixes());
		if (getSecurityProtocol() != null)
			jndiEnv.put(Context.SECURITY_PROTOCOL, getSecurityProtocol());
		
		if (log.isDebugEnabled()) {
			for(Iterator it=jndiEnv.keySet().iterator(); it.hasNext();) {
				String key=(String) it.next();
				String value=jndiEnv.getProperty(key);
				log.debug("jndiEnv ["+key+"] = ["+value+"]");
			}
		}
		return jndiEnv;
	}
	
    /**
     *  Gets the Context<br/>
     *  When InitialContextFactory and ProviderURL are set, these are used
     *  to get the <code>Context</code>. Otherwise the the InitialContext is
     *  retrieved without parameters.<br/>
     *  <b>Notice:</b> you can set the parameters on the commandline with <br/>
     *  java -Djava.naming.factory.initial= xxx -Djava.naming.provider.url=xxx
     * <br/><br/>
     */
    public Context getContext() throws NamingException {

        if (null == context) {
        	Hashtable jndiEnv = getJndiEnv();
        	if (jndiEnv.size()>0) {
				log.debug("creating initial JNDI-context using specified environment");
                context = (Context) new InitialContext(jndiEnv);
            } else {
				log.debug("creating initial JNDI-context");
                context = (Context) new InitialContext();
            }
        }
        return context;
    }
    
    public String getCredentials() {
        return credentials;
    }
    /**
     *  Gets the value of initialContextFactoryName
     */
    public String getInitialContextFactoryName() {
        return initialContextFactoryName;
    }
    /**
     *  Gets the value of providerURL
     */
    public String getProviderURL() {
        return providerURL;
    }
    public String getSecurityProtocol() {
        return securityProtocol;
    }
    public String getUrlPkgPrefixes() {
        return urlPkgPrefixes;
    }

	@IbisDoc({"maps to the field context.security_authentication", ""})
    public void setAuthentication(String newAuthentication) {
        authentication = newAuthentication;
    }

	@IbisDoc({"username to connect to context, maps to context.security_credentials", ""})
    public void setCredentials(String newCredentials) {
        credentials = newCredentials;
    }
    /**
     *  Sets the value of initialContextFactoryName
     */
	@IbisDoc({"class to use as initial context factory", ""})
    public void setInitialContextFactoryName(String value) {
        initialContextFactoryName = value;
    }
    /**
     *  Sets the value of providerURL
     */
	@IbisDoc({"", " "})
    public void setProviderURL(String value) {
        providerURL = value;
    }

	@IbisDoc({"maps to the field context.security_protocol", ""})
    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }
    /**
     * Setter for <code>Context.URL_PKG_PREFIXES</code><br/>
     * Creation date: (03-04-2003 8:50:36)
     */
	@IbisDoc({"maps to the field context.url_pkg_prefixes", ""})
    public void setUrlPkgPrefixes(String newUrlPkgPrefixes) {
        urlPkgPrefixes = newUrlPkgPrefixes;
    }
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

	public String getJmsRealmName() {
		return this.jmsRealmName;
	}

	public String getAuthentication() {
		return authentication;
	}

	public String getPrincipal() {
		return principal;
	}

	@IbisDoc({"username to connect to context, maps to context.security_principal", ""})
	public void setPrincipal(String string) {
		principal = string;
	}

	@IbisDoc({"authentication alias, may be used to override principal and credential-settings", ""})
	public void setJndiAuthAlias(String string) {
		jndiAuthAlias = string;
	}
	public String getJndiAuthAlias() {
		return jndiAuthAlias;
	}

	public void setJndiContextPrefix(String string) {
		jndiContextPrefix = string;
	}
	public String getJndiContextPrefix() {
		return jndiContextPrefix;
	}

	public String getJndiProperties() {
		return jndiProperties;
	}
	public void setJndiProperties(String jndiProperties) {
		this.jndiProperties = jndiProperties;
	}
}
