/*
 * $Log: JmsRealm.java,v $
 * Revision 1.3  2004-03-23 18:06:05  L190409
 * added properties for Transaction control
 *
 */
package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.core.INamedObject;

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
 * <p>$Id: JmsRealm.java,v 1.3 2004-03-23 18:06:05 L190409 Exp $</p>
 * @see JMSBase#setJmsRealm
 * @author Johan Verrips IOS
 */
public class JmsRealm {
	//TODO: change to J2eeRealm
	public static final String version="$Id: JmsRealm.java,v 1.3 2004-03-23 18:06:05 L190409 Exp $";
	private String realmName;
	private Logger log = Logger.getLogger(this.getClass());

    private String providerURL = null;
    private String initialContextFactoryName = null;
    private String authentication = null;
    private String credentials = null;
    private String urlPkgPrefixes = null;
    private String securityProtocol = null;

	private String queueConnectionFactoryName;
	private String topicConnectionFactoryName;
	private String queueConnectionFactoryNameXA;
	private String topicConnectionFactoryNameXA;

	private String datasourceName;
	private String datasourceNameXA;
	private String username;
	private String password;
    
	private String transactionManagerFactoryClassName;
	private String transactionManagerFactoryMethod;
	private String userTransactionUrl;

/**
 * JndiConfiguration constructor comment.
 */
public JmsRealm() {

	super();
}
 	/**
 	 * Includes another realm into this one
 	 */ 
	public void setAliasForRealm(String jmsRealmName){
		String myName=getRealmName(); // save name, as it will be overwritten by the copy
		copyRealm(this,jmsRealmName);
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
 	 */ 
	public static void copyRealm(Object destination, String jmsRealmName) {

	    JmsRealm jmsRealm=JmsRealmFactory.getInstance().getJmsRealm(jmsRealmName);
	    if (null!=jmsRealm) {
		    jmsRealm.copyRealm(destination);
	    }
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
 * Creation date: (27-03-2003 8:40:28)
 * @return java.lang.String
 */
public java.lang.String getQueueConnectionFactoryName() {
	return queueConnectionFactoryName;
}
/**
 * The name of this realm<br/>
 * Creation date: (27-03-2003 8:40:28)
 * @return java.lang.String
 */
public java.lang.String getRealmName() {
	return realmName;
}
    public String getSecurityProtocol() {
        return securityProtocol;
    }
/**
 * The name of the TopicConnectionFactory <br/>
 * Creation date: (27-03-2003 8:40:28)
 * @return java.lang.String
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
 * Creation date: (27-03-2003 8:40:28)
 * @param newQueueConnectionFactoryName java.lang.String
 */
public void setQueueConnectionFactoryName(java.lang.String newQueueConnectionFactoryName) {
	queueConnectionFactoryName = newQueueConnectionFactoryName;
}
/**
 * Set the name of this realm<br/>.
 * Creation date: (27-03-2003 8:40:28)
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
 * Creation date: (27-03-2003 8:40:28)
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
   * by reflection
   * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
   *
   **/
  public String toString() {
	return  ToStringBuilder.reflectionToString(this);

  }
	/**
	 * Returns the queueConnectionFactoryNameXA.
	 * @return String
	 */
	public String getQueueConnectionFactoryNameXA() {
		return queueConnectionFactoryNameXA;
	}

	/**
	 * Returns the topicConnectionFactoryNameXA.
	 * @return String
	 */
	public String getTopicConnectionFactoryNameXA() {
		return topicConnectionFactoryNameXA;
	}

	/**
	 * Returns the transactionManagerFactoryClassName.
	 * @return String
	 */
	public String getTransactionManagerFactoryClassName() {
		return transactionManagerFactoryClassName;
	}

	/**
	 * Returns the transactionManagerFactoryMethod.
	 * @return String
	 */
	public String getTransactionManagerFactoryMethod() {
		return transactionManagerFactoryMethod;
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

	/**
	 * Sets the transactionManagerFactoryClassName.
	 * @param transactionManagerFactoryClassName The transactionManagerFactoryClassName to set
	 */
	public void setTransactionManagerFactoryClassName(String transactionManagerFactoryClassName) {
		this.transactionManagerFactoryClassName =
			transactionManagerFactoryClassName;
	}

	/**
	 * Sets the transactionManagerFactoryMethod.
	 * @param transactionManagerFactoryMethod The transactionManagerFactoryMethod to set
	 */
	public void setTransactionManagerFactoryMethod(String transactionManagerFactoryMethod) {
		this.transactionManagerFactoryMethod = transactionManagerFactoryMethod;
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

}
