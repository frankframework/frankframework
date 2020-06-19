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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.stream.Message;

/**
 * The receiver is the trigger and central communicator for the framework.
 * <br/>
 * The main responsibilities are:
 * <ul>
 *    <li>receiving messages</li>
 *    <li>for asynchronous receivers (which have a separate sender):<br/>
 *            <ul><li>initializing ISender objects</li>
 *                <li>stopping ISender objects</li>
 *                <li>sending the message with the ISender object</li>
 *            </ul>
 *    <li>synchronous receivers give the result directly</li>
 *    <li>take care of connection, sessions etc. to startup and shutdown</li>
 * </ul>
 * Listeners call the IAdapter.processMessage(String correlationID,String message)
 * to do the actual work, which returns a <code>{@link PipeLineResult}</code>. The receiver
 * may observe the status in the <code>{@link PipeLineResult}</code> to perfom committing
 * requests.
 * 
 *  @author Johan Verrips
 *  @see IAdapter
 *  @see IAdapter#processMessage(String, Message, IPipeLineSession)
 *  @see ISender
 *  @see PipeLineResult
 *
 */
public interface IReceiver<M> extends IManagable, HasStatistics {

 	/**
 	 * This method is called by the <code>IAdapter</code> to let the
 	 * receiver do things to initialize itself before the <code>startListening</code>
 	 * method is called.
 	 * @see #startRunning
 	 * @throws ConfigurationException when initialization did not succeed.
 	 */ 
	public void configure() throws ConfigurationException;
	
	/**
	 * get the number of messages received by this receiver.
	 */
	public long getMessagesReceived();
	/**
	 * get the number of duplicate messages received this receiver.
	 */
	public long getMessagesRetried();
	/**
	 * get the number of messages received this receiver that are not process, but are discarded or put in the errorstorage.
	 */
	public long getMessagesRejected();
	
    /**
     * The processing of messages must be delegated to the <code>Adapter</code>
     * object. The adapter also provides a MessageKeeper, which the receiver
     * may use to store messages in.
     * @see nl.nn.adapterframework.core.IAdapter
     */
    public void setAdapter(IAdapter adapter);
    public IAdapter getAdapter();

	public IListener<M> getListener();

}
