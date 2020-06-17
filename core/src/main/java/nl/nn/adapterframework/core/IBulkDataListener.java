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
package nl.nn.adapterframework.core;

import java.util.Map;

import nl.nn.adapterframework.stream.Message;

/**
 * Listener extension that allows to transfer of a lot of data, and do it within the transaction handling.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public interface IBulkDataListener<M> extends IListener<M> {

	/**
	 * Retrieves the bulk data associated with the message, stores it in a file or something similar.
	 * It returns the handle to the file as a result, and uses that as the message for the pipeline.
	 * @return input message for adapter.
	 */
	String retrieveBulkData(Object rawMessageOrWrapper, Message message, Map<String,Object> context) throws ListenerException;

}
