package nl.nn.adapterframework.jms;

/**
 * Wrapper around the JmsRealmFactory, especially for the digester.
 * <p>$Id: JmsRealmFactoryBean.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $</p>
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
