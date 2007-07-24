/*
 * $Log: StreamTransformerPipe.java,v $
 * Revision 1.1  2007-07-24 08:04:15  europe\L190409
 * first version, to be tested
 *
 */
package nl.nn.adapterframework.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for transforming a stream with records. Records in the stream must be separated
 * with new line characters.

 *  * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.BatchFileTransformerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #move2dirAfterTransform(String) directory}</td><td>Directory in which the transformed file(s) is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #move2dirAfterError(String) directory}</td><td>Directory to which the inputfile is moved in case an error occurs</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandler recordHandler}</td><td>Handler for transforming records of a specific type</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IResultHandler resultHandler}</td><td>Handler for processing transformed records</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.RecordHandlingFlow flow}</td><td>Element that contains the handlers for a specific record type, to be assigned to a manager</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class StreamTransformerPipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: StreamTransformerPipe.java,v $  $Revision: 1.1 $ $Date: 2007-07-24 08:04:15 $";

	private IRecordHandlerManager initialFactory;
	private IResultHandler defaultHandler;
	private HashMap registeredManagers;
	private HashMap registeredRecordHandlers;
	private HashMap registeredResultHandlers;
	private Collection registeredFlows;
	
	public StreamTransformerPipe() {
		this.registeredManagers = new HashMap();
		this.registeredRecordHandlers = new HashMap();
		this.registeredResultHandlers = new HashMap();
		this.registeredFlows = new LinkedList(); 
	}
	
	/**
	 * register a uniquely named manager.
	 * @param manager
	 * @throws Exception
	 * @deprecated please use registerManager
	 */
	public void registerChild(IRecordHandlerManager manager) throws Exception {
		log.warn("configuration using element 'child' is deprecated. Please use element 'manager'");
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
			initialFactory = manager;
		}
	}

	
	/**
	 * register a flow element that contains the handlers for a specific record type (key)
	 * @param flowEl
	 * @throws Exception
	 * @deprecated please use registerFlow()
	 */
	public void registerChild(RecordHandlingFlow flowEl) throws Exception {
		log.warn("configuration using element 'child' is deprecated. Please use element 'flow'");
		registerFlow(flowEl);
	}
	/**
	 * register a flow element that contains the handlers for a specific record type (key)
	 * @param flowEl
	 * @throws Exception
	 * @deprecated please use registerFlow()
	 */
	public void registerFlow(RecordHandlingFlow flowEl) throws Exception {
		registeredFlows.add(flowEl);
	}

	
	/**
	 * register a uniquely named record manager.
	 * @param handler
	 * @throws Exception
	 * @deprecated please use registerRecordHandler()
	 */
	public void registerChild(IRecordHandler handler) throws Exception {
		log.warn("configuration using element 'child' is deprecated. Please use element 'recordHandler'");
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
	
	/**
	 * register a uniquely named result manager.
	 * @param handler
	 * @throws Exception
	 * @deprecated Please use registerResultHandler()
	 */
	public void registerChild(IResultHandler handler) throws Exception {
		log.warn("configuration using element 'child' is deprecated. Please use element 'resultHandler'");
		registerResultHandler(handler);
	}
	/**
	 * register a uniquely named result manager.
	 * @param handler
	 * @throws Exception
	 */
	public void registerResultHandler(IResultHandler handler) throws Exception {
		registeredResultHandlers.put(handler.getName(), handler);
		if (handler.isDefault()) {
			defaultHandler = handler;
		}
	}
	
	
	public void configure() throws ConfigurationException {
		super.configure();
		for (Iterator flowIt = registeredFlows.iterator(); flowIt.hasNext();) {
			RecordHandlingFlow flowEl = (RecordHandlingFlow) flowIt.next();
			configure(flowEl);
		}
	}


	public void start() throws PipeStartException {
		super.start();
		for (Iterator flowIt = registeredFlows.iterator(); flowIt.hasNext();) {
			RecordHandlingFlow flowEl = (RecordHandlingFlow) flowIt.next();
			try {
				open(flowEl);
			} catch (SenderException e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start", e);
			}
		}
	}
	public void stop() {
		super.stop();
		for (Iterator flowIt = registeredFlows.iterator(); flowIt.hasNext();) {
			RecordHandlingFlow flowEl = (RecordHandlingFlow) flowIt.next();
			try {
				close(flowEl);
			} catch (SenderException e) {
				log.error(getLogPrefix(null)+"exception on close", e);
			}
		}
	}

	private void configure(RecordHandlingFlow flow) throws ConfigurationException {
		log.debug("configuring flow ["+ClassUtils.nameOf(flow)+"]");
		// obtain the named manager 
		IRecordHandlerManager manager = (IRecordHandlerManager)registeredManagers.get(flow.getRecordHandlerManagerRef());
		if (manager == null) {
			throw new ConfigurationException("RecordHandlerManager [" + flow.getRecordHandlerManagerRef() + "] not found");
		}
		// register the flow with the manager
		manager.addHandler(flow);

		// obtain the named manager that is to be used after a specified record  
		IRecordHandlerManager nextManager = null;
		if (StringUtils.isEmpty(flow.getNextRecordHandlerManagerRef())) {
			nextManager = manager; 
		} else { 
			nextManager = (IRecordHandlerManager)registeredManagers.get(flow.getNextRecordHandlerManagerRef());
			if (nextManager == null) {
				throw new ConfigurationException("RecordHandlerManager [" + flow.getNextRecordHandlerManagerRef() + "] not found");
			}
		}
			
		// obtain the recordhandler 
		IRecordHandler recordHandler = (IRecordHandler)registeredRecordHandlers.get(flow.getRecordHandlerRef());
		if (recordHandler!=null) {
			recordHandler.configure();
		} else {
			log.debug("no recordhandler defined for ["+ClassUtils.nameOf(flow)+"]");
//			throw new ConfigurationException("no recordhandler defined for ["+ClassUtils.nameOf(flow)+"]");
		}
		
		// obtain the named resulthandler
		IResultHandler resultHandler = (IResultHandler)registeredResultHandlers.get(flow.getResultHandlerRef());
		if (resultHandler == null) {
			if (StringUtils.isEmpty(flow.getResultHandlerRef())) {
				resultHandler = defaultHandler;
			}
			else {
				throw new ConfigurationException("ResultHandler [" + flow.getResultHandlerRef() + "] not found");
			}
		}
		
		// initialise the flow object
		flow.setNextRecordHandlerManager(nextManager);
		flow.setRecordHandler(recordHandler);
		flow.setResultHandler(resultHandler);
	}
	
	public void open(RecordHandlingFlow flow) throws SenderException {
		IRecordHandler recordHandler = (IRecordHandler)registeredRecordHandlers.get(flow.getRecordHandlerRef());
		recordHandler.open();
	}
	public void close(RecordHandlingFlow flow) throws SenderException {
		IRecordHandler recordHandler = (IRecordHandler)registeredRecordHandlers.get(flow.getRecordHandlerRef());
		recordHandler.close();
	}

	protected String getStreamId(Object input, PipeLineSession session) throws PipeRunException {
		return session.getMessageId();
	}
	protected Reader getReader(String streamId, Object input, PipeLineSession session) throws PipeRunException {
		return new InputStreamReader((InputStream)input);
	}
	
	/**
	 * Open a reader for the file named according the input messsage and transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files. 
	 * 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String streamId = getStreamId(input, session);
		Reader reader = getReader(streamId, input,session);
		if (reader==null) {
			throw new PipeRunException(this,"could not obtain reader for ["+streamId+"]");
		}
		BufferedReader breader;
		if (reader instanceof BufferedReader) {
			breader = (BufferedReader)reader;
		} else {
			breader = new BufferedReader(reader);
		}
		Object transformationResult=null;
		try {
			transformationResult = transform(streamId, breader, session);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				log.warn(getLogPrefix(session)+"Exception closing reader",e);
			}
		}
		return new PipeRunResult(getForward(),transformationResult);
	}

	/*
	 * Read all lines from the reader, tread every line as a record and transform 
	 * it using the registered managers, record and result handlerds.
	 */	
	private Object transform(String streamId, BufferedReader reader, PipeLineSession session) throws PipeRunException {
		String rawRecord = null;
		int linenumber = 0;
		ArrayList prevParsedRecord = null; 
		IRecordHandler prevHandler = null;

		IRecordHandlerManager currentManager = initialFactory.getRecordFactoryUsingFilename(session, streamId);
		try {
			while ((rawRecord = reader.readLine()) != null) {
				linenumber++; // remember linenumber for exception handler
				if (StringUtils.isEmpty(rawRecord)) {
					continue; // ignore empty line
				}
				
				// get handlers for current line
				RecordHandlingFlow handlers = currentManager.getRecordHandler(session, rawRecord);
				if (handlers == null) {
					continue; // ignore line for which no handlers are registered
				}
				
				IRecordHandler curHandler = handlers.getRecordHandler(); 
				if (curHandler != null) {
					// there is a record handler, so transform the line
					ArrayList parsedRecord = curHandler.parse(session, rawRecord);
					Object result = curHandler.handleRecord(session, parsedRecord);
				
					// if there is a result handler, write the transformed result
					IResultHandler resultHandler = handlers.getResultHandler();
					if (result != null && resultHandler != null) {
						boolean mustPrefix = curHandler.mustPrefix(session, curHandler.equals(prevHandler), prevParsedRecord, parsedRecord); 
						resultHandler.writePrefix(session, streamId, mustPrefix, prevHandler != null);
						resultHandler.handleResult(session, streamId, handlers.getRecordKey(), result);
					}
					prevParsedRecord = parsedRecord;
					prevHandler = curHandler;
				}
				
				// get the manager for the next record
				currentManager = handlers.getNextRecordHandlerManager();
			}
			return finalizeResult(session, streamId, false);
		} catch(Exception e) {
			try {
				finalizeResult(session, streamId, true);
		 	} catch(Throwable t) {
				log.error("Unexpected error during finalizeResult of [" + streamId + "]", t);
			}
			throw new PipeRunException(this, "Error while transforming " + streamId + " at line " + linenumber, e);		
		}
	}
	
	/*
	 * finalizeResult is called when all records in the input file are handled
	 * and gives the resulthandlers a chance to f
	 */	
	private Object finalizeResult(PipeLineSession session, String inputFilename, boolean error) throws Exception {
		// finalize result
		ArrayList results = new ArrayList();
		for (Iterator handlersIt = registeredResultHandlers.values().iterator(); handlersIt.hasNext();) {
			IResultHandler resultHandler = (IResultHandler)handlersIt.next();
			resultHandler.writeSuffix(session, inputFilename);
			Object result = resultHandler.finalizeResult(session, inputFilename, error);
			if (result != null) {
				results.add(result);
			}
		}
		return FileUtils.getNamesFromList(results, ';');
	}
	
}
