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
package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.FrankDocGroup;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Interface to monitoring service. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@FrankDocGroup(name = "Monitoring")
public interface IMonitorAdapter {

	void configure() throws ConfigurationException;

	void fireEvent(String subSource, EventType eventType, Severity severity, String message, Throwable t);

	public XmlBuilder toXml();

	void setName(String name);
	String getName();
}
