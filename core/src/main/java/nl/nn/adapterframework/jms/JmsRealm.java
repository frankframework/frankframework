/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.util.Iterator;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.BeanMap;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.LogUtil;
/**
 * A JmsRealm is a definition of a JMS provider, and is kind of a utility
 * class to prevent the tedeous work of repeatedly defining all parameters
 * to connect to a queue or topic.
 * <br/>
 * This class is not an extension of JNDIBase, which would be logical, because
 * in the JMSBase class the function PropertyUtils.copyProperties is used, which cannot
 * handle this.
 * @see JMSFacade#setJmsRealm
 * @author Johan Verrips IOS
 */
public class JmsRealm {
	//TODO: change to J2eeRealm
	private Logger log = LogUtil.getLogger(this);

	private String realmName;

	private String providerURL = null;
	private String initialContextFactoryName = null;
	private String authentication = null;
	private String credentials = null;
	private String principal = null;
	private String authAlias = null;
	private String jndiAuthAlias = null;
	private String urlPkgPrefixes = null;
	private String securityProtocol = null;
	private String jndiContextPrefix = "";
	private String jndiProperties = null;

	private String queueConnectionFactoryName;
	private String topicConnectionFactoryName;

	private String datasourceName;

	private String userTransactionUrl;

	public JmsRealm() {
		super();
	}

	/**
	 * Includes another realm into this one
	 */
	public void setAliasForRealm(String jmsRealmName) {
		String myName = getRealmName(); // save name, as it will be overwritten by the copy
		try {
			copyRealm(this, jmsRealmName);
		} catch (ConfigurationException e) {
			log.warn("cannot set aliasForRealm", e);
		}
		setRealmName(myName); // restore the original name
	}

	/**
	 * copies matching properties to any other class
	 */
	public void copyRealm(Object destination) {
		String logPrefixDest=destination.getClass().getName()+" ";
		if (destination instanceof INamedObject) {
			INamedObject namedDestination = (INamedObject) destination;
			logPrefixDest += "["+namedDestination.getName()+"] ";
		}
		try {
			BeanMap thisBeanMap = new BeanMap(this);
			BeanMap destinationBeanMap = new BeanMap(destination);
			Iterator<String> iterator = thisBeanMap.keyIterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				Object value = thisBeanMap.get(key);
				if (value != null && !key.equals("class") && destinationBeanMap.containsKey(key)) {
					PropertyUtils.setProperty(destination, key, value);
				}
			}
		}catch (Exception e) {
			log.error(logPrefixDest+"unable to copy properties of JmsRealm", e);
		}
		log.info(logPrefixDest+"loaded properties from jmsRealm ["+toString()+"]");
	}

	/**
	 * copies matching properties from a JmsRealm to any other class
	 * 
	 * @see JmsRealm
	 * 
	 * TODO: Some amount of cleanup possible by putting JmsRealmFactory in Spring context
	 */
	public static void copyRealm(Object destination, String jmsRealmName) throws ConfigurationException {

		JmsRealm jmsRealm = JmsRealmFactory.getInstance().getJmsRealm(jmsRealmName);
		if (jmsRealm == null) {
			throw new ConfigurationException("Could not find jmsRealm [" + jmsRealmName + "]");
		}
		jmsRealm.copyRealm(destination);
	}

	/**
	 * The <code>toString()</code> method retrieves its value by reflection.
	 * 
	 * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
	 *
	 **/
	@Override
	public String toString() {
		try {
			return ToStringBuilder.reflectionToString(this);
		} catch (Throwable t) {
			log.warn("exception getting string representation of jmsRealm [" + getRealmName() + "]", t);
		}
		return null;
	}

	/**
	 * Set the name of this realm<br/>
	 */
	public void setRealmName(String newName) {
		realmName = newName;
	}
	public String getRealmName() {
		return realmName;
	}

	public String getAuthentication() {
		return authentication;
	}

	public String getCredentials() {
		return credentials;
	}

	public String getInitialContextFactoryName() {
		return initialContextFactoryName;
	}

	public String getProviderURL() {
		return providerURL;
	}

	public String retrieveConnectionFactoryName() {
		if (queueConnectionFactoryName != null) {
			return queueConnectionFactoryName;
		} else if (topicConnectionFactoryName != null) {
			return topicConnectionFactoryName;
		}
		return null;
	}


	/**
	 * Set the name of the QueueConnectionFactory<br/>
	 */
	public void setQueueConnectionFactoryName(String newQueueConnectionFactoryName) {
		queueConnectionFactoryName = newQueueConnectionFactoryName;
	}
	public String getQueueConnectionFactoryName() {
		return queueConnectionFactoryName;
	}

	/**
	 * Set the name of the TopicConnectionFactory<br/>
	 */
	public void setTopicConnectionFactoryName(String newTopicConnectionFactoryName) {
		topicConnectionFactoryName = newTopicConnectionFactoryName;
	}
	public String getTopicConnectionFactoryName() {
		return topicConnectionFactoryName;
	}

	public void setDatasourceName(String string) {
		datasourceName = string;
	}
	public String getDatasourceName() {
		return datasourceName;
	}


	public void setSecurityProtocol(String securityProtocol) {
		this.securityProtocol = securityProtocol;
	}
	public String getSecurityProtocol() {
		return securityProtocol;
	}


	public void setUrlPkgPrefixes(String urlPkgPrefixes) {
		this.urlPkgPrefixes = urlPkgPrefixes;
	}
	public String getUrlPkgPrefixes() {
		return urlPkgPrefixes;
	}

	public void setAuthentication(String authentication) {
		this.authentication = authentication;
	}

	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}

	public void setInitialContextFactoryName(String initialContextFactoryName) {
		this.initialContextFactoryName = initialContextFactoryName;
	}

	public void setProviderURL(String providerURL) {
		this.providerURL = providerURL;
	}







	public void setUserTransactionUrl(String string) {
		userTransactionUrl = string;
	}
	public String getUserTransactionUrl() {
		return userTransactionUrl;
	}

	public void setPrincipal(String string) {
		principal = string;
	}
	public String getPrincipal() {
		return principal;
	}

	public void setJndiAuthAlias(String string) {
		jndiAuthAlias = string;
	}
	public String getJndiAuthAlias() {
		return jndiAuthAlias;
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	public void setJndiContextPrefix(String string) {
		jndiContextPrefix = string;
	}
	public String getJndiContextPrefix() {
		return jndiContextPrefix;
	}

	public void setJndiProperties(String jndiProperties) {
		this.jndiProperties = jndiProperties;
	}
	public String getJndiProperties() {
		return jndiProperties;
	}


}
