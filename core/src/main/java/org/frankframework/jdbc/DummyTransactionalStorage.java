/*
   Copyright 2015 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.jdbc;

import java.io.Serializable;
import java.util.Date;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.SenderException;

/**
 * Class for a messageLog element to be used in combination with a
 * {@link MessageStoreSender} who's messages are processed by a
 * {@link MessageStoreListener}.
 *
 * @author Jaco de Groot
 */
@Deprecated(forRemoval = true, since = "7.8.0")
@ConfigurationWarning("It is no longer necessary to use the DummyTransactionalStorage")
public class DummyTransactionalStorage<S extends Serializable> extends JdbcTransactionalStorage<S> {

	@Override
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, S message) throws SenderException {
		return null;
	}

}
