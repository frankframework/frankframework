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

import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Interface for transforming a record (= structured ASCII line). 
 * 
 * @author John Dekker
 */
public interface IRecordHandler extends INamedObject {

	public void configure() throws ConfigurationException;
	public void open() throws SenderException;
	public void close() throws SenderException;

	/**
	 * Parse the line into an array of fields.
	 * 
	 * @return List with String values for each inputfield
	 * @throws Exception
	 */
	List parse(IPipeLineSession session, String record) throws Exception;

	/**
	 * Perform an action on the array of fields.
	 * 
	 * @return transformed result
	 * @throws Exception
	 */	
	Object handleRecord(IPipeLineSession session, List parsedRecord, ParameterResolutionContext prc) throws Exception;
	
	boolean isNewRecordType(IPipeLineSession session, boolean equalRecordTypes, List prevRecord, List curRecord) throws Exception;
	
	public String getRecordType(List record);
	
}
