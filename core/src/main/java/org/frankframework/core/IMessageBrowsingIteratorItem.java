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
package org.frankframework.core;

import java.util.Date;

/**
 * Iterator item for messagebrowsers.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public interface IMessageBrowsingIteratorItem extends AutoCloseable {

	String getId() throws ListenerException;
	String getOriginalId() throws ListenerException;
	String getCorrelationId() throws ListenerException;
	Date   getInsertDate() throws ListenerException;
	Date   getExpiryDate() throws ListenerException;
	String getType() throws ListenerException;
	String getHost() throws ListenerException;
	String getCommentString() throws ListenerException;
	String getLabel() throws ListenerException;

	/**
	 * close() must be called, in a finally clause, after the item is not used anymore,
	 * to allow to free resources.
	 */
	@Override
	void close();

}
