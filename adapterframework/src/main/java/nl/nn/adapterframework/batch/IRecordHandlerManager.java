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
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;

/**
 * Interface for handling a transformed record.
 * 
 * A RecordHandlerManager decides, based on some implementation dependent algorithm, which record handler
 * is to be used to process a record. 
 * A record manager keeps a table of flow-elements, that each define a recordhandler, resulthandler and, 
 * optionally, a next-manager.
 * 
 * @author John Dekker
 */
public interface IRecordHandlerManager extends INamedObject {

	public void configure(Map registeredManagers, Map registeredRecordHandlers, Map registeredResultHandlers, IResultHandler defaultHandler) throws ConfigurationException;

	/**
	 * @param flow New flow to be added to the managed flow elements
	 */
	void addHandler(RecordHandlingFlow flow);
	
	/**
	 * @param record 
	 * @return the RecordHandlingFlow element to be used to handle the record
	 * @throws Exception
	 */
	RecordHandlingFlow getRecordHandler(IPipeLineSession session, String record) throws Exception;
	
	/**
	 * @param filename
	 * @return the IRecordHandlingManager to be used initially based on the name of the input file 
	 */
	IRecordHandlerManager getRecordFactoryUsingFilename(IPipeLineSession session, String filename);
	
	/**
	 * @param initialFactory inidicates if this manager is the initial manager
	 */
	void setInitial(boolean initialFactory);
	boolean isInitial();
}
