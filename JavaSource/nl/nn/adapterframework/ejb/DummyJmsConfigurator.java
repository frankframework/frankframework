/*
 * $Log: DummyJmsConfigurator.java,v $
 * Revision 1.3  2007-11-05 10:33:15  europe\M00035F
 * Move interface 'IJmsConfigurator' from package 'configuration' to package 'core' in preparation of renaming it
 *
 * Revision 1.2  2007/10/09 16:07:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import nl.nn.adapterframework.core.IJmsConfigurator;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.unmanaged.AbstractJmsConfigurator;

/**
 * Implementation of interface IJmsConfigurator which does not actually
 * do anything. This implementation is for the EJB version of IBIS, where
 * all JMS Configuration happens on the container-level.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class DummyJmsConfigurator extends AbstractJmsConfigurator implements IJmsConfigurator {
    
    public void openJmsReceiver() throws ListenerException {
        return;
    }

    public void closeJmsReceiver() throws ListenerException {
        return;
    }
}
