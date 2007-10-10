/*
 * $Log: JmsRealmFactoryBean.java,v $
 * Revision 1.4  2007-10-10 08:26:17  europe\L190409
 * added ToDo + cleanup
 *
 */
package nl.nn.adapterframework.jms;

/**
 * Wrapper around the JmsRealmFactory, especially for the digester.
 * TODO: Possible to remove this class by putting JmsRealmFactory in Spring context
 * 
 * @author Johan Verrips IOS
 * @version Id
 */
public class JmsRealmFactoryBean  {

	private JmsRealmFactory jmsRealmFactory;

	public JmsRealmFactoryBean(){
		jmsRealmFactory=JmsRealmFactory.getInstance();
	}
	public void registerJmsRealm(JmsRealm jmsRealm) {
		jmsRealmFactory.registerJmsRealm(jmsRealm);
	}
}
