/*
 * EjbJmsConfigurator.java
 * 
 * Created on 4-okt-2007, 10:38:10
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.ejb;

import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import java.util.Hashtable;
import java.util.Set;
import javax.jms.Destination;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;

/**
 *
 * @author m00035f
 */
public class EjbJmsConfigurator implements IJmsConfigurator {
    private final static String LISTENER_PORTNAME_SUFFIX = "JmsListenerPort";
    
    private JmsListener jmsListener;
    private ObjectName listenerPortName;
    private AdminService adminService;
    private Destination destination;
    
    public Destination getDestination() {
        return destination;
    }

    public void configureJmsReceiver(JmsListener jmsListener) throws ConfigurationException {
        try {
            this.jmsListener = jmsListener;
            this.listenerPortName = getListenerPortMBean(jmsListener);
            String destinationName = (String) getAdminService().getAttribute(listenerPortName, "jmsDestJNDIName");
            Context ctx = new InitialContext();
            this.destination = (Destination) ctx.lookup(destinationName);
        } catch (Exception ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void openJmsReceiver() throws ListenerException {
        try {
            getAdminService().invoke(listenerPortName, "start", null, null);
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }

    public void closeJmsReceiver() throws ListenerException {
        try {
            getAdminService().invoke(listenerPortName, "stop", null, null);
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }
    
    protected ObjectName getListenerPortMBean(JmsListener jmsListener)  throws ConfigurationException {
        try {
            // Get the admin service
            AdminService as = getAdminService();
            
            // Create ObjectName instance to search for
            Hashtable queryProperties = new Hashtable();
            String listenerPortName1 = getListenerPortName(jmsListener);
            queryProperties.put("name",listenerPortName1);
            queryProperties.put("type", "ListenerPort");
            ObjectName queryName = new ObjectName("server", queryProperties);
            
            // Query AdminService for the name
            Set names = as.queryNames(queryName, null);
            
            // Assume that only 1 is returned and return it
            if (names.size() == 0) {
                throw new ConfigurationException("Can not find WebSphere ListenerPort by name of '"
                        + listenerPortName1 + "', JmsListener can not be configured");
            } else if (names.size() > 1) {
                throw new ConfigurationException("Multiple WebSphere ListenerPorts found by name of '"
                        + listenerPortName1 + "': " + names + ", JmsListener can not be configured");
            } else {
                return (ObjectName) names.iterator().next();
            }
        } catch (MalformedObjectNameException ex) {
            throw new ConfigurationException(ex);
        }
    }
    
    protected synchronized AdminService getAdminService() {
        if (this.adminService == null) {
            this.adminService = AdminServiceFactory.getAdminService();
        }
        return this.adminService;
    }
    protected String getListenerPortName(JmsListener jmsListener) {
        String name = jmsListener.getListenerPort();
        
        if (name == null) {
            IAdapter adapter;
            GenericReceiver receiver;
            receiver = (GenericReceiver)jmsListener.getHandler();
            adapter = receiver.getAdapter();
            name = adapter.getName() + '-' + receiver.getName() + '-' + LISTENER_PORTNAME_SUFFIX;
            name = name.replace(' ', '-');
        }
        
        return name;
    }

}
