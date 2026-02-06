/*
   Copyright 2013 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Iterator item for messagebrowsers.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@NullMarked
public interface IMessageBrowsingIteratorItem extends AutoCloseable {

	@Nullable
	String getId() throws ListenerException;
	@Nullable
	String getOriginalId() throws ListenerException;
	@Nullable
	String getCorrelationId() throws ListenerException;
	@Nullable
	Date   getInsertDate() throws ListenerException;
	@Nullable
	Date   getExpiryDate() throws ListenerException;
	@Nullable
	String getType() throws ListenerException;
	@Nullable
	String getHost() throws ListenerException;
	@Nullable
	String getCommentString() throws ListenerException;
	@Nullable
	String getLabel() throws ListenerException;

	/**
	 * close() must be called, in a finally clause, after the item is not used anymore,
	 * to allow to free resources.
	 */
	@Override
	void close();
}
