package nl.nn.adapterframework.jms;

import org.apache.commons.lang.builder.ToStringBuilder;
/**
 * A JmsRealm is a definition of a JMS provider, and is kind of a utility
 * class to prevent the tedeous work of repeatedly defining all parameters
 * to connect to a queue or topic.
 * <br/>
 * This class is not an extension of JNDIBase, which would be logical, because
 * in the JMSBase class the function PropertyUtils.copyProperties is used, which cannot
 * handle this.
 * <p>$Id: JmsRealm.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $</p>
 * @see JMSBase#setJmsRealm
 * @author Johan Verrips IOS
 */
public class JmsRealm  {
	public static final String version="$Id: JmsRealm.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $";
	private String realmName;

	private String queueConnectionFactoryName;
	private String topicConnectionFactoryName;
    private String providerURL = null;
    private String initialContextFactoryName = null;
    private String authentication = null;
    private String credentials = null;
    private String urlPkgPrefixes = null;
    private String securityProtocol = null;


/**
 * JndiConfiguration constructor comment.
 */
public JmsRealm() {

	super();
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
}
