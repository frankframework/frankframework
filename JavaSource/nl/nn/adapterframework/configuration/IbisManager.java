/*
 * Created on 6-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;

/**
 * @author m00035f
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface IbisManager {
    Configuration getConfiguration();
    void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy);
    void startIbis();
    void startAdapters();
    void stopAdapters();
    void startAdapter(IAdapter adapter);
    void stopAdapter(IAdapter adapter);
}
