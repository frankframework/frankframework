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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.pipes.PipeAware;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for transforming a stream with records. Records in the stream must be separated
 * with new line characters.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.StreamTransformerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreOriginalBlock(boolean) storeOriginalBlock}</td><td>when set <code>true</code> the original block is stored under the session key originalBlock</td><td>false</td></tr>
 * <tr><td>{@link #setCloseInputstreamOnExit(boolean) closeInputstreamOnExit}</td><td>when set to <code>false</code>, the inputstream is not closed after it has been used</td><td>true</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>characterset used for reading file or inputstream</td><td>UTF-8</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IInputStreamReaderFactory readerFactory}</td><td>Factory for reader of inputstream. Default implementation {@link nl.nn.adapterframework.batch.InputStreamReaderFactory} just converts using the specified characterset</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line. 
 * 			If no manager is specified, a default manager and flow are created. The default manager 
 * 			always uses the default flow. The default flow always uses the first registered recordHandler 
 * 			(if available) and the first registered resultHandler (if available).</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.RecordHandlingFlow manager/flow}</td><td>Element that contains the handlers for a specific record type, to be assigned to the manager</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandler recordHandler}</td><td>Handler for transforming records of a specific type</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IResultHandler resultHandler}</td><td>Handler for processing transformed records</td></tr>
 * </table>
 * </p>
 * 
 * For file containing only a single type of lines, a simpler configuration without managers and flows
 * can be specified. A single recordHandler with key="*" and (optional) a single resultHandler need to be specified.
 * Each line will be handled by this recordHandler and resultHandler.
 * 
 * @author John Dekker / Gerrit van Brakel
 * @since   4.7
 */
public class StreamTransformerPipe extends FixedForwardPipe {

	public static final String originalBlockKey="originalBlock";

	private boolean storeOriginalBlock=false;
	private boolean closeInputstreamOnExit=true;
	private String charset=Misc.DEFAULT_INPUT_STREAM_ENCODING;

	private IRecordHandlerManager initialManager=null;
	private IResultHandler defaultHandler=null;
	private Map registeredManagers= new HashMap();
	private Map registeredRecordHandlers= new HashMap();
	private Map registeredResultHandlers= new LinkedHashMap();
	
	private IInputStreamReaderFactory readerFactory=new InputStreamReaderFactory();

	protected String getStreamId(Object input, IPipeLineSession session) throws PipeRunException {
		return session.getMessageId();
	}
	
	/*
	 * obtain data inputstream.
	 */
	protected InputStream getInputStream(String streamId, Object input, IPipeLineSession session) throws PipeRunException {
		return (InputStream)input;
	}
	/*
	 * method called by doPipe to obtain reader.
	 */
	protected BufferedReader getReader(String streamId, Object input, IPipeLineSession session) throws PipeRunException {
		try {
			Reader reader=getReaderFactory().getReader(getInputStream(streamId, input,session),getCharset(),streamId,session);
			if (reader instanceof BufferedReader) {
				return (BufferedReader)reader;
			}
			return new BufferedReader(reader);
		} catch (SenderException e) {
			throw new PipeRunException(this,getLogPrefix(session)+"cannot create reader",e);
		}
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (registeredManagers.size()==0) {
			log.info(getLogPrefix(null)+"creating default manager");
			IRecordHandlerManager manager = new RecordHandlerManager();
			manager.setInitial(true);
			manager.setName("default");
			RecordHandlingFlow flow = new RecordHandlingFlow();
			flow.setRecordKey("*");
			Iterator itrch = registeredRecordHandlers.keySet().iterator();
			if (itrch.hasNext()) {
				String recordHandlerName = (String)itrch.next();
				flow.setRecordHandlerRef(recordHandlerName);
			}
			Iterator itrsh = registeredResultHandlers.keySet().iterator();
			if (itrsh.hasNext()) {
				String resultHandlerName = (String)itrsh.next();
				flow.setResultHandlerRef(resultHandlerName);
			}
			manager.addHandler(flow);
			try {
				registerManager(manager);
			} catch (Exception e) {
				throw new ConfigurationException(getLogPrefix(null)+"could not register default manager and flow");
			}
		}
		if (initialManager==null) {
			throw new ConfigurationException(getLogPrefix(null)+"no initial manager specified");
		}
		for (Iterator it = registeredManagers.keySet().iterator(); it.hasNext();) {
			String managerName = (String)it.next();
			IRecordHandlerManager manager = getManager(managerName);
			manager.configure(registeredManagers, registeredRecordHandlers, registeredResultHandlers, defaultHandler);
		}
		for (Iterator it = registeredRecordHandlers.keySet().iterator(); it.hasNext();) {
			String recordHandlerName = (String)it.next();
			IRecordHandler handler = getRecordHandler(recordHandlerName);
			if(handler instanceof PipeAware) {
				((PipeAware)handler).setPipe(this);
			}
			handler.configure();
		}
		for (Iterator it = registeredResultHandlers.keySet().iterator(); it.hasNext();) {
			String resultHandlerName = (String)it.next();
			IResultHandler handler = getResultHandler(resultHandlerName);
			handler.configure();
		}
	}


