/*
 * $Log: JmsRealm.java,v $
 * Revision 1.12.2.2  2007-10-10 14:30:42  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.13  2007/10/10 08:23:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added a ToDo about cleanup
 *
 * Revision 1.12  2007/07/10 07:19:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add some checks
 *
 * Revision 1.11  2007/05/08 16:07:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add jndiAuthAlias attribute
 *
 * Revision 1.10  2007/05/03 11:40:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add Principal
 *
 * Revision 1.9  2007/02/12 13:58:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.8  2006/08/21 15:11:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.7  2004/10/05 10:42:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.6  2004/08/23 13:08:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.5  2004/03/30 07:30:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.4  2004/03/26 10:42:55  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 18:06:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added properties for Transaction control
 *
 */
package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
/**
 * A JmsRealm is a definition of a JMS provider, and is kind of a utility
 * class to prevent the tedeous work of repeatedly defining all parameters
 * to connect to a queue or topic.
 * <br/>
 * This class is not an extension of JNDIBase, which would be logical, because
 * in the JMSBase class the function PropertyUtils.copyProperties is used, which cannot
 * handle this.
 * @version Id
 * @see JMSFacade#setJmsRealm
 * @author Johan Verrips IOS
 */
public class JmsRealm {
	//TODO: change to J2eeRealm
	public static final String version="$RCSfile: JmsRealm.java,v $ $Revision: 1.12.2.2 $ $Date: 2007-10-10 14:30:42 $";
	private Logger log = LogUtil.getLogger(this);

	private String realmName;

    private String providerURL = null;
    private String initialContextFactoryName = null;
    private String authentication = null;
    private String credentials = null;
	private String principal = null;
	private String jndiAuthAlias = null;
    private String urlPkgPrefixes = null;
    private String securityProtocol = null;

	private String queueConnectionFactoryName;
	private String topicConnectionFactoryName;
	private String queueConnectionFactoryNameXA;
	private String topicConnectionFactoryNameXA;

	private String datasourceName;
	private String datasourceNameXA;
    
	private String userTransactionUrl;

	public JmsRealm() {
		super();
	}

 	/**
 	 * Includes another realm into this one
 	 */ 
	public void setAliasForRealm(String jmsRealmName){
		String myName=getRealmName(); // save name, as it will be overwritten by the copy
		try {
			copyRealm(this,jmsRealmName);
		} catch (ConfigurationException e) {
			log.warn("cannot set aliasForRealm",e);
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
		    PropertyUtils.copyProperties(destination, this);
	    }catch (Exception e) {
			log.error(logPrefixDest+"unable to copy properties of JmsRealm", e);
		}
		log.info(logPrefixDest+"loaded properties from jmsRealm ["+toString()+"]");				    
    }

 	/**
 	 * copies matching properties from a JmsRealm to any other class
 	 * @see JmsRealm
     * 
     * TODO: Some amount of cleanup possible by putting JmsRealmFactory in Spring context
 	 */ 
	public static void copyRealm(Object destination, String jmsRealmName) throws ConfigurationException {

	    JmsRealm jmsRealm=JmsRealmFactory.getInstance().getJmsRealm(jmsRealmName);
	    if (jmsRealm==null) {
	    	throw new ConfigurationException("Could not find jmsRealm ["+jmsRealmName+"]");
	    }
	    jmsRealm.copyRealm(destination);
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
	/**
	 * The name of the QueueConnectionFactory <br/>
	 */
	public java.lang.String getQueueConnectionFactoryName() {
		return queueConnectionFactoryName;
	}
	/**
	 * The name of this realm<br/>
	 */
	public java.lang.String getRealmName() {
		return realmName;
	}
    public String getSecurityProtocol() {
        return securityProtocol;
    }
	/**
	 * The name of the TopicConnectionFactory <br/>
	 */
	public java.lang.String getTopicConnectionFactoryName() {
		return topicConnectionFactoryName;
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
	/**
	 * Set the name of the QueueConnectionFactory<br/>
	 * @param newQueueConnectionFactoryName java.lang.String
	 */
	public void setQueueConnectionFactoryName(java.lang.String newQueueConnectionFactoryName) {
		queueConnectionFactoryName = newQueueConnectionFactoryName;
	}
	/**
	 * Set the name of this realm<br/>.
	 * @param newName java.lang.String
	 */
	public void setRealmName(java.lang.String newName) {
		realmName = newName;
	}
	    public void setSecurityProtocol(String securityProtocol) {
	        this.securityProtocol = securityProtocol;
	    }
	/**
	 * Set the name of the TopicConnectionFactory<br/>
	 * @param newTopicConnectionFactoryName java.lang.String
	 */
	public void setTopicConnectionFactoryName(java.lang.String newTopicConnectionFactoryName) {
		topicConnectionFactoryName = newTopicConnectionFactoryName;
	}
    public void setUrlPkgPrefixes(String urlPkgPrefixes) {
        this.urlPkgPrefixes = urlPkgPrefixes;
    }
  /**
   * The <code>toString()</code> method retrieves its value
   * by reflection.
   * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
   *
   **/
  public String toString() {
	return  ToStringBuilder.reflectionToString(this);

  }
	/**
	 * Returns the queueConnectionFactoryNameXA.
	 */
	public String getQueueConnectionFactoryNameXA() {
		return queueConnectionFactoryNameXA;
	}

	/**
	 * Returns the topicConnectionFactoryNameXA.
	 */
	public String getTopicConnectionFactoryNameXA() {
		return topicConnectionFactoryNameXA;
	}


	/**
	 * Sets the queueConnectionFactoryNameXA.
	 * @param queueConnectionFactoryNameXA The queueConnectionFactoryNameXA to set
	 */
	public void setQueueConnectionFactoryNameXA(String queueConnectionFactoryNameXA) {
		this.queueConnectionFactoryNameXA = queueConnectionFactoryNameXA;
	}

	/**
	 * Sets the topicConnectionFactoryNameXA.
	 * @param topicConnectionFactoryNameXA The topicConnectionFactoryNameXA to set
	 */
	public void setTopicConnectionFactoryNameXA(String topicConnectionFactoryNameXA) {
		this.topicConnectionFactoryNameXA = topicConnectionFactoryNameXA;
	}


	public String getDatasourceName() {
		return datasourceName;
	}
	public String getDatasourceNameXA() {
		return datasourceNameXA;
	}
	public void setDatasourceName(String string) {
		datasourceName = string;
	}
	public void setDatasourceNameXA(String string) {
		datasourceNameXA = string;
	}

	public String getUserTransactionUrl() {
		return userTransactionUrl;
	}
	public void setUserTransactionUrl(String string) {
		userTransactionUrl = string;
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

}
