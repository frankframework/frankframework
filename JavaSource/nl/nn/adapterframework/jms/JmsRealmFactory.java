package nl.nn.adapterframework.jms;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Singleton that has the different jmsRealms.   <br/>
 * Typical use: JmsRealmFactory.getInstance().&lt;method to execute&gt;
 * <br/>
 * @author Johan Verrips IOS
 * @see JmsRealm
 */
public class JmsRealmFactory {
	public static final String version="$Id: JmsRealmFactory.java,v 1.1 2004-02-04 08:36:16 a1909356#db2admin Exp $";


    private static JmsRealmFactory self = null;
    private Hashtable jmsRealms = new Hashtable();
    private Logger log;

    /**
     * Private constructor to prevent breaking of the singleton pattern
     */
    private JmsRealmFactory() {
        super();
        log = Logger.getLogger(this.getClass());

    }
    /**
     * Get a hold of the singleton JmsRealmFactory

     */
    public static synchronized JmsRealmFactory getInstance() {
        if (self == null) {
            self = new JmsRealmFactory();
        }
        return (self);

    }
    /**
     * Get a realm by name
     * @return JmsRealm the requested realm or null if none was found under that name
     */
    public JmsRealm getJmsRealm(String jmsRealmName) {
        JmsRealm jmsRealm = null;
        try {
            jmsRealm = (JmsRealm) jmsRealms.get(jmsRealmName);
            if (jmsRealm==null) {
                log.warn("no jms realm found under name ["+jmsRealmName+"]");
                log.warn(this.toString());
            }

        } catch (NullPointerException ignore) {
            log.warn("no jms realm found under name ["+jmsRealmName+"]");
            log.warn(this.toString());
        }
        return jmsRealm;
    }
    /**
     * Get the realmnames as an Iterator, alphabetically sorted
     * @return Iterator with the realm names, alphabetically sorted
     */
    public Iterator getRegisteredRealmNames() {
        SortedSet sortedKeys = new TreeSet(jmsRealms.keySet());
        return sortedKeys.iterator();
    }
    /**
     * Get the names as a list
     * @return ArrayList with the realm names
     */
    public ArrayList getRegisteredRealmNamesAsList() {
        Iterator it = getRegisteredRealmNames();
        ArrayList result = new ArrayList();
        while (it.hasNext()) {
            result.add((String) it.next());
        }
        return result;
    }
    /**
     * register a Realm
     * @param jmsRealm
     */
    public void registerJmsRealm(JmsRealm jmsRealm) {
        jmsRealms.put(jmsRealm.getRealmName(), jmsRealm);
        log.debug("JmsRealmFactory registered realm [" + jmsRealm.toString() + "]");
    }
    public String toString(){
        return ToStringBuilder.reflectionToString(this);
    }
}
