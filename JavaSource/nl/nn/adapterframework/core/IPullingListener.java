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
 * $Log: IPullingListener.java,v $
 * Revision 1.10  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.8  2007/10/03 08:13:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.7  2004/09/08 14:15:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.6  2004/08/03 13:10:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved afterMessageProcessed to IListener
 *
 * Revision 1.5  2004/07/15 07:38:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IListener as common root for Pulling and Pushing listeners
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import java.util.Map;
/**
 * Defines listening behaviour of pulling receivers.
 * Pulling receivers are receivers that poll for a message, as opposed to pushing receivers
 * that are 'message driven'
 * 
 * @author  Gerrit van Brakel
 * @version $Id$
 */
public interface IPullingListener extends IListener {
	public static final String version = "$RCSfile: IPullingListener.java,v $ $Revision: 1.10 $ $Date: 2011-11-30 13:51:55 $";

	/**
	 * Prepares a thread for receiving messages.
	 * Called once for each thread that will listen for messages.
	 * @return the threadContext for this thread. The threadContext is a Map in which
	 * thread-specific data can be stored. 
	 */
	Map openThread() throws ListenerException;
	
	/**
	 * Finalizes a message receiving thread.
	 * Called once for each thread that listens for messages, just before
	 * {@link #close()} is called.
	 */
	void closeThread(Map threadContext) throws ListenerException;
	
	/**
	 * Retrieves messages from queue or other channel, but does no processing on it.
	 * Multiple objects may try to call this method at the same time, from different threads. 
	 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
	 * <p>Any thread-specific properties should be stored in and retrieved from the threadContext.
	 */
	Object getRawMessage(Map threadContext) throws ListenerException;

}
