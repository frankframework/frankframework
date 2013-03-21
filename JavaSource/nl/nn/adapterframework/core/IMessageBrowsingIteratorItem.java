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
 * $Log: IMessageBrowsingIteratorItem.java,v $
 * Revision 1.4  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/01/04 15:05:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added label
 *
 * Revision 1.1  2009/12/23 17:05:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 */
package nl.nn.adapterframework.core;

import java.util.Date;

/**
 * Iterator item for messagebrowsers. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public interface IMessageBrowsingIteratorItem {

	String getId() throws ListenerException;
	String getOriginalId() throws ListenerException;
	String getCorrelationId() throws ListenerException;
	Date   getInsertDate() throws ListenerException;
	Date   getExpiryDate() throws ListenerException;
	String getType() throws ListenerException;
	String getHost() throws ListenerException;
	String getCommentString() throws ListenerException;
	String getLabel() throws ListenerException;
	
	/*
	 * release must be called, in a finally clause, after the item is not used anymore, 
	 * to allow to free resources.
	 */
	void release();

}
