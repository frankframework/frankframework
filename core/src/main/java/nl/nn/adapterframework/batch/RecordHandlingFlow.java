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
package nl.nn.adapterframework.batch;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

/**
 * The flow contains the handlers to handle records of a specific type. 
 * Each flow is registered to a manager using the recordHandlerManagerRef.
 *  
 * 
 * @author  John Dekker
 */
public final class RecordHandlingFlow {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter String recordKey;
	private @Getter String recordHandlerRef;
	private @Getter String recordHandlerManagerRef;
	private @Getter String nextRecordHandlerManagerRef;
	private @Getter String resultHandlerRef;
	
	private @Getter String openBlockBeforeLine=null;
	private @Getter String closeBlockBeforeLine=null;
	private @Getter String openBlockAfterLine=null;
	private @Getter String closeBlockAfterLine=null;
	private @Getter boolean autoCloseBlock=true;
	private @Getter int openBlockBeforeLineNumber=0;
	
	private @Getter IRecordHandler recordHandler;
	private @Getter IRecordHandlerManager nextRecordHandlerManager;
	private @Getter IResultHandler resultHandler;
	
	public void configure(IRecordHandlerManager manager, Map<String,IRecordHandlerManager> registeredManagers, Map<String,IRecordHandler> registeredRecordHandlers, Map<String,IResultHandler> registeredResultHandlers, IResultHandler defaultHandler) throws ConfigurationException {
		if (getOpenBlockBeforeLineNumber()>0 && StringUtils.isEmpty(getOpenBlockBeforeLine())) {
					throw new ConfigurationException("openBlockBeforeLine must be set when openBlockBeforeLineNumber > 0");
			}

		if (StringUtils.isNotEmpty(getRecordHandlerManagerRef()) &&
			!getRecordHandlerManagerRef().equals(manager.getName())) {
				throw new ConfigurationException("recordHandlerManagerRef ["+getRecordHandlerManagerRef()+"] should be either equal to name of manager ["+manager.getName()+"], or left unspecified");
		}
		// obtain the named manager that is to be used after a specified record  
		IRecordHandlerManager nextManager = null;
		if (StringUtils.isEmpty(getNextRecordHandlerManagerRef())) {
			nextManager = manager; 
		} else { 
			nextManager = registeredManagers.get(getNextRecordHandlerManagerRef());
			if (nextManager == null) {
				throw new ConfigurationException("cannot find nextRecordHandlerManager [" + getNextRecordHandlerManagerRef() + "] for flow of manager [" + getNextRecordHandlerManagerRef() + "], key ["+getRecordKey()+"]");
			}
		}
		setNextRecordHandlerManager(nextManager);
			
		// obtain the recordHandler 
		if (StringUtils.isNotEmpty(getRecordHandlerRef())) {
			IRecordHandler recordHandler = registeredRecordHandlers.get(getRecordHandlerRef());
			if (recordHandler!=null) {
				setRecordHandler(recordHandler);
			} else {
				throw new ConfigurationException("cannot find recordhandler ["+getRecordHandlerRef()+"] for flow of manager [" + getNextRecordHandlerManagerRef() + "], key ["+getRecordKey()+"]");
			}
		} else {
			log.debug("no recordhandler defined for flow of manager [" + getNextRecordHandlerManagerRef() + "], key ["+getRecordKey()+"]");
		}
		
		// obtain the named resultHandler
		IResultHandler resultHandler = registeredResultHandlers.get(getResultHandlerRef());
		if (resultHandler == null) {
			if (StringUtils.isEmpty(getResultHandlerRef())) {
				resultHandler = defaultHandler;
			} else {
				throw new ConfigurationException("ResultHandler [" + getResultHandlerRef() + "] not found");
			}
		}
		setResultHandler(resultHandler);
	}
	
	
	public void setRecordHandler(IRecordHandler handler) {
		recordHandler = handler;
	}

	public void setResultHandler(IResultHandler handler) {
		resultHandler = handler;
	}

	public void setNextRecordHandlerManager(IRecordHandlerManager manager) {
		nextRecordHandlerManager = manager;
	}

	
	@IbisDoc({"1", "Key under which this flow is registered in the manager", ""})
	public void setRecordKey(String recordKey) {
		this.recordKey = recordKey;
	}
	
	@IbisDoc({"2", "Name of the manager to be used after handling this record", ""})
	public void setNextRecordHandlerManagerRef(String nextRecordHandlerManagerName) {
		nextRecordHandlerManagerRef = nextRecordHandlerManagerName;
	}

	@IbisDoc({"3", "Name of the recordHandler to be used to transform records of the type specified in the key (optional)", ""})
	public void setRecordHandlerRef(String recordHandlerName) {
		recordHandlerRef = recordHandlerName;
	}

	@IbisDoc({"4", "Name of the manager to which this flow must be added", ""})
	public void setRecordHandlerManagerRef(String recordHandlerManagerName) {
		recordHandlerManagerRef = recordHandlerManagerName;
	}

	@IbisDoc({"5", "Name of the resultHandler to be used to handle the transformed result", ""})
	public void setResultHandlerRef(String resultHandlerName) {
		resultHandlerRef = resultHandlerName;
	}


	@IbisDoc({"6", "Instructs the resultHandler to start a new block before the parsed line is processed", ""})
	public void setOpenBlockBeforeLine(String blockName) {
		openBlockBeforeLine = blockName;
	}

	@IbisDoc({"7", "Instructs the resultHandler to end the specified block before the parsed line is processed", ""})
	public void setCloseBlockBeforeLine(String blockName) {
		closeBlockBeforeLine = blockName;
	}


	@IbisDoc({"8", "Instructs the resultHandler to start a new block after the parsed line is processed", ""})
	public void setOpenBlockAfterLine(String blockName) {
		openBlockAfterLine = blockName;
	}

	@IbisDoc({"9", "Instructs the resultHandler to end the specified block after the parsed line is processed", ""})
	public void setCloseBlockAfterLine(String blockName) {
		closeBlockAfterLine = blockName;
	}


	@IbisDoc({"10", "If <code>true</code>, any open block of this type (and other nested open 'autoClose' block) is closed before a new one of the same type is opened. At a forced close, nested blocks are closed too (since 4.9)", "<code>true</code>"})
	public void setAutoCloseBlock(boolean b) {
		autoCloseBlock = b;
	}


	@IbisDoc({"11", "If &gt;0 the <code>openBlockBeforeLine</code> instruction is only performed when the current line number is a multiple of this value", "0"})
	public void setOpenBlockBeforeLineNumber(int i) {
		openBlockBeforeLineNumber = i;
	}
}
