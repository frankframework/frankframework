/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.util.Map;

import org.springframework.beans.factory.DisposableBean;

import nl.nn.adapterframework.lifecycle.LazyLoadingEventListener;
import nl.nn.adapterframework.monitoring.events.FireMonitorEvent;
import nl.nn.adapterframework.util.XmlBuilder;

public interface ITrigger extends LazyLoadingEventListener<FireMonitorEvent>, DisposableBean {
	boolean isAlarm();
	void clearEvents();
	boolean isConfigured();
	void configure();
	void toXml(XmlBuilder monitor);
	String getType();
	String[] getEventCodes();
	SeverityEnum getSeverityEnum();
	String getSeverity();
	int getThreshold();
	int getPeriod();
	Map<String, AdapterFilter> getAdapterFilters();
	SourceFiltering getSourceFilteringEnum();
	String getSourceFiltering();
	void setEventCodes(String[] arr);
	void setType(String type);
	void setSeverityEnum(SeverityEnum enumeration);
	void setThreshold(int i);
	void setPeriod(int i);
	void clearAdapterFilters();
	void setSourceFilteringEnum(SourceFiltering filtering);
	void registerAdapterFilter(AdapterFilter af);
	void setMonitor(Monitor monitor);
	void setAlarm(boolean b);
}
