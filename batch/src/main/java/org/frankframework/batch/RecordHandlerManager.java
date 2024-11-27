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
package org.frankframework.batch;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.util.LogUtil;

/**
 * Basic implementation of RecordHandlerManager, that allows only for a single flow.
 * The manager decides which handlers to be used for a specific record.
 *
 *
 * @author  John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
public class RecordHandlerManager implements IRecordHandlerManager {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter String name;
	private @Getter boolean initial;

	private final Map<String, RecordHandlingFlow> flowMap = new LinkedHashMap<>();

	@Override
	public IRecordHandlerManager getRecordFactoryUsingFilename(PipeLineSession session, String inputFilename) {
		return this;
	}

	@Override
	public void configure(Map<String, IRecordHandlerManager> registeredManagers, Map<String, IRecordHandler> registeredRecordHandlers, Map<String, IResultHandler> registeredResultHandlers, IResultHandler defaultHandler) throws ConfigurationException {
		for(String flowName: flowMap.keySet()) {
			RecordHandlingFlow flow = getFlowByName(flowName);
			flow.configure(this, registeredManagers, registeredRecordHandlers, registeredResultHandlers, defaultHandler);
		}
	}

	private RecordHandlingFlow getFlowByName(String flowName) {
		return flowMap.get(flowName);
	}

	/** Element that contains the handlers for a specific record type, to be assigned to the manager */
	@Override
	public void addHandler(RecordHandlingFlow handlers) {
		flowMap.put(handlers.getRecordKey(), handlers);
		if (handlers.getNextRecordHandlerManager() == null) {
			handlers.setNextRecordHandlerManager(this);
		}
	}

	protected Map<String,RecordHandlingFlow> getFlowMap() {
		return flowMap;
	}

	@Override
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
		return flowMap.get("*");
	}

	/**
	 * Determines the recordhandler to use, based on key.
	 * Key is "*" by default, but can be changed by descendant implementations.
	 *
	 * @return RecordHandlingFlow element to be used for handling records of type recordkey
	 */
	public RecordHandlingFlow getRecordHandlerByKey(String recordKey) throws Exception {
		RecordHandlingFlow rhf =flowMap.get(recordKey);
		if (rhf == null) {
			rhf =flowMap.get("*");
			if (rhf == null) {
				throw new Exception("No handlers (flow) found for recordKey [" + recordKey + "]");
			}
		}
		return rhf;
	}

	/** Name of the manager */
	@Override
	public void setName(String string) {
		name = string;
	}

	/**
	 * This manager is the initial manager, i.e. to be used for the first record
	 * @ff.default false
	 */
	@Override
	public void setInitial(boolean b) {
		initial = b;
	}

}
