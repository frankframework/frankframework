/*
 * $Log: StartIbis.java,v $
 * Revision 1.7  2007-12-27 16:00:30  europe\L190409
 * cosmetic changes
 *
 * Revision 1.6  2007/10/09 15:02:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * delegate work to IbisManager
 *
 */
package nl.nn.adapterframework.cmdline;

import nl.nn.adapterframework.configuration.IbisMain;
import nl.nn.adapterframework.configuration.IbisManager;

/**
 * Starts up a configuration in a plain JVM.
 * 
 * Works only for pulling listeners.
 * 
 * @author  Johan Verrips
 * @version Id
 */
public class StartIbis {

	public static void main(String[] args) {
        String configFile = IbisManager.DFLT_CONFIGURATION;
        if (args.length > 0) {
            configFile = args[0];
        }
        IbisMain im=new IbisMain();
        im.initConfig(null, configFile, IbisMain.DFLT_AUTOSTART);
	}
}
