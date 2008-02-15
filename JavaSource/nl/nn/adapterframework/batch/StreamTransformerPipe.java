/*
 * $Log: StreamTransformerPipe.java,v $
 * Revision 1.13  2008-02-15 16:05:45  europe\L190409
 * added default manager and flow, for simple configurations
 *
 * Revision 1.12  2007/10/08 13:28:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.11  2007/10/08 12:14:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.10  2007/09/24 14:55:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameters
 *
 * Revision 1.9  2007/09/24 13:02:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved debug logging
 *
 * Revision 1.8  2007/09/19 13:03:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added block managing code
 *
 * Revision 1.7  2007/09/17 08:21:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * only write suffix and prefix in the middle of processing if prefix non-emtpy
 *
 * Revision 1.6  2007/09/13 12:38:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved debug info
 *
 * Revision 1.5  2007/09/10 11:08:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed logic processing from writePrefix to calling class
 * renamed writePrefix() and writeSuffix() into open/closeRecordType()
 *
 * Revision 1.4  2007/08/03 08:40:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * call configure(), open() and close() on resulthandlers too
 *
 * Revision 1.3  2007/07/26 16:12:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.2  2007/07/24 16:11:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first working version
 *
 * Revision 1.1  2007/07/24 08:04:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version, to be tested
 */
