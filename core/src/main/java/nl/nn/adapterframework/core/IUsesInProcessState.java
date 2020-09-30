/*
   Copyright 2020 WeAreFrank!

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
/**
 * Extension for transacted pulling listeners, that allows them to set the status of 
 * a message to 'inProcess' while it is being executed. In case of a rollback of the executing
 * transaction, the status of the message will be reverted to 'available', so that it can be retried. 
 * 
 * @author  Gerrit van Brakel
 */
public interface IUsesInProcessState<M> extends IPullingListener<M> {

	/**
	 * Should sets the status of the message to 'inProcess'. When it returns true,
	 * transacted Receivers will commit the transaction, and run the processing of the message
	 * in a fresh transaction.
	 */
	boolean setMessageStateToInProcess(M rawMessage, Map<String,Object> threadContext) throws ListenerException;
	
	/**
	 * Reverts, after the processing of the message has been ended unsuccessfully, the status of the
	 * message to 'available' in case it is still left in the status 'inProcess'.
	 * This method is called in a fresh transaction, and only when the original transaction, that hosted
	 * the main processing of the message and the call to afterMessageProcessed(), has failed, and only
	 * when {{@link #setMessageStateToInProcess(Object, Map)} returned true.
	 * Implementations should check, and act accordingly.
	 */
	void revertInProcessStatusToAvailable(M rawMessage, Map<String,Object> threadContext) throws ListenerException;

}
