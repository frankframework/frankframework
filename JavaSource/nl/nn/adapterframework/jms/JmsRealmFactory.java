/*
 * $Log: JmsRealmFactory.java,v $
 * Revision 1.5  2007-02-12 13:58:11  europe\L190409
 * Logger from LogUtil
 *
 * Revision 1.4  2004/03/26 10:42:55  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 18:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 */
package nl.nn.adapterframework.jms;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * Singleton that has the different jmsRealms.   <br/>
 * Typical use: JmsRealmFactory.getInstance().&lt;method to execute&gt;
 * <br/>
 * @version Id
 * @author Johan Verrips IOS
 * @see JmsRealm
 */
public class JmsRealmFactory {
	public static final String version="$RCSfile: JmsRealmFactory.java,v $ $Revision: 1.5 $ $Date: 2007-02-12 13:58:11 $";
	private Logger log;


    private static JmsRealmFactory self = null;
    private Hashtable jmsRealms = new Hashtable();

    /**
     * Private constructor to prevent breaking of the singleton pattern
     */
    private JmsRealmFactory() {
        super();
        log = LogUtil.getLogger(this);

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
                log.error("no JmsRealm found under name ["+jmsRealmName+"], factory contents ["+toString()+"]");
            }

        } catch (NullPointerException ignore) {
            log.error("no JmsRealm found under name ["+jmsRealmName+"], factory contents ["+toString()+"]");
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
