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
 * $Log: EchoSender.java,v $
 * Revision 1.7  2011-11-30 13:51:51  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2008/12/30 17:01:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.4  2008/11/26 09:38:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Fixed warning message in deprecated classes
 *
 * Revision 1.3  2008/08/06 16:38:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved from pipes to senders package
 *
 * Revision 1.2  2008/05/15 15:10:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved implementation to senders package
 *
 * Revision 1.1  2007/07/19 15:12:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Echos input to output. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version $Id$
 * @deprecated Please replace with nl.nn.adapterframework.senders.EchoSender
 */
public class EchoSender extends nl.nn.adapterframework.senders.EchoSender {

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}

}
