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

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/** 
 * @author  John Dekker
 */
@IbisDescription(
	"The flow contains the handlers to handle records of a specific type. " + 
	"Each flow is registered to a manager using the recordHandlerManagerRef." + 
	" " 
)
public final class RecordHandlingFlow {
	protected Logger log = LogUtil.getLogger(this);

	private String recordKey;
	private String recordHandlerRef;
	private String recordHandlerManagerRef;
	private String nextRecordHandlerManagerRef;
	private String resultHandlerRef;
	
	private String openBlockBeforeLine=null;
	private String closeBlockBeforeLine=null;
	private String openBlockAfterLine=null;
	private String closeBlockAfterLine=null;
	private boolean autoCloseBlock=true;
	private int openBlockBeforeLineNumber=0;
	
	private IRecordHandler recordHandler;
	private IRecordHandlerManager nextRecordHandlerManager;
	private IResultHandler resultHandler;
	
	public void configure(IRecordHandlerManager manager, Map registeredManagers, Map registeredRecordHandlers, Map registeredResultHandlers, IResultHandler defaultHandler) throws ConfigurationException {
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
			nextManager = (IRecordHandlerManager)registeredManagers.get(getNextRecordHandlerManagerRef());
			if (nextManager == null) {
				throw new ConfigurationException("cannot find nextRecordHandlerManager [" + getNextRecordHandlerManagerRef() + "] for flow of manager [" + getNextRecordHandlerManagerRef() + "], key ["+getRecordKey()+"]");
			}
		}
		setNextRecordHandlerManager(nextManager);
			
		// obtain the recordhandler 
		if (StringUtils.isNotEmpty(getRecordHandlerRef())) {
			IRecordHandler recordHandler = (IRecordHandler)registeredRecordHandlers.get(getRecordHandlerRef());
			if (recordHandler!=null) {
				setRecordHandler(recordHandler);
			} else {
				throw new ConfigurationException("cannot find recordhandler ["+getRecordHandlerRef()+"] for flow of manager [" + getNextRecordHandlerManagerRef() + "], key ["+getRecordKey()+"]");
			}
		} else {
			log.debug("no recordhandler defined for flow of manager [" + getNextRecordHandlerManagerRef() + "], key ["+getRecordKey()+"]");
		}
		
		// obtain the named resulthandler
		IResultHandler resultHandler = (IResultHandler)registeredResultHandlers.get(getResultHandlerRef());
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
	public IRecordHandler getRecordHandler() {
		return recordHandler;
	}

	public void setResultHandler(IResultHandler handler) {
		resultHandler = handler;
	}
	public IResultHandler getResultHandler() {
		return resultHandler;
	}

	public void setNextRecordHandlerManager(IRecordHandlerManager manager) {
		nextRecordHandlerManager = manager;
	}
	public IRecordHandlerManager getNextRecordHandlerManager() {
		return nextRecordHandlerManager;
	}

	
	@IbisDoc({"key under which this recordhandlingflow must be registered in the manager", ""})
	public void setRecordKey(String recordKey) {
		this.recordKey = recordKey;
	}
	public String getRecordKey() {
		return recordKey;
	}
	
	@IbisDoc({"name of the manager to be used after handling this record", ""})
	public void setNextRecordHandlerManagerRef(String nextRecordHandlerManagerName) {
		nextRecordHandlerManagerRef = nextRecordHandlerManagerName;
	}
	public String getNextRecordHandlerManagerRef() {
		return nextRecordHandlerManagerRef;
	}

	@IbisDoc({"name of the recordhandler to be used to transform records of the type specified in the key (optional)", ""})
	public void setRecordHandlerRef(String recordHandlerName) {
		recordHandlerRef = recordHandlerName;
	}
	public String getRecordHandlerRef() {
		return recordHandlerRef;
	}

	@IbisDoc({"name of the manager to which this recordhandlingflow must be added", ""})
	public void setRecordHandlerManagerRef(String recordHandlerManagerName) {
		recordHandlerManagerRef = recordHandlerManagerName;
	}
	public String getRecordHandlerManagerRef() {
		return recordHandlerManagerRef;
	}

	@IbisDoc({"name of the resulthandler to be used to handle the transformed result", ""})
	public void setResultHandlerRef(String resultHandlerName) {
		resultHandlerRef = resultHandlerName;
	}
	public String getResultHandlerRef() {
		return resultHandlerRef;
	}


	@IbisDoc({"instructs the resulthandler to start a new block before the parsed line is processed", ""})
	public void setOpenBlockBeforeLine(String blockName) {
		openBlockBeforeLine = blockName;
	}
	public String getOpenBlockBeforeLine() {
		return openBlockBeforeLine;
	}

	@IbisDoc({"instructs the resulthandler to end the specified block before the parsed line is processed", ""})
	public void setCloseBlockBeforeLine(String blockName) {
		closeBlockBeforeLine = blockName;
	}
	public String getCloseBlockBeforeLine() {
		return closeBlockBeforeLine;
	}


	@IbisDoc({"instructs the resulthandler to start a new block after the parsed line is processed", ""})
	public void setOpenBlockAfterLine(String blockName) {
		openBlockAfterLine = blockName;
	}
	public String getOpenBlockAfterLine() {
		return openBlockAfterLine;
	}

	@IbisDoc({"instructs the resulthandler to end the specified block after the parsed line is processed", ""})
	public void setCloseBlockAfterLine(String blockName) {
		closeBlockAfterLine = blockName;
	}
	public String getCloseBlockAfterLine() {
		return closeBlockAfterLine;
	}


	@IbisDoc({"when <code>true</code>, any open block of this type (and other nested open 'autoclose' block) is closed before a new one of the same type is opened. at a forced close, nested blocks are closed too (since 4.9)", "<code>true</code>"})
	public void setAutoCloseBlock(boolean b) {
		autoCloseBlock = b;
	}
	public boolean isAutoCloseBlock() {
		return autoCloseBlock;
	}


	@IbisDoc({"when &gt;0 the <code>openblockbeforeline</code> instruction is only performed when the current line number is a multiple of this value", "0"})
	public void setOpenBlockBeforeLineNumber(int i) {
		openBlockBeforeLineNumber = i;
	}
	public int getOpenBlockBeforeLineNumber() {
		return openBlockBeforeLineNumber;
	}
}