	public void start() throws PipeStartException {
		super.start();
		for (Iterator it = registeredRecordHandlers.keySet().iterator(); it.hasNext();) {
			String recordHandlerName = (String)it.next();
			IRecordHandler handler = getRecordHandler(recordHandlerName);
			try {
				handler.open();
			} catch (SenderException e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start recordhandler ["+recordHandlerName+"]", e);
			}
		}
		for (Iterator it = registeredResultHandlers.keySet().iterator(); it.hasNext();) {
			String resultHandlerName = (String)it.next();
			IResultHandler handler = getResultHandler(resultHandlerName);
			try {
				handler.open();
			} catch (SenderException e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start resulthandler ["+resultHandlerName+"]", e);
			}
		}
	}
	public void stop() {
		super.stop();
		for (Iterator it = registeredRecordHandlers.keySet().iterator(); it.hasNext();) {
			String recordHandlerName = (String)it.next();
			IRecordHandler handler = getRecordHandler(recordHandlerName);
			try {
				handler.close();
			} catch (SenderException e) {
				log.error(getLogPrefix(null)+"exception on closing recordhandler ["+recordHandlerName+"]", e);
			}
		}
		for (Iterator it = registeredResultHandlers.keySet().iterator(); it.hasNext();) {
			String resultHandlerName = (String)it.next();
			IResultHandler handler = getResultHandler(resultHandlerName);
			try {
				handler.close();
			} catch (SenderException e) {
				log.error(getLogPrefix(null)+"exception on closing resulthandler ["+resultHandlerName+"]", e);
			}
		}
	}

	
	/**
	 * register a uniquely named manager.
	 * @param manager
	 * @throws Exception
	 * @deprecated please use registerManager
	 */
	public void registerChild(IRecordHandlerManager manager) throws Exception {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "configuration using element 'child' is deprecated. Please use element 'manager'";
		configWarnings.add(log, msg);
		registerManager(manager);
	}
	/**
	 * register a uniquely named manager.
	 * @param manager
	 * @throws Exception
	 */
	public void registerManager(IRecordHandlerManager manager) throws Exception {
		registeredManagers.put(manager.getName(), manager);
		if (manager.isInitial()) {
			if (initialManager != null) {
				throw new ConfigurationException("manager ["+manager.getName()+"] has initial=true, but initial manager already set to ["+initialManager.getName()+"]");
			}
			initialManager = manager;
		}
	}

	public IRecordHandlerManager getManager(String name) {
		return (IRecordHandlerManager)registeredManagers.get(name);
	}
	
	/**
	 * register a flow element that contains the handlers for a specific record type (key)
	 * @param flowEl
	 * @throws Exception
	 * @deprecated please use manager.addFlow()
	 */
	public void registerChild(RecordHandlingFlow flowEl) throws Exception {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "configuration using element 'child' is deprecated. Please use element 'flow' nested in element 'manager'";
		configWarnings.add(log, msg);
		IRecordHandlerManager manager = (IRecordHandlerManager)registeredManagers.get(flowEl.getRecordHandlerManagerRef());
		if (manager == null) {
			throw new ConfigurationException("RecordHandlerManager [" + flowEl.getRecordHandlerManagerRef() + "] not found. Manager must be defined before the flows it contains");
		}
		// register the flow with the manager
		manager.addHandler(flowEl);
	}
	
