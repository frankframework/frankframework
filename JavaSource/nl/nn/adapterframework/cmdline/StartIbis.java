/*
 * $Log: StartIbis.java,v $
 * Revision 1.5.4.4  2007-10-10 14:30:47  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
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
	public static final String version="$RCSfile: StartIbis.java,v $ $Revision: 1.5.4.4 $ $Date: 2007-10-10 14:30:47 $";

	public StartIbis() {
		super();
	}
	public static void main(String[] args) {
        String configFile = IbisManager.DFLT_CONFIGURATION;
        if (args.length > 0) {
            configFile = args[0];
        }
        IbisMain im=new IbisMain();
        im.initConfig(null, configFile, 
            IbisMain.DFLT_AUTOSTART);
	}
}
