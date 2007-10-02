/*
 * EjbIbisManager.java
 * 
 * Created on 2-okt-2007, 10:06:39
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.ejb;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;

/**
 *
 * @author m00035f
 */
public class EjbIbisManager implements IbisManager {

    public Configuration getConfiguration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void startIbis() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void startAdapters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stopAdapters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void startAdapter(IAdapter adapter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stopAdapter(IAdapter adapter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void loadConfigurationFile(String configurationFile) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getDeploymentModeString() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getDeploymentMode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