	/**
	 * register a uniquely named record manager.
	 * @param handler
	 * @throws Exception
	 * @deprecated please use registerRecordHandler()
	 */
	public void registerChild(IRecordHandler handler) throws Exception {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "configuration using element 'child' is deprecated. Please use element 'recordHandler'";
		configWarnings.add(log, msg);
		registerRecordHandler(handler);
	}
	/**
	 * register a uniquely named record manager.
	 * @param handler
	 * @throws Exception
	 */
	public void registerRecordHandler(IRecordHandler handler) throws Exception {
		registeredRecordHandlers.put(handler.getName(), handler);
	}
	public IRecordHandler getRecordHandler(String name) {
		return (IRecordHandler)registeredRecordHandlers.get(name);
	}


	
	/**
	 * register a uniquely named result manager.
	 * @param handler
	 * @throws Exception
	 * @deprecated Please use registerResultHandler()
	 */
	public void registerChild(IResultHandler handler) throws Exception {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "configuration using element 'child' is deprecated. Please use element 'resultHandler'";
		configWarnings.add(log, msg);
		registerResultHandler(handler);
	}
	/**
	 * register a uniquely named result manager.
	 * @param handler
	 * @throws Exception
	 */
	public void registerResultHandler(IResultHandler handler) throws Exception {
		handler.setPipe(this);
		registeredResultHandlers.put(handler.getName(), handler);
		if (handler.isDefault()) {
			defaultHandler = handler;
		}
	}
	public IResultHandler getResultHandler(String name) {
		return (IResultHandler)registeredResultHandlers.get(name);
	}
	
	
	
