/*
   Copyright 2013 Nationale-Nederlanden, 2023-2025 WeAreFrank!

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
package org.frankframework.monitoring;

import org.frankframework.core.HasName;
import org.frankframework.core.NameAware;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.util.XmlBuilder;

/**
 * Interface to monitoring service.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@FrankDocGroup(FrankDocGroupValue.MONITORING)
public interface IMonitorDestination extends ConfigurableLifecycle, NameAware, HasName {

	void fireEvent(String monitorName, EventType eventType, Severity severity, String eventCode, MonitorEvent message);

	XmlBuilder toXml();
}