package nl.nn.adapterframework.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for transforming a stream with records. Records in the stream must be separated
 * with new line characters.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.StreamTransformerPipe</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line</td></tr>
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
 * @author: John Dekker / Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class StreamTransformerPipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: StreamTransformerPipe.java,v $  $Revision: 1.13 $ $Date: 2008-02-15 16:05:45 $";

	private IRecordHandlerManager initialManager=null;
	private IResultHandler defaultHandler=null;
	private Map registeredManagers= new HashMap();
	private Map registeredRecordHandlers= new HashMap();
	private Map registeredResultHandlers= new HashMap();
	
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
		log.warn("configuration using element 'child' is deprecated. Please use element 'flow' nested in element 'manager'");
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
	public IResultHandler getResultHandler(String name) {
		return (IResultHandler)registeredResultHandlers.get(name);
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
		ParameterResolutionContext prc = new ParameterResolutionContext("", session);
		try {
			transformationResult = transform(streamId, breader, session, prc);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				log.warn(getLogPrefix(session)+"Exception closing reader",e);
			}
		}
		return new PipeRunResult(getForward(),transformationResult);
	}

	protected List getBlockStack(PipeLineSession session, String streamId, boolean create) {
		List list = (List)session.get("blockStack");
		if (list==null) {
			if (create) {
				list=new ArrayList();
				session.put("blockStack",list);
			}
		}
		return list;
	}

	protected List getBlockStack(PipeLineSession session, String streamId) throws SenderException {
		return getBlockStack(session,streamId,false);
	}

	protected void openBlock(PipeLineSession session, String streamId, IResultHandler handler, RecordHandlingFlow flow, String blockName, ParameterResolutionContext prc) throws Exception {
		if (StringUtils.isNotEmpty(blockName)) {
			if (handler!=null) {
				if (flow.isAutoCloseBlock()) {
					List blockStack=getBlockStack(session,streamId,true);
					int blockLevel;
					for (blockLevel=blockStack.size()-1;blockLevel>=0; blockLevel--) {
						String stackedBlock=(String)blockStack.get(blockLevel);
						if (stackedBlock.equals(blockName)) {
							break;
						}
					}
					if (blockLevel>=0) {
						for (int i=blockStack.size()-1; i>=blockLevel; i--) {
							String stackedBlock=(String)blockStack.remove(i);
							closeBlock(session,streamId,handler,stackedBlock, prc);
						}
					}
					blockStack.add(blockName);
				}
				if (log.isDebugEnabled()) log.debug("opening block ["+blockName+"] resultHandler["+handler.getName()+"]");
				handler.openBlock(session, streamId, blockName, prc);
			} else {
				log.warn("openBlock("+blockName+") without resultHandler");
			}
		}
	}
	protected void closeBlock(PipeLineSession session, String streamId, IResultHandler handler, String blockName, ParameterResolutionContext prc) throws Exception {
		if (handler!=null && StringUtils.isNotEmpty(blockName)) {
			if (log.isDebugEnabled()) log.debug("closing block ["+blockName+"] resultHandler["+handler.getName()+"]");
			handler.closeBlock(session, streamId, blockName, prc);
		}
	}

	protected void closeAllBlocks(PipeLineSession session, String streamId, IResultHandler handler, ParameterResolutionContext prc) throws Exception {
		if (handler!=null) {
			List blockStack=getBlockStack(session,streamId,false);
			if (blockStack!=null) {
				for (int i=blockStack.size()-1; i>=0; i--) {
					String stackedBlock=(String)blockStack.remove(i);
					closeBlock(session,streamId,handler,stackedBlock, prc);
				}
			}
		}
	}


	/*
	 * Read all lines from the reader, tread every line as a record and transform 
	 * it using the registered managers, record and result handlerds.
	 */	
	private Object transform(String streamId, BufferedReader reader, PipeLineSession session, ParameterResolutionContext prc) throws PipeRunException {
		String rawRecord = null;
		int linenumber = 0;
		List prevParsedRecord = null; 
		IRecordHandler prevHandler = null;

		IRecordHandlerManager currentManager = initialManager.getRecordFactoryUsingFilename(session, streamId);
		try {
			openDocument(session,streamId, prc);
			while ((rawRecord = reader.readLine()) != null) {
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
				closeBlock(session, streamId, resultHandler, flow.getCloseBlockBeforeLine(), prc);
				openBlock(session, streamId, resultHandler, flow, flow.getOpenBlockBeforeLine(), prc);
				
				IRecordHandler curHandler = flow.getRecordHandler(); 
				if (curHandler != null) {
					log.debug("manager ["+currentManager.getName()+"] key ["+flow.getRecordKey()+"] record handler ["+curHandler.getName()+"]: "+rawRecord);
					// there is a record handler, so transform the line
					List parsedRecord = curHandler.parse(session, rawRecord);
					Object result = curHandler.handleRecord(session, parsedRecord, prc);
				
					// if there is a result handler, write the transformed result
					if (result != null && resultHandler != null) {
						boolean recordTypeChanged = curHandler.isNewRecordType(session, curHandler.equals(prevHandler), prevParsedRecord, parsedRecord);
						// the hasPrefix() call allows users use a suffix without a prefix. 
						// The suffix is then only written at the end of the file.
						if (recordTypeChanged && resultHandler.hasPrefix()) {   
							if (prevHandler != null)  {
								resultHandler.closeRecordType(session, streamId, prc);
							}
							resultHandler.openRecordType(session, streamId, prc);
						}
						resultHandler.handleResult(session, streamId, flow.getRecordKey(), result, prc);
					}
					prevParsedRecord = parsedRecord;
					prevHandler = curHandler;
				} else {
					log.debug("manager ["+currentManager.getName()+"] key ["+flow.getRecordKey()+"], no record handler: "+rawRecord);
				}
				
				closeBlock(session, streamId, resultHandler, flow.getCloseBlockAfterLine(), prc);
				openBlock(session, streamId, resultHandler, flow, flow.getOpenBlockAfterLine(), prc);
				
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
			throw new PipeRunException(this, "Error while transforming " + streamId + " at line " + linenumber, e);		
		} finally {
			closeDocument(session,streamId, prc);
		}
	}

	private void openDocument(PipeLineSession session, String inputFilename, ParameterResolutionContext prc) throws Exception {
		for (Iterator it = registeredResultHandlers.values().iterator(); it.hasNext();) {
			IResultHandler resultHandler = (IResultHandler)it.next();
			resultHandler.openDocument(session, inputFilename, prc);
		}
	}
	private void closeDocument(PipeLineSession session, String inputFilename, ParameterResolutionContext prc) {
		for (Iterator it = registeredResultHandlers.values().iterator(); it.hasNext();) {
			IResultHandler resultHandler = (IResultHandler)it.next();
			resultHandler.closeDocument(session, inputFilename, prc);
		}
	}
	
	/*
	 * finalizeResult is called when all records in the input file are handled
	 * and gives the resulthandlers a chance to finalize.
	 */	
	private Object finalizeResult(PipeLineSession session, String inputFilename, boolean error, ParameterResolutionContext prc) throws Exception {
		// finalize result
		List results = new ArrayList();
		for (Iterator handlersIt = registeredResultHandlers.values().iterator(); handlersIt.hasNext();) {
			IResultHandler resultHandler = (IResultHandler)handlersIt.next();
			resultHandler.closeRecordType(session, inputFilename, prc);
			closeAllBlocks(session,inputFilename,resultHandler, prc);
			log.debug("finalizing resulthandler ["+resultHandler.getName()+"]");
			Object result = resultHandler.finalizeResult(session, inputFilename, error, prc);
			if (result != null) {
				results.add(result);
			}
		}
		return FileUtils.getNamesFromList(results, ';');
	}
	
}
