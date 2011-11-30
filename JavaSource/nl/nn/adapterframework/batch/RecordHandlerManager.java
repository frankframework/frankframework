/*
 * $Log: RecordHandlerManager.java,v $
 * Revision 1.13  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.11  2008/06/30 08:51:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * make valueHandlersMap available to descenders
 *
 * Revision 1.10  2008/02/19 09:23:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.9  2008/02/15 16:07:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2007/10/08 12:14:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.7  2007/08/03 08:28:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.6  2007/07/24 16:13:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved configure to manager
 *
 * Revision 1.5  2007/07/24 08:02:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reformatted code
 *
 * Revision 1.4  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Basic implementation of RecordHandlerManager, that allows only for a single flow.
 * The manager decides which handlers to be used for a specific record.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.FieldPositionRecordHandlerManager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the manager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInitial(boolean) initial}</td><td>This manager is the initial manager, i.e. to be used for the first record</td><td>false</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version Id
 */
public class RecordHandlerManager implements IRecordHandlerManager {
	public static final String version = "$RCSfile: RecordHandlerManager.java,v $  $Revision: 1.13 $ $Date: 2011-11-30 13:51:56 $";
	protected Logger log = LogUtil.getLogger(this);

	private Map valueHandlersMap;
	private String name;
	private boolean initial;

	RecordHandlerManager() {
		this.valueHandlersMap = new HashMap();
	}
	
	public IRecordHandlerManager getRecordFactoryUsingFilename(PipeLineSession session, String inputFilename) {
		return this;
	}

	public void configure(Map registeredManagers, Map registeredRecordHandlers, Map registeredResultHandlers, IResultHandler defaultHandler) throws ConfigurationException {
		for(Iterator it=valueHandlersMap.keySet().iterator();it.hasNext();) {
			String name=(String)it.next();
			RecordHandlingFlow flow = getFlowByName(name);
			flow.configure(this, registeredManagers, registeredRecordHandlers, registeredResultHandlers, defaultHandler);
		}
	}

	private RecordHandlingFlow getFlowByName(String name) {
		return (RecordHandlingFlow)valueHandlersMap.get(name);
	}
	
	public void addHandler(RecordHandlingFlow handlers) {
		valueHandlersMap.put(handlers.getRecordKey(), handlers);
		if (handlers.getNextRecordHandlerManager() == null) {
			handlers.setNextRecordHandlerManager(this);
		}
	}

	public Collection getRecordHandlers() {
		return valueHandlersMap.values();	
	}
	
	protected Map getValueHandlersMap() {
		return valueHandlersMap;	
	}
	
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
		return (RecordHandlingFlow)valueHandlersMap.get("*");
	}

	/**
	 * Determines the recordhandler to use, based on key.
	 * Key is "*" by default, but can be changed by descendant implementations.
	 * @param recordKey
	 * @return RecordHandlingFlow element to be used for handling records of type recordkey
	 * @throws Exception
	 */
	public RecordHandlingFlow getRecordHandlerByKey(String recordKey) throws Exception {
		RecordHandlingFlow rhf =(RecordHandlingFlow)valueHandlersMap.get(recordKey);
		if  (rhf == null) {
			rhf =(RecordHandlingFlow)valueHandlersMap.get("*");
			if  (rhf == null) {
				throw new Exception("No handlers (flow) found for recordKey [" + recordKey + "]");
			}
		}
		return rhf;
		
	}

	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void setInitial(boolean b) {
		initial = b;
	}
	public boolean isInitial() {
		return initial;
	}


}
