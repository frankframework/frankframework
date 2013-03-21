/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: StartIbis.java,v $
 * Revision 1.10  2011-11-30 13:52:03  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.8  2008/02/13 12:50:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed IbisMain to IbisContext
 *
 * Revision 1.7  2007/12/27 16:00:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.6  2007/10/09 15:02:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * delegate work to IbisManager
 *
 */
package nl.nn.adapterframework.cmdline;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;

/**
 * Starts up a configuration in a plain JVM.
 * 
 * Works only for pulling listeners.
 * 
 * @author  Johan Verrips
 * @version $Id$
 */
public class StartIbis {

	public static void main(String[] args) {
        String configFile = IbisManager.DFLT_CONFIGURATION;
        if (args.length > 0) {
            configFile = args[0];
        }
        IbisContext im=new IbisContext();
        im.initConfig(null, configFile, IbisContext.DFLT_AUTOSTART);
	}
}
