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
 * $Log: ConfigurationException.java,v $
 * Revision 1.5  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2004/03/30 07:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2004/03/26 10:42:51  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception thrown by ConfigurationDigester and configure()-methods, signaling the configuration
 * did not succeed.
 * 
 * @version $Id$
 * @author Johan Verrips
 */
public class ConfigurationException extends IbisException {
public ConfigurationException(String msg) {
	super(msg);
}
public ConfigurationException(String msg, Throwable th) {
	super(msg,th);
	}
public ConfigurationException(Throwable e) {
	super(e);
}
}
