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
/*
 * $Log: IReceiver.java,v $
 * Revision 1.11  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.9  2009/12/29 14:32:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified imports to reflect move of statistics classes to separate package
 *
 * Revision 1.8  2009/03/30 12:23:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added counter for messagesRejected
 *
 * Revision 1.7  2008/08/12 15:33:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * receiver must implement HasStatistics
 * added property messagesRetried
 *
 * Revision 1.6  2004/08/25 09:11:33  unknown <unknown@ibissource.org>
 * Add waitForRunstate with timeout
 *
 * Revision 1.5  2004/03/31 12:04:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.util.RunStateEnum;

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
 *  @version $Id$
 *  @author Johan Verrips
 *  @see IAdapter
 *  @see IAdapter#processMessage(String, String)
 *  @see ISender
 *  @see PipeLineResult
 *
 */
public interface IReceiver extends IManagable, HasStatistics {

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
    
	void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException;
	boolean waitForRunState(RunStateEnum requestedRunState, long timeout) throws InterruptedException;
}
