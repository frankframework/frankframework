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
package nl.nn.adapterframework.batch;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;


/** 
 * @author  John Dekker
 */
@IbisDescription(
	"Basic implementation of RecordHandlerManager, that allows only for a single flow." + 
	"The manager decides which handlers to be used for a specific record." 
)
public class RecordHandlerManager implements IRecordHandlerManager {
	protected Logger log = LogUtil.getLogger(this);

	private Map valueHandlersMap;
	private String name;
	private boolean initial;

	RecordHandlerManager() {
		this.valueHandlersMap = new LinkedHashMap();
	}
	
	public IRecordHandlerManager getRecordFactoryUsingFilename(IPipeLineSession session, String inputFilename) {
		return this;
	}

	public void configure(Map registeredManagers, Map registeredRecordHandlers, Map registeredResultHandlers, IResultHandler defaultHandler) throws ConfigurationException {
		for(Iterator it=valueHandlersMap.keySet().iterator();it.hasNext();) {
			String name=(String)it.next();
			RecordHandlingFlow flow = getFlowByName(name);
			flow.configure(this, registeredManagers, registeredRecordHandlers, registeredResultHandlers, defaultHandler);
		}
	}

	private RecordHandlingFlow getFlowByName(String name) {
		return (RecordHandlingFlow)valueHandlersMap.get(name);
	}
	
	public void addHandler(RecordHandlingFlow handlers) {
		valueHandlersMap.put(handlers.getRecordKey(), handlers);
		if (handlers.getNextRecordHandlerManager() == null) {
			handlers.setNextRecordHandlerManager(this);
		}
	}

	public Collection getRecordHandlers() {
		return valueHandlersMap.values();	
	}
	
	protected Map getValueHandlersMap() {
		return valueHandlersMap;	
	}
	
	public RecordHandlingFlow getRecordHandler(IPipeLineSession session, String record) throws Exception {
		return (RecordHandlingFlow)valueHandlersMap.get("*");
	}

	/**
	 * Determines the recordhandler to use, based on key.
	 * Key is "*" by default, but can be changed by descendant implementations.
	 *
	 * @return RecordHandlingFlow element to be used for handling records of type recordkey
	 */
	public RecordHandlingFlow getRecordHandlerByKey(String recordKey) throws Exception {
		RecordHandlingFlow rhf =(RecordHandlingFlow)valueHandlersMap.get(recordKey);
		if  (rhf == null) {
			rhf =(RecordHandlingFlow)valueHandlersMap.get("*");
			if  (rhf == null) {
				throw new Exception("No handlers (flow) found for recordKey [" + recordKey + "]");
			}
		}
		return rhf;
		
	}

	@IbisDoc({"name of the manager", ""})
	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	@IbisDoc({"this manager is the initial manager, i.e. to be used for the first record", "false"})
	public void setInitial(boolean b) {
		initial = b;
	}
	public boolean isInitial() {
		return initial;
	}


}
