package nl.nn.adapterframework.jms;

/**
 * Wrapper around the JmsRealmFactory, especially for the digester.
 * TODO: Possible to remove this class by putting JmsRealmFactory in Spring context
 * @version Id
 * @author Johan Verrips IOS
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
