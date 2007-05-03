/*
 * $Log: BatchFileTransformerPipe.java,v $
 * Revision 1.5  2007-05-03 11:30:45  europe\L190409
 * implement methods configure(), open() and close()
 *
 * Revision 1.4  2006/05/19 09:28:38  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 07:27:58  John Dekker <john.dekker@ibissource.org>
 * Resolves bug for writing suffix
 *
 * Revision 1.1  2005/10/11 13:00:22  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for transforming a (batch)file with records. Records in the file must be seperated
 * with new line characters.
 * You can use the <child> tag to register RecordHandlers, RecordHandlerManagers, ResultHandlers
 * and RecordHandlingFlow elements.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.BatchFileTransformerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #registerChild(IRecordHandler) recordHandler}</td><td>Handler for transforming records of a specific type</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #registerChild(IResultHandler) resultHandler}</td><td>Handler for processing transformed records</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #registerChild(IRecordHandlerManager) manager}</td><td>Manager determines which handlers are to be used for the current line</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #registerChild(RecordHandlingFlow) flow}</td><td>Element that contains the handlers for a specific record type</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #move2dirAfterTransform(String) directory}</td><td>Directory in which the transformed file(s) is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #move2dirAfterError(String) directory}</td><td>Directory to which the inputfile is moved in case an error occurs</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class BatchFileTransformerPipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: BatchFileTransformerPipe.java,v $  $Revision: 1.5 $ $Date: 2007-05-03 11:30:45 $";

	private IRecordHandlerManager initialFactory;
	private IResultHandler defaultHandler;
	private HashMap registeredManagers;
	private HashMap registeredRecordHandlers;
	private HashMap registeredResultHandlers;
	private Collection registeredFlows;
	private String move2dirAfterTransform;
	private String move2dirAfterError;
	
	public BatchFileTransformerPipe() {
		this.registeredManagers = new HashMap();
		this.registeredRecordHandlers = new HashMap();
		this.registeredResultHandlers = new HashMap();
		this.registeredFlows = new LinkedList(); 
	}
	
	/**
	 * register a uniquely named manager
	 * @param manager
	 * @throws Exception
	 */
	public void registerChild(IRecordHandlerManager manager) throws Exception {
		registeredManagers.put(manager.getName(), manager);
		if (manager.isInitial()) {
			initialFactory = manager;
		}
	}
	
	/**
	 * register a uniquely named record manager
	 * @param handler
	 * @throws Exception
	 */
	public void registerChild(IRecordHandler handler) throws Exception {
		registeredRecordHandlers.put(handler.getName(), handler);
	}
	
	/**
	 * register a uniquely named result manager
	 * @param handler
	 * @throws Exception
	 */
	public void registerChild(IResultHandler handler) throws Exception {
		registeredResultHandlers.put(handler.getName(), handler);
		if (handler.isDefault()) {
			defaultHandler = handler;
		}
	}
	
	/**
	 * register a flow element that contains the handlers for a specific record type (key)
	 * @param flowEl
	 * @throws Exception
	 */
	public void registerChild(RecordHandlingFlow flowEl) throws Exception {
		registeredFlows.add(flowEl);
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
		// obtain the named manager  
		IRecordHandlerManager manager = (IRecordHandlerManager)registeredManagers.get(flow.getRecordHandlerManagerRef());
		if (manager == null) {
			throw new ConfigurationException("RecordHandlerManager [" + flow.getRecordHandlerManagerRef() + "] not found");
		}
		manager.addHandler(flow);

		// obtain the named manager that is to be used after a specified record  
		IRecordHandlerManager nextManager = null;
		if (StringUtils.isEmpty(flow.getNextRecordHandlerManagerRef())) {
			nextManager = manager; 
		}
		else { 
			nextManager = (IRecordHandlerManager)registeredManagers.get(flow.getNextRecordHandlerManagerRef());
			if (nextManager == null) {
				throw new ConfigurationException("RecordHandlerManager [" + flow.getNextRecordHandlerManagerRef() + "] not found");
			}
		}
			
		// obtain the recordhandler 
		IRecordHandler recordHandler = (IRecordHandler)registeredRecordHandlers.get(flow.getRecordHandlerRef());
		recordHandler.configure();
		
		// obtain the named resulthandler
		IResultHandler resultHandler = (IResultHandler)registeredResultHandlers.get(flow.getResultHandlerRef());
		if (resultHandler == null) {
			if (StringUtils.isEmpty(flow.getResultHandlerRef())) {
				resultHandler = defaultHandler;
			}
			else {
				throw new ConfigurationException("ResultHandler [" + flow.getResultHandlerRef() + "] ntot found");
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

	/**
	 * Open a reader for the file named according the input messsage and 
	 * transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files. 
	 * 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		// Transform file content
		String filename = input.toString();
		File file = new File(filename);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			PipeRunResult result = null;
			try {
				result = new PipeRunResult(getForward(), transform(session, filename, reader));
			}
			finally {
				reader.close();
			}
			// Transformation is completed, move the file
			if (! StringUtils.isEmpty(move2dirAfterTransform)) {
				File move2 = new File(move2dirAfterTransform, file.getName());
				file.renameTo(move2); 
			}
			return result;
		}
		catch(Exception e) {
			try {
				finalizeResult(session, filename, true);

				if (! StringUtils.isEmpty(move2dirAfterError)) {
					File move2 = new File(move2dirAfterError, file.getName());
					file.renameTo(move2); 
				}
			}
			catch(Exception ex) {
				log.error("Unexpected error during finalizeResult of [" + filename + "]", ex);
			}
			
			if (e instanceof PipeRunException) {
				throw (PipeRunException)e;
			}
			throw new PipeRunException(this, "Unexpected error", e);
		}
	}

	/*
	 * Read all lines from the reader, tread every line as a record and transform 
	 * it using the registered managers, record and result handlerds.
	 */	
	private Object transform(PipeLineSession session, String inputFilename, BufferedReader reader) throws PipeRunException {
		String rawRecord = null;
		int linenumber = 0;
		ArrayList prevParsedRecord = null; 
		IRecordHandler prevHandler = null;

		IRecordHandlerManager currentManager = initialFactory.getRecordFactoryUsingFilename(session, inputFilename);
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
						resultHandler.writePrefix(session, inputFilename, mustPrefix, prevHandler != null);
						resultHandler.handleResult(session, inputFilename, handlers.getRecordKey(), result);
					}
					prevParsedRecord = parsedRecord;
					prevHandler = curHandler;
				}
				
				// get the manager for the next record
				currentManager = handlers.getNextRecordHandlerManager();
			}
			
			return finalizeResult(session, inputFilename, false);
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming " + inputFilename + " at line " + linenumber, e);		
		}
	}
	
	/*
	 * finalizeResult is called when all records in the input file are handled
	 * and gives the resulthandlers a change to f
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
	
	/**
	 * @param readyDir directory where input file is moved to in case of a succesful transformation
	 */
	public void setMove2dirAfterTransform(String readyDir) {
		move2dirAfterTransform = readyDir;
	}

	/**
	 * @param errorDir directory where input file is moved to in case of an error
	 */
	public void setMove2dirAfterError(String errorDir) {
		move2dirAfterError = errorDir;
	}

}
