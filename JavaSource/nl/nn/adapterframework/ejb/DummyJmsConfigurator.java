/*
 * DummyJmsConfigurator.java
 * 
 * Created on 2-okt-2007, 9:30:36
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.ejb;

import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.unmanaged.AbstractJmsConfigurator;

/**
 * Implementation of interface IJmsConfigurator which does not actually
 * do anything. This implementation is for the EJB version of IBIS, where
 * all JMS Configuration happens on the container-level.
 * 
 * @author m00035f
 */
public class DummyJmsConfigurator extends AbstractJmsConfigurator implements IJmsConfigurator {
    
    public void openJmsReceiver() throws ListenerException {
        return;
    }

    public void closeJmsReceiver() throws ListenerException {
        return;
    }
}
