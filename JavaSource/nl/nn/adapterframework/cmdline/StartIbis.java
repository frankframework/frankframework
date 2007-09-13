/*
 * Created on 26-apr-04
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.cmdline;

import nl.nn.adapterframework.configuration.IbisMain;

/**
 *  
 * Configures a queue connection and receiver and starts listening to it.
 * @author     Johan Verrips
 * created    14 februari 2003
 */
public class StartIbis {
	public static final String version="$RCSfile: StartIbis.java,v $ $Revision: 1.5.4.1 $ $Date: 2007-09-13 13:27:18 $";

	/**
	 * 
	 */
	public StartIbis() {
		super();
	}
	public static void main(String[] args) {
        String configFile = IbisMain.DFLT_CONFIGURATION;
        if (args.length > 0) {
            configFile = args[0];
        }
        IbisMain im=new IbisMain();
        im.initConfig(configFile, 
            IbisMain.DFLT_DIGESTER_RULES, IbisMain.
            DFLT_AUTOSTART);
	}
}
