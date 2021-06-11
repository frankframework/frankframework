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
package nl.nn.adapterframework.webcontrol.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.Trigger;

import org.apache.struts.action.DynaActionForm;



/**
 * Edit a Monitor - display the form.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class EditMonitor extends ShowMonitors {

	protected String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
		MonitorManager mm = getMonitorManager();
	
		if (index>=0) {
			Monitor monitor = mm.getMonitor(index);
			monitorForm.set("monitor",monitor);
		}
		
		return null;
	}

	/**
	 * set List of all adapters that are present in the FilterMap, to be called from client
	 */
	public void setAdapters(Trigger trigger, String[] arr) {
		log.debug(trigger.toString()+"setAdapters()");
		for (Iterator<String> it=trigger.getAdapterFilters().keySet().iterator(); it.hasNext();) {
			String adapterName=(String)it.next();
			boolean found=true;
			for(int i=0; i<arr.length; i++) {
				if (adapterName.equals(arr[i])) {
					break;
				}
				found=false;
			}
			if (!found) {
				log.debug(trigger.toString()+"setAdapters() removing adapter ["+adapterName+"] from filter");
				it.remove();
			}
		}
		for(int i=0; i<arr.length; i++) {
			String adapterName=arr[i];
			if (!trigger.getAdapterFilters().containsKey(adapterName)) {
				log.debug(trigger.toString()+"setAdapters() addding adapter ["+adapterName+"] to filter");
				AdapterFilter af=new AdapterFilter();
				af.setAdapter(adapterName);
				trigger.registerAdapterFilter(af);
			}
		}
	}


	/**
	 * get List of all adapters that are present in the FilterMap.
	 */
	protected String[] getAdapters(Trigger trigger) {
		String[] result=(String[])trigger.getAdapterFilters().keySet().toArray(new String[trigger.getAdapterFilters().size()]);
		if (log.isDebugEnabled()) {
			log.debug(trigger.toString()+"getAdapters() returns results:");
			for (int i=0; i<result.length; i++) {
				log.debug(trigger.toString()+"getAdapters() returns ["+ result[i]+"]");
			}
		}
		return result;
	}

	private List<EventThrowing> getEventSources(MonitorManager mm) {
		return new ArrayList<>();
	}

	/**
	 * set List of all throwers that can trigger this Trigger.
	 */
	protected void setSources(MonitorManager mm, Trigger trigger, String[] sourcesArr) {
		log.debug(trigger.toString()+"setSources()");
		List<EventThrowing> list= getEventSources(mm);
		log.debug(trigger.toString()+"setSources() clearing adapterFilter");
		trigger.getAdapterFilters().clear();
		for (int i=0; i<list.size();i++) {
			EventThrowing thrower=(EventThrowing)list.get(i);
			IAdapter adapter = thrower.getAdapter();
			String adaptername;
			String sourcename;
			if (adapter==null) {
				adaptername="-";
			} else {
				adaptername=adapter.getName();
			}
			sourcename=adaptername+" / "+thrower.getEventSourceName();
			for (int j=0; j<sourcesArr.length; j++) {
				if (sourcesArr[j].equals(sourcename)) {
					AdapterFilter af =trigger.getAdapterFilters().get(adaptername);
					if (af==null) {
						af = new AdapterFilter();
						af.setAdapter(adaptername);
						log.debug(trigger.toString()+"setSources() registered adapter ["+adaptername+"]");
						trigger.registerAdapterFilter(af);
					}
					af.registerSubObject(thrower.getEventSourceName());
					log.debug(trigger.toString()+"setSources() registered source ["+thrower.getEventSourceName()+"] on adapter ["+adapter.getName()+"]");
					break;
				}
			}
		}
	}

	protected List<EventThrowing> getSourceList(MonitorManager mm, Trigger trigger) {
		if (log.isDebugEnabled()) log.debug(trigger.toString()+"getSourceList() collecting sources:");
		List<EventThrowing> list=new ArrayList<EventThrowing>();
		for(Iterator adapterIterator=trigger.getAdapterFilters().entrySet().iterator();adapterIterator.hasNext();) {
			Map.Entry entry= (Map.Entry)adapterIterator.next();
			String adapterName=(String)entry.getKey();
			if (log.isDebugEnabled()) log.debug(trigger.toString()+"getSourceList() collecting sources for adapter ["+adapterName+"]:");
			AdapterFilter af=(AdapterFilter)entry.getValue();
			for(Iterator subSourceIterator=af.getSubObjectList().iterator();subSourceIterator.hasNext();) {
				String throwerName=(String)subSourceIterator.next();
//				EventThrowing thrower=mm.findThrowerByName(adapterName,throwerName);
//				if (log.isDebugEnabled()) log.debug(trigger.toString()+"getSourceList() adding source adapter ["+adapterName+"], source ["+thrower.getEventSourceName()+"]");
//				list.add(thrower);
			}
		}
		return list;
	}


	/**
	 * get List of all throwers that can trigger this Trigger.
	 */
	protected String[] getSources(Trigger trigger) {
		List<String> list=new ArrayList<String>();
		for(Iterator adapterIterator=trigger.getAdapterFilters().entrySet().iterator();adapterIterator.hasNext();) {
			Map.Entry entry= (Map.Entry)adapterIterator.next();
			String adapterName=(String)entry.getKey();
			AdapterFilter af=(AdapterFilter)entry.getValue();
			for(Iterator subSourceIterator=af.getSubObjectList().iterator();subSourceIterator.hasNext();) {
				String throwerName=(String)subSourceIterator.next();
				String sourceName=adapterName+" / "+throwerName;
				list.add(sourceName);
			}
		}
		String[] result=new String[list.size()];
		return result=(String[])list.toArray(result);
	}
}
