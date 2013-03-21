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
/*
 * $Log: RecordHandlingFlow.java,v $
 * Revision 1.17  2012-04-03 08:13:12  europe\m168309
 * added ConfigurationException for openBlockBeforeLineNumber
 *
 * Revision 1.16  2011/11/30 13:51:56  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.14  2011/10/10 12:49:30  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added openBlockBeforeLineNumber attribute
 *
 * Revision 1.13  2008/03/27 10:53:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * javadoc for autoclose block on forced close, too
 *
 * Revision 1.12  2008/03/20 11:57:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.11  2007/10/08 12:14:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.10  2007/09/24 13:02:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.9  2007/09/19 11:17:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added block handling functions
 *
 * Revision 1.8  2007/09/13 12:37:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in configuration
 *
 * Revision 1.7  2007/08/03 08:28:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.6  2007/07/24 16:12:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved configure to flow
 *
 * Revision 1.5  2007/07/24 08:03:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reformatted code
 *
 * Revision 1.4  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * The flow contains the handlers to handle records of a specific type. 
 * Each flow is registered to a manager using the recordHandlerManagerRef.
 *  
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordHandlingFlow</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordHandlerManagerRef(String) recordHandlerManagerRef}</td><td>Name of the manager to which this RecordHandlingFlow must be added</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordKey(String) recordKey}</td><td>Key under which this RecordHandlingFlow must be registered in the manager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordHandlerRef(String) recordHandlerRef}</td><td>Name of the recordhandler to be used to transform records of the type specified in the key (optional)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResultHandlerRef(String) resultHandlerRef}</td><td>Name of the resulthandler to be used to handle the transformed result</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNextRecordHandlerManagerRef(String) nextRecordHandlerManagerRef}</td><td>Name of the manager to be used after handling this record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOpenBlockBeforeLine(String) openBlockBeforeLine}</td><td>instructs the resultHandler to start a new block before the parsed line is processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOpenBlockBeforeLineNumber(int) openBlockBeforeLineNumber}</td><td>when &gt;0 the <code>openBlockBeforeLine</code> instruction is only performed when the current line number is a multiple of this value</td><td>0</td></tr>
 * <tr><td>{@link #setCloseBlockBeforeLine(String) closeBlockBeforeLine}</td><td>instructs the resultHandler to end the specified block before the parsed line is processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOpenBlockAfterLine(String) openBlockAfterLine}</td><td>instructs the resultHandler to start a new block after the parsed line is processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCloseBlockAfterLine(String) closeBlockAfterLine}</td><td>instructs the resultHandler to end the specified block after the parsed line is processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAutoCloseBlock(boolean) autoCloseBlock}</td><td>when <code>true</code>, any open block of this type (and other nested open 'autoclose' block) is closed before a new one of the same type is opened. At a forced close, nested blocks are closed too (since 4.9)</td><td><code>true</code></td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version $Id$
 */
public final class RecordHandlingFlow {
	public static final String version = "$RCSfile: RecordHandlingFlow.java,v $  $Revision: 1.17 $ $Date: 2012-04-03 08:13:12 $";
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

	
	public void setRecordKey(String recordKey) {
		this.recordKey = recordKey;
	}
	public String getRecordKey() {
		return recordKey;
	}
	
	public void setNextRecordHandlerManagerRef(String nextRecordHandlerManagerName) {
		nextRecordHandlerManagerRef = nextRecordHandlerManagerName;
	}
	public String getNextRecordHandlerManagerRef() {
		return nextRecordHandlerManagerRef;
	}

	public void setRecordHandlerRef(String recordHandlerName) {
		recordHandlerRef = recordHandlerName;
	}
	public String getRecordHandlerRef() {
		return recordHandlerRef;
	}

	public void setRecordHandlerManagerRef(String recordHandlerManagerName) {
		recordHandlerManagerRef = recordHandlerManagerName;
	}
	public String getRecordHandlerManagerRef() {
		return recordHandlerManagerRef;
	}

	public void setResultHandlerRef(String resultHandlerName) {
		resultHandlerRef = resultHandlerName;
	}
	public String getResultHandlerRef() {
		return resultHandlerRef;
	}


	public void setOpenBlockBeforeLine(String blockName) {
		openBlockBeforeLine = blockName;
	}
	public String getOpenBlockBeforeLine() {
		return openBlockBeforeLine;
	}

	public void setCloseBlockBeforeLine(String blockName) {
		closeBlockBeforeLine = blockName;
	}
	public String getCloseBlockBeforeLine() {
		return closeBlockBeforeLine;
	}


	public void setOpenBlockAfterLine(String blockName) {
		openBlockAfterLine = blockName;
	}
	public String getOpenBlockAfterLine() {
		return openBlockAfterLine;
	}

	public void setCloseBlockAfterLine(String blockName) {
		closeBlockAfterLine = blockName;
	}
	public String getCloseBlockAfterLine() {
		return closeBlockAfterLine;
	}


	public void setAutoCloseBlock(boolean b) {
		autoCloseBlock = b;
	}
	public boolean isAutoCloseBlock() {
		return autoCloseBlock;
	}


	public void setOpenBlockBeforeLineNumber(int i) {
		openBlockBeforeLineNumber = i;
	}
	public int getOpenBlockBeforeLineNumber() {
		return openBlockBeforeLineNumber;
	}
}
