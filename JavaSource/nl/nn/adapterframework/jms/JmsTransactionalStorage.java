/*
 * $Log: JmsTransactionalStorage.java,v $
 * Revision 1.1  2004-03-23 18:02:25  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.receivers.TransactionalStorage;

/**
 * JMS implementation of <code>ITransactionalStorage</code>.
 * 
 * <p>$Id: JmsTransactionalStorage.java,v 1.1 2004-03-23 18:02:25 L190409 Exp $</p>
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JmsTransactionalStorage extends TransactionalStorage implements HasPhysicalDestination{
	public static final String version="$Id: JmsTransactionalStorage.java,v 1.1 2004-03-23 18:02:25 L190409 Exp $";

public JmsTransactionalStorage() {
	super();
	JmsSender jmsSender = new JmsSender();
	JmsListener jmsListener = new JmsListener();
	
	jmsSender.setTransacted(true);
	jmsListener.setTransacted(true);
	setSender(jmsSender);
	setListener(jmsListener);
	setPersistent(true);

}
 	/**
 	 * loads JNDI properties from a JmsRealm
 	 * @see JmsRealm
 	 */ 
	public void setJmsRealm(String jmsRealmName){
		JmsRealm.copyRealm(getSender(),jmsRealmName);
		JmsRealm.copyRealm(getListener(),jmsRealmName);
    }
    public void setDestinationName(String destinationName) {
    	((JmsSender)getSender()).setDestinationName(destinationName);
    	((JmsListener)getListener()).setDestinationName(destinationName);
    }
    public void setDestinationType(String type) {
    	((JmsSender)getSender()).setDestinationType(type);
    	((JmsListener)getListener()).setDestinationType(type);
    }
    public void setPersistent(boolean persistent) {
    	((JmsSender)getSender()).setPersistent(persistent);
    	((JmsListener)getListener()).setPersistent(persistent);
    }
	public String getPhysicalDestinationName() {
		return ((JmsSender)getSender()).getPhysicalDestinationName();
	}
}