	/**
	 * Open a reader for the file named according the input messsage and transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files. 
	 * 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String streamId = getStreamId(input, session);
		BufferedReader reader = getReader(streamId, input,session);
		if (reader==null) {
			throw new PipeRunException(this,"could not obtain reader for ["+streamId+"]");
		}
		Object transformationResult=null;
		ParameterResolutionContext prc = new ParameterResolutionContext("", session);
		try {
			transformationResult = transform(streamId, reader, session, prc);
		} finally {
			if (isCloseInputstreamOnExit()) {
				try {
					reader.close();
				} catch (IOException e) {
					log.warn(getLogPrefix(session)+"Exception closing reader",e);
				}
			}
		}
		return new PipeRunResult(getForward(),transformationResult);
	}

	private List getBlockStack(IPipeLineSession session, IResultHandler handler, String streamId, boolean create) {
		String blockStackKey="blockStack for "+handler.getName();
		List list = (List)session.get(blockStackKey);
		if (list==null) {
			if (create) {
				list=new ArrayList();
				session.put(blockStackKey,list);
			}
		}
		return list;
	}

	private List getBlockStack(IPipeLineSession session, IResultHandler handler, String streamId) throws SenderException {
		return getBlockStack(session, handler, streamId, false);
	}

	private boolean autoCloseBlocks(IPipeLineSession session, IResultHandler handler, String streamId, RecordHandlingFlow flow, String blockName, ParameterResolutionContext prc) throws Exception {
		List blockStack=getBlockStack(session,handler, streamId, true);
		int blockLevel;
		if (log.isDebugEnabled()) log.debug("searching block stack for open block ["+blockName+"] to perform autoclose");
		for (blockLevel=blockStack.size()-1;blockLevel>=0; blockLevel--) {
			String stackedBlock=(String)blockStack.get(blockLevel);
			if (log.isDebugEnabled()) log.debug("stack position ["+blockLevel+"] block ["+stackedBlock+"]");
			if (stackedBlock.equals(blockName)) {
				break;
			}
		}
		if (blockLevel>=0) {
			if (log.isDebugEnabled()) log.debug("found open block ["+blockName+"] at stack position ["+blockLevel+"]");
			for (int i=blockStack.size()-1; i>=blockLevel; i--) {
				String stackedBlock=(String)blockStack.remove(i);
				closeBlock(session, handler, streamId,null,stackedBlock, "autoclose of previous blocks while opening block ["+blockName+"]", prc);
			}
			return true;
		} else {
			if (log.isDebugEnabled()) log.debug("did not found open block ["+blockName+"] at block stack");
			return false;
		}
	}

	private void openBlock(IPipeLineSession session, IResultHandler handler, String streamId, RecordHandlingFlow flow, String blockName, ParameterResolutionContext prc) throws Exception {
		if (StringUtils.isNotEmpty(blockName)) {
			if (handler!=null) {
				if (flow.isAutoCloseBlock()) {
					autoCloseBlocks(session, handler, streamId,flow, blockName, prc);
					List blockStack=getBlockStack(session, handler, streamId, true);
					if (log.isDebugEnabled()) log.debug("adding block ["+blockName+"] to block stack at position ["+blockStack.size()+"]");
					blockStack.add(blockName);
				}
				if (log.isDebugEnabled()) log.debug("opening block ["+blockName+"] for resultHandler ["+handler.getName()+"]");
				handler.openBlock(session, streamId, blockName, prc);
			} else {
				log.warn("openBlock("+blockName+") without resultHandler");
			}
		}
	}
	private void closeBlock(IPipeLineSession session, IResultHandler handler, String streamId, RecordHandlingFlow flow, String blockName, String reason, ParameterResolutionContext prc) throws Exception {
		if (StringUtils.isNotEmpty(blockName)) {
			if (handler!=null) {
				if (flow!=null && flow.isAutoCloseBlock()) {
					if (autoCloseBlocks(session, handler, streamId, flow, blockName, prc)) {
						if (log.isDebugEnabled()) log.debug("autoclosed block ["+blockName+"] due to "+reason);
					} else {
						if (log.isDebugEnabled()) log.debug("autoclose did not find block ["+blockName+"] to close due to "+reason);
					}
				} else {
					if (log.isDebugEnabled()) log.debug("closing block ["+blockName+"] for resultHandler ["+handler.getName()+"] due to "+reason);
					handler.closeBlock(session, streamId, blockName, prc);
					if (isStoreOriginalBlock()) {
						if (handler instanceof ResultBlock2Sender) {
							if (((ResultBlock2Sender)handler).getLevel(streamId)==0) {
								session.remove(originalBlockKey);
							}
						}
					}
				}
			} else {
				log.warn("closeBlock("+blockName+") without resultHandler");
			}
		}
	}

	protected void closeAllBlocks(IPipeLineSession session, String streamId, IResultHandler handler, ParameterResolutionContext prc) throws Exception {
		if (handler!=null) {
			List blockStack=getBlockStack(session, handler,streamId);
			if (blockStack!=null) {
				for (int i=blockStack.size()-1; i>=0; i--) {
					String stackedBlock=(String)blockStack.remove(i);
					closeBlock(session,handler,streamId,null,stackedBlock,"closeAllBlocks",prc);
				}
			}
		}
	}


	/*
	 * Read all lines from the reader, treat every line as a record and transform 
	 * it using the registered managers, record- and result handlers.
	 */	
	private Object transform(String streamId, BufferedReader reader, IPipeLineSession session, ParameterResolutionContext prc) throws PipeRunException {
		String rawRecord = null;
		int linenumber = 0;
		int counter = 0;
		StringBuffer sb = null;
		List prevParsedRecord = null; 
		IRecordHandler prevHandler = null;

		IRecordHandlerManager currentManager = initialManager.getRecordFactoryUsingFilename(session, streamId);
		try {
			openDocument(session,streamId, prc);
			while ((rawRecord = reader.readLine()) != null) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				linenumber++; // remember linenumber for exception handler
				if (StringUtils.isEmpty(rawRecord)) {
					continue; // ignore empty line
				}
				
				// get handlers for current line
				RecordHandlingFlow flow = currentManager.getRecordHandler(session, rawRecord);
				if (flow == null) {
					log.debug("<no flow>: "+rawRecord);
					continue; // ignore line for which no handlers are registered
				} else {
					//log.debug("flow ["+flow.getRecordKey()+"] openBlockBeforeLine ["+flow.getOpenBlockBeforeLine()+"]");
				}
				IResultHandler resultHandler = flow.getResultHandler();
				closeBlock(session, resultHandler, streamId, flow, flow.getCloseBlockBeforeLine(),"closeBlockBeforeLine of flow ["+flow.getRecordKey()+"]", prc);
				String obbl = null;
				if (flow.getOpenBlockBeforeLineNumber()>0) {
					if (counter%flow.getOpenBlockBeforeLineNumber()==0) {
						obbl = flow.getOpenBlockBeforeLine();
					}
				} else {
					obbl = flow.getOpenBlockBeforeLine();				
				}
				openBlock(session, resultHandler, streamId, flow, obbl, prc);

				if (isStoreOriginalBlock()) {
					if (resultHandler instanceof ResultBlock2Sender) {
						// If session does not contain a previous block, it never existed, or has been removed by closing the block.
						// In both cases a new block has just started
						if (!session.containsKey(originalBlockKey)) {
							sb = new StringBuffer();
						}
						if (sb.length()>0) {
							sb.append(System.getProperty("line.separator"));
						}
						sb.append(rawRecord);
						// already put the block in the session, also if the block is not yet complete.
						session.put(originalBlockKey, sb.toString());
					}
				}

				IRecordHandler curHandler = flow.getRecordHandler(); 
				if (curHandler != null) {
					log.debug("manager ["+currentManager.getName()+"] key ["+flow.getRecordKey()+"] record handler ["+curHandler.getName()+"] line ["+linenumber+"]: "+rawRecord);
					// there is a record handler, so transform the line
					List parsedRecord = curHandler.parse(session, rawRecord);
					Object result = curHandler.handleRecord(session, parsedRecord, prc);
					counter++;
				
					// if there is a result handler, write the transformed result
					if (result != null && resultHandler != null) {
						boolean recordTypeChanged = curHandler.isNewRecordType(session, curHandler.equals(prevHandler), prevParsedRecord, parsedRecord);
						log.debug("manager ["+currentManager.getName()+"] key ["+flow.getRecordKey()+"] record handler ["+curHandler.getName()+"] recordTypeChanged ["+recordTypeChanged+"]");
						if (recordTypeChanged && prevHandler!=null && resultHandler.isBlockByRecordType()) {
							String prevRecordType = prevHandler.getRecordType(prevParsedRecord);
							log.debug("record handler ["+prevHandler.getName()+"] result handler ["+resultHandler.getName()+"] closing block for record type ["+prevRecordType+"]");
							closeBlock(session, resultHandler, streamId, flow, prevRecordType, "record type change", prc);
						}
						// the hasPrefix() call allows users use a suffix without a prefix. 
						// The suffix is then only written at the end of the file.
						if (recordTypeChanged && resultHandler.hasPrefix()) {   
							if (prevHandler != null)  {
								resultHandler.closeRecordType(session, streamId, prc);
							}
							resultHandler.openRecordType(session, streamId, prc);
						}
						if (recordTypeChanged && resultHandler.isBlockByRecordType()) {
							String recordType = curHandler.getRecordType(parsedRecord);
							log.debug("record handler ["+curHandler.getName()+"] result handler ["+resultHandler.getName()+"] opening block ["+recordType+"]");
							openBlock(session, resultHandler, streamId, flow, recordType, prc);
						}
						resultHandler.handleResult(session, streamId, flow.getRecordKey(), result, prc);
					}
					prevParsedRecord = parsedRecord;
					prevHandler = curHandler;
				} else {
					log.debug("manager ["+currentManager.getName()+"] key ["+flow.getRecordKey()+"], no record handler: "+rawRecord);
				}
				
				closeBlock(session, resultHandler, streamId, flow, flow.getCloseBlockAfterLine(),"closeBlockAfterLine of flow ["+flow.getRecordKey()+"]", prc);
				openBlock(session, resultHandler, streamId, flow, flow.getOpenBlockAfterLine(), prc);
				
				// get the manager for the next record
				currentManager = flow.getNextRecordHandlerManager();
			}
			return finalizeResult(session, streamId, false, prc);
		} catch(Exception e) {
			try {
				finalizeResult(session, streamId, true, prc);
		 	} catch(Throwable t) {
				log.error("Unexpected error during finalizeResult of [" + streamId + "]", t);
			}
			throw new PipeRunException(this, "Error while transforming [" + streamId + "] at or after line [" + linenumber+"]", e);		
		} finally {
			closeDocument(session,streamId, prc);
		}
	}

	private void openDocument(IPipeLineSession session, String inputFilename, ParameterResolutionContext prc) throws Exception {
		for (Iterator it = registeredResultHandlers.values().iterator(); it.hasNext();) {
			IResultHandler resultHandler = (IResultHandler)it.next();
			resultHandler.openDocument(session, inputFilename, prc);
		}
	}
	private void closeDocument(IPipeLineSession session, String inputFilename, ParameterResolutionContext prc) {
		for (Iterator it = registeredResultHandlers.values().iterator(); it.hasNext();) {
			IResultHandler resultHandler = (IResultHandler)it.next();
			resultHandler.closeDocument(session, inputFilename, prc);
		}
	}
	
	/*
	 * finalizeResult is called when all records in the input file are handled
	 * and gives the resulthandlers a chance to finalize.
	 */	
	private Object finalizeResult(IPipeLineSession session, String inputFilename, boolean error, ParameterResolutionContext prc) throws Exception {
		// finalize result
		List results = new ArrayList();
		for (Iterator handlersIt = registeredResultHandlers.values().iterator(); handlersIt.hasNext();) {
			IResultHandler resultHandler = (IResultHandler)handlersIt.next();
			resultHandler.closeRecordType(session, inputFilename, prc);
			closeAllBlocks(session,inputFilename,resultHandler,prc);
			log.debug("finalizing resulthandler ["+resultHandler.getName()+"]");
			Object result = resultHandler.finalizeResult(session, inputFilename, error, prc);
			if (result != null) {
				results.add(result);
			}
		}
		return FileUtils.getNamesFromList(results, ';');
	}
	
	public void setStoreOriginalBlock(boolean b) {
		storeOriginalBlock = b;
	}
	public boolean isStoreOriginalBlock() {
		return storeOriginalBlock;
	}

	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	public void setReaderFactory(IInputStreamReaderFactory factory) {
		readerFactory = factory;
	}
	public IInputStreamReaderFactory getReaderFactory() {
		return readerFactory;
	}
}
