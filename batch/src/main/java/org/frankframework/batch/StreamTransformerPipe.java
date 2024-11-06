/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.core.SenderException;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

/**
 * Pipe for transforming a stream with records. Records in the stream must be separated with new line characters.
 *
 * For file containing only a single type of lines, a simpler configuration without managers and flows
 * can be specified. A single recordHandler with key="*" and (optional) a single resultHandler need to be specified.
 * Each line will be handled by this recordHandler and resultHandler.
 *
 * @author John Dekker / Gerrit van Brakel
 * @since   4.7
 * @deprecated Warning: non-maintained functionality.
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class StreamTransformerPipe extends FixedForwardPipe {

	public static final String originalBlockKey = "originalBlock";

	private @Getter boolean storeOriginalBlock = false;
	private @Getter boolean closeInputstreamOnExit = true;
	private @Getter String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

	private IRecordHandlerManager initialManager = null;
	private IResultHandler defaultHandler = null;
	private final Map<String,IRecordHandlerManager> registeredManagers = new HashMap<>();
	private final Map<String,IRecordHandler> registeredRecordHandlers = new HashMap<>();
	private final Map<String,IResultHandler> registeredResultHandlers = new LinkedHashMap<>();

	private @Getter IReaderFactory readerFactory = new InputStreamReaderFactory();

	protected String getStreamId(Message input, PipeLineSession session) {
		return session.getCorrelationId();
	}

	/*
	 * obtain data inputstream.
	 */
	protected InputStream getInputStream(String streamId, Message input, PipeLineSession session) throws PipeRunException {
		try {
			return input.asInputStream();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
	}

	/*
	 * method called by doPipe to obtain reader.
	 */
	protected BufferedReader getReader(String streamId, Message input, PipeLineSession session) throws PipeRunException {
		try {
			Reader reader = getReaderFactory().getReader(getInputStream(streamId, input, session), getCharset(), streamId, session);
			if (reader instanceof BufferedReader bufferedReader) {
				return bufferedReader;
			}
			return new BufferedReader(reader);
		} catch (SenderException e) {
			throw new PipeRunException(this, "cannot create reader", e);
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (registeredManagers.isEmpty()) {
			log.info("creating default manager");
			IRecordHandlerManager manager = new RecordHandlerManager();
			manager.setInitial(true);
			manager.setName("default");
			RecordHandlingFlow flow = new RecordHandlingFlow();
			flow.setRecordKey("*");
			for(String recordHandlerName: registeredRecordHandlers.keySet()) {
				flow.setRecordHandlerRef(recordHandlerName);
			}
			for (String resultHandlerName: registeredResultHandlers.keySet()) {
				flow.setResultHandlerRef(resultHandlerName);
			}
			manager.addHandler(flow);
			try {
				addManager(manager);
			} catch (Exception e) {
				throw new ConfigurationException("could not register default manager and flow", e);
			}
		}
		if (initialManager == null) {
			throw new ConfigurationException("no initial manager specified");
		}
		for (String managerName: registeredManagers.keySet()) {
			IRecordHandlerManager manager = getManager(managerName);
			manager.configure(registeredManagers, registeredRecordHandlers, registeredResultHandlers, defaultHandler);
		}
		for (String recordHandlerName: registeredRecordHandlers.keySet()) {
			IRecordHandler handler = getRecordHandler(recordHandlerName);
			handler.configure();
		}
		for (String resultHandlerName: registeredResultHandlers.keySet()) {
			IResultHandler handler = getResultHandler(resultHandlerName);
			handler.configure();
		}
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		for (String recordHandlerName: registeredRecordHandlers.keySet()) {
			IRecordHandler handler = getRecordHandler(recordHandlerName);
			try {
				handler.open();
			} catch (SenderException e) {
				throw new PipeStartException("cannot start recordhandler ["+recordHandlerName+"]", e);
			}
		}
		for (String resultHandlerName: registeredResultHandlers.keySet()) {
			IResultHandler handler = getResultHandler(resultHandlerName);
			try {
				handler.open();
			} catch (SenderException e) {
				throw new PipeStartException("cannot start resulthandler ["+resultHandlerName+"]", e);
			}
		}
	}

	@Override
	public void stop() {
		super.stop();
		for (String recordHandlerName: registeredRecordHandlers.keySet()) {
			IRecordHandler handler = getRecordHandler(recordHandlerName);
			try {
				handler.close();
			} catch (SenderException e) {
				log.error("exception on closing recordhandler [{}]", recordHandlerName, e);
			}
		}
		for (String resultHandlerName: registeredResultHandlers.keySet()) {
			IResultHandler handler = getResultHandler(resultHandlerName);
			try {
				handler.close();
			} catch (SenderException e) {
				log.error("exception on closing resulthandler [{}]", resultHandlerName, e);
			}
		}
	}

	/**
	 * Register a uniquely named manager.
	 * @deprecated please use addManager
	 */
	@Deprecated
	public void addChild(IRecordHandlerManager manager) throws Exception {
		ConfigurationWarnings.add(this, log, "configuration using element 'child' is deprecated. Please use element 'manager'", SuppressKeys.DEPRECATION_SUPPRESS_KEY, getAdapter());
		addManager(manager);
	}

	/**
	 * Manager determines which handlers are to be used for the current line. If no manager is specified, a default manager and flow are created.
	 * The default manager always uses the default flow. The default flow always uses the first registered recordHandler (if available)
	 * and the first registered resultHandler (if available).
	 * @ff.mandatory
	 */
	public void addManager(IRecordHandlerManager manager) throws Exception {
		registeredManagers.put(manager.getName(), manager);
		if (manager.isInitial()) {
			if (initialManager != null) {
				throw new ConfigurationException("manager ["+manager.getName()+"] has initial=true, but initial manager already set to ["+initialManager.getName()+"]");
			}
			initialManager = manager;
		}
	}

	public IRecordHandlerManager getManager(String name) {
		return registeredManagers.get(name);
	}

	/**
	 * Register a flow element that contains the handlers for a specific record type (key)
	 * @deprecated please use manager.addFlow()
	 */
	@Deprecated
	public void addChild(RecordHandlingFlow flowEl) throws ConfigurationException {
		ConfigurationWarnings.add(this, log, "configuration using element 'child' is deprecated. Please use element 'flow' nested in element 'manager'", SuppressKeys.DEPRECATION_SUPPRESS_KEY, getAdapter());
		IRecordHandlerManager manager = registeredManagers.get(flowEl.getRecordHandlerManagerRef());
		if (manager == null) {
			throw new ConfigurationException("RecordHandlerManager [" + flowEl.getRecordHandlerManagerRef() + "] not found. Manager must be defined before the flows it contains");
		}
		// register the flow with the manager
		manager.addHandler(flowEl);
	}

	/**
	 * Register a uniquely named record manager.
	 * @deprecated please use addRecordHandler()
	 */
	@Deprecated
	public void addChild(IRecordHandler handler) {
		ConfigurationWarnings.add(this, log, "configuration using element 'child' is deprecated. Please use element 'recordHandler'", SuppressKeys.DEPRECATION_SUPPRESS_KEY, getAdapter());
		addRecordHandler(handler);
	}

	/** Handler for transforming records of a specific type */
	public void addRecordHandler(IRecordHandler handler) {
		registeredRecordHandlers.put(handler.getName(), handler);
	}

	public IRecordHandler getRecordHandler(String name) {
		return registeredRecordHandlers.get(name);
	}

	/**
	 * Register a uniquely named result manager.
	 * @deprecated Please use addResultHandler()
	 */
	@Deprecated
	public void addChild(IResultHandler handler) {
		ConfigurationWarnings.add(this, log, "configuration using element 'child' is deprecated. Please use element 'resultHandler'", SuppressKeys.DEPRECATION_SUPPRESS_KEY, getAdapter());
		addResultHandler(handler);
	}

	/** Handler for processing transformed records */
	public void addResultHandler(IResultHandler handler) {
		handler.setPipe(this);
		registeredResultHandlers.put(handler.getName(), handler);
		if (handler.isDefault()) {
			defaultHandler = handler;
		}
	}

	public IResultHandler getResultHandler(String name) {
		return registeredResultHandlers.get(name);
	}

	/**
	 * Open a reader for the file named according the input messsage and transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files.
	 *
	 * @see IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		String streamId = getStreamId(input, session);
		BufferedReader reader = getReader(streamId, input, session);
		if (reader==null) {
			throw new PipeRunException(this,"could not obtain reader for ["+streamId+"]");
		}
		String transformationResult=null;
		try {
			Map<String, Object> blocks = new HashMap<>();
			transformationResult = transform(streamId, reader, session, blocks);
		} finally {
			if (isCloseInputstreamOnExit()) {
				try {
					reader.close();
				} catch (IOException e) {
					log.warn("Exception closing reader",e);
				}
			}
		}
		return new PipeRunResult(getSuccessForward(),transformationResult);
	}

	private List<String> getBlockStack(IResultHandler handler, String streamId, boolean create, Map<String, Object> blocks) {
		String blockStackKey="blockStack for "+handler.getName();
		List<String> list = (List<String>)blocks.get(blockStackKey);
		if (list==null) {
			if (create) {
				list=new ArrayList<>();
				blocks.put(blockStackKey,list);
			}
		}
		return list;
	}

	private List<String> getBlockStack(IResultHandler handler, String streamId, Map<String, Object> blocks) {
		return getBlockStack(handler, streamId, false, blocks);
	}

	private boolean autoCloseBlocks(PipeLineSession session, IResultHandler handler, String streamId, RecordHandlingFlow flow, String blockName, Map<String, Object> blocks) throws Exception {
		List<String> blockStack=getBlockStack(handler, streamId, true, blocks);
		int blockLevel;
		if (log.isTraceEnabled()) log.trace("searching block stack for open block [{}] to perform autoclose", blockName);
		for (blockLevel=blockStack.size()-1;blockLevel>=0; blockLevel--) {
			String stackedBlock=blockStack.get(blockLevel);
			if (log.isTraceEnabled()) log.trace("stack position [{}] block [{}]", blockLevel, stackedBlock);
			if (stackedBlock.equals(blockName)) {
				break;
			}
		}
		if (blockLevel>=0) {
			if (log.isTraceEnabled()) log.trace("found open block [{}] at stack position [{}]", blockName, blockLevel);
			for (int i=blockStack.size()-1; i>=blockLevel; i--) {
				String stackedBlock=blockStack.remove(i);
				closeBlock(session, handler, streamId,null,stackedBlock, "autoclose of previous blocks while opening block ["+blockName+"]", blocks);
			}
			return true;
		}
		if (log.isTraceEnabled()) log.trace("did not find open block [{}] at block stack", blockName);
		return false;
	}

	private void openBlock(PipeLineSession session, IResultHandler handler, String streamId, RecordHandlingFlow flow, String blockName, Map<String, Object> blocks) throws Exception {
		if (StringUtils.isNotEmpty(blockName)) {
			if (handler!=null) {
				if (flow.isAutoCloseBlock()) {
					autoCloseBlocks(session, handler, streamId,flow, blockName, blocks);
					List<String> blockStack=getBlockStack(handler, streamId, true, blocks);
					if (log.isTraceEnabled()) log.trace("adding block [{}] to block stack at position [{}]", blockName, blockStack.size());
					blockStack.add(blockName);
				}
				if (log.isTraceEnabled()) log.trace("opening block [{}] for resultHandler [{}]", blockName, handler.getName());
				handler.openBlock(session, streamId, blockName, blocks);
			} else {
				log.warn("openBlock({}) without resultHandler", blockName);
			}
		}
	}

	private void closeBlock(PipeLineSession session, IResultHandler handler, String streamId, RecordHandlingFlow flow, String blockName, String reason, Map<String, Object> blocks) throws Exception {
		if (StringUtils.isNotEmpty(blockName)) {
			if (handler!=null) {
				if (flow!=null && flow.isAutoCloseBlock()) {
					if (autoCloseBlocks(session, handler, streamId, flow, blockName, blocks)) {
						if (log.isDebugEnabled()) log.debug("autoclosed block [{}] due to {}", blockName, reason);
					} else {
						if (log.isDebugEnabled()) log.debug("autoclose did not find block [{}] to close due to {}", blockName, reason);
					}
				} else {
					if (log.isDebugEnabled()) log.debug("closing block [{}] for resultHandler [{}] due to {}", blockName, handler.getName(), reason);
					if (isStoreOriginalBlock()) {
						if (handler instanceof ResultBlock2Sender sender) {
							if (sender.getLevel(streamId)==1) {
								session.put(originalBlockKey, blocks.remove(originalBlockKey));
							}
						}
					}
					handler.closeBlock(session, streamId, blockName, blocks);
				}
			} else {
				log.warn("closeBlock({}) without resultHandler", blockName);
			}
		}
	}

	protected void closeAllBlocks(PipeLineSession session, String streamId, IResultHandler handler, Map<String, Object> blocks) throws Exception {
		if (handler!=null) {
			List<String> blockStack=getBlockStack(handler, streamId, blocks);
			if (blockStack!=null) {
				for (int i=blockStack.size()-1; i>=0; i--) {
					String stackedBlock=blockStack.remove(i);
					closeBlock(session,handler,streamId,null,stackedBlock,"closeAllBlocks",blocks);
				}
			}
		}
	}

	/*
	 * Read all lines from the reader, treat every line as a record and transform
	 * it using the registered managers, record- and result handlers.
	 */
	private String transform(String streamId, BufferedReader reader, PipeLineSession session, Map<String, Object> blocks) throws PipeRunException {
		String rawRecord = null;
		int linenumber = 0;
		int counter = 0;
		StringBuilder sb = null;
		List<String> prevParsedRecord = null;
		IRecordHandler prevHandler = null;

		IRecordHandlerManager currentManager = initialManager.getRecordFactoryUsingFilename(session, streamId);
		try {
			openDocument(session, streamId);
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
					log.debug("<no flow>: {}", rawRecord);
					continue; // ignore line for which no handlers are registered
				}
				//log.debug("flow ["+flow.getRecordKey()+"] openBlockBeforeLine ["+flow.getOpenBlockBeforeLine()+"]");
				IResultHandler resultHandler = flow.getResultHandler();
				closeBlock(session, resultHandler, streamId, flow, flow.getCloseBlockBeforeLine(),"closeBlockBeforeLine of flow ["+flow.getRecordKey()+"]",blocks);
				String obbl = null;
				if (flow.getOpenBlockBeforeLineNumber()>0) {
					if (counter%flow.getOpenBlockBeforeLineNumber()==0) {
						obbl = flow.getOpenBlockBeforeLine();
					}
				} else {
					obbl = flow.getOpenBlockBeforeLine();
				}
				openBlock(session, resultHandler, streamId, flow, obbl, blocks);

				if (isStoreOriginalBlock()) {
					if (resultHandler instanceof ResultBlock2Sender) {
						// If blocks does not contain a previous block, it never existed, or has been removed by closing the block.
						// In both cases a new block has just started
						if (!blocks.containsKey(originalBlockKey)) {
							sb = new StringBuilder();
						}
						if (sb.length()>0) {
							sb.append(System.getProperty("line.separator"));
						}
						sb.append(rawRecord);
						// already put the block in the blocks, also if the block is not yet complete.
						blocks.put(originalBlockKey, sb.toString());
					}
				}

				IRecordHandler curHandler = flow.getRecordHandler();
				if (curHandler != null) {
					if (log.isDebugEnabled())
						log.debug("manager [{}] key [{}] record handler [{}] line [{}] record [{}]", currentManager.getName(), flow.getRecordKey(), curHandler.getName(), linenumber, rawRecord);
					// there is a record handler, so transform the line
					List<String> parsedRecord = curHandler.parse(session, rawRecord);
					String result = curHandler.handleRecord(session, parsedRecord);
					counter++;

					// if there is a result handler, write the transformed result
					if (result != null && resultHandler != null) {
						boolean recordTypeChanged = curHandler.isNewRecordType(session, curHandler.equals(prevHandler), prevParsedRecord, parsedRecord);
						if (log.isTraceEnabled())
							log.trace("manager [{}] key [{}] record handler [{}] recordTypeChanged [{}]", currentManager.getName(), flow.getRecordKey(), curHandler.getName(), recordTypeChanged);
						if (recordTypeChanged && prevHandler!=null && resultHandler.isBlockByRecordType()) {
							String prevRecordType = prevHandler.getRecordType(prevParsedRecord);
							if (log.isDebugEnabled())
								log.debug("record handler [{}] result handler [{}] closing block for record type [{}]", prevHandler.getName(), resultHandler.getName(), prevRecordType);
							closeBlock(session, resultHandler, streamId, flow, prevRecordType, "record type change", blocks);
						}
						// the hasPrefix() call allows users use a suffix without a prefix.
						// The suffix is then only written at the end of the file.
						if (recordTypeChanged && resultHandler.hasPrefix()) {
							if (prevHandler != null)  {
								resultHandler.closeRecordType(session, streamId);
							}
							resultHandler.openRecordType(session, streamId);
						}
						if (recordTypeChanged && resultHandler.isBlockByRecordType()) {
							String recordType = curHandler.getRecordType(parsedRecord);
							if (log.isDebugEnabled())
								log.debug("record handler [{}] result handler [{}] opening block [{}]", curHandler.getName(), resultHandler.getName(), recordType);
							openBlock(session, resultHandler, streamId, flow, recordType, blocks);
						}
						resultHandler.handleResult(session, streamId, flow.getRecordKey(), result);
					}
					prevParsedRecord = parsedRecord;
					prevHandler = curHandler;
				} else {
					if (log.isDebugEnabled())
						log.debug("manager [{}] key [{}], no record handler, line [{}] record [{}]", currentManager.getName(), flow.getRecordKey(), linenumber, rawRecord);
				}

				closeBlock(session, resultHandler, streamId, flow, flow.getCloseBlockAfterLine(),"closeBlockAfterLine of flow ["+flow.getRecordKey()+"]", blocks);
				openBlock(session, resultHandler, streamId, flow, flow.getOpenBlockAfterLine(), blocks);

				// get the manager for the next record
				currentManager = flow.getNextRecordHandlerManager();
			}
			return finalizeResult(session, streamId, false, blocks);
		} catch(Exception e) {
			try {
				finalizeResult(session, streamId, true, blocks);
			} catch(Throwable t) {
				log.error("Unexpected error during finalizeResult of [{}]", streamId, t);
			}
			throw new PipeRunException(this, "Error while transforming [" + streamId + "] at or after line [" + linenumber+"]", e);
		} finally {
			closeDocument(session,streamId);
		}
	}

	private void openDocument(PipeLineSession session, String inputFilename) throws Exception {
		for (IResultHandler resultHandler: registeredResultHandlers.values()) {
			resultHandler.openDocument(session, inputFilename);
		}
	}

	private void closeDocument(PipeLineSession session, String inputFilename) {
		for (IResultHandler resultHandler: registeredResultHandlers.values()) {
			resultHandler.closeDocument(session, inputFilename);
		}
	}

	/**
	 * finalizeResult is called when all records in the input file are handled
	 * and gives the resulthandlers a chance to finalize.
	 * @param blocks
	 */
	private String finalizeResult(PipeLineSession session, String inputFilename, boolean error, Map<String, Object> blocks) throws Exception {
		// finalize result
		List<String> results = new ArrayList<>();
		for (IResultHandler resultHandler: registeredResultHandlers.values()) {
			resultHandler.closeRecordType(session, inputFilename);
			closeAllBlocks(session, inputFilename, resultHandler, blocks);
			log.debug("finalizing resulthandler [{}]", resultHandler.getName());
			String result = resultHandler.finalizeResult(session, inputFilename, error);
			if (result != null) {
				results.add(result);
			}
		}
		return String.join(";" , results);
	}

	/**
	 * If set <code>true</code> the original block is stored under the session key <code>originalBlock</code>.
	 * @ff.default false
	 */
	public void setStoreOriginalBlock(boolean b) {
		storeOriginalBlock = b;
	}

	/**
	 * If set to <code>false</code>, the inputstream is not closed after it has been used.
	 * @ff.default true
	 */
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}

	/**
	 * Characterset used for reading file or inputstream"
	 * @ff.default UTF-8
	 */
	public void setCharset(String string) {
		charset = string;
	}

	/** Factory for the <code>reader</code>. The default implementation {@link InputStreamReaderFactory} converts using the specified character set. */
	public void setReaderFactory(IReaderFactory factory) {
		readerFactory = factory;
	}
}
