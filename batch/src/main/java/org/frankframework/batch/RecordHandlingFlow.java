/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2024 WeAreFrank!

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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.util.LogUtil;

/**
 * The flow contains the handlers to handle records of a specific type.
 * Each flow is registered to a manager using the recordHandlerManagerRef.
 *
 * @author  John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
@FrankDocGroup(FrankDocGroupValue.BATCH)
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
			log.debug("no recordhandler defined for flow of manager [{}], key [{}]", getNextRecordHandlerManagerRef(), getRecordKey());
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

	/** Key under which this flow is registered in the manager */
	public void setRecordKey(String recordKey) {
		this.recordKey = recordKey;
	}

	/** Name of the manager to be used after handling this record */
	public void setNextRecordHandlerManagerRef(String nextRecordHandlerManagerName) {
		nextRecordHandlerManagerRef = nextRecordHandlerManagerName;
	}

	/** Name of the recordHandler to be used to transform records of the type specified in the key (optional) */
	public void setRecordHandlerRef(String recordHandlerName) {
		recordHandlerRef = recordHandlerName;
	}

	/** Name of the manager to which this flow must be added */
	public void setRecordHandlerManagerRef(String recordHandlerManagerName) {
		recordHandlerManagerRef = recordHandlerManagerName;
	}

	/** Name of the resultHandler to be used to handle the transformed result */
	public void setResultHandlerRef(String resultHandlerName) {
		resultHandlerRef = resultHandlerName;
	}


	/** Instructs the resultHandler to start a new block before the parsed line is processed */
	public void setOpenBlockBeforeLine(String blockName) {
		openBlockBeforeLine = blockName;
	}

	/** Instructs the resultHandler to end the specified block before the parsed line is processed */
	public void setCloseBlockBeforeLine(String blockName) {
		closeBlockBeforeLine = blockName;
	}


	/** Instructs the resultHandler to start a new block after the parsed line is processed */
	public void setOpenBlockAfterLine(String blockName) {
		openBlockAfterLine = blockName;
	}

	/** Instructs the resultHandler to end the specified block after the parsed line is processed */
	public void setCloseBlockAfterLine(String blockName) {
		closeBlockAfterLine = blockName;
	}


	/**
	 * If <code>true</code>, any open block of this type (and other nested open 'autoClose' block) is closed before a new one of the same type is opened. At a forced close, nested blocks are closed too (since 4.9)
	 * @ff.default true
	 */
	public void setAutoCloseBlock(boolean b) {
		autoCloseBlock = b;
	}


	/**
	 * If &gt;0 the <code>openBlockBeforeLine</code> instruction is only performed when the current line number is a multiple of this value
	 * @ff.default 0
	 */
	public void setOpenBlockBeforeLineNumber(int i) {
		openBlockBeforeLineNumber = i;
	}
}
