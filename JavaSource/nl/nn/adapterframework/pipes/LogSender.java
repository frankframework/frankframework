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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Sender that just logs its message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setLogLevel(String) logLevel}</td><td>level on which messages are logged</td><td>info</td></tr>
 * <tr><td>{@link #setLogCategory(String) logCategory}</td><td>category under which messages are logged</td><td>name of the sender</td></tr>
 * </table>
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 * @version $Id$
 * @deprecated Please replace with nl.nn.adapterframework.senders.LogSender
 */
public class LogSender extends nl.nn.adapterframework.senders.LogSender {

	public void configure() throws ConfigurationException {
		super.configure();
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
	}
}
