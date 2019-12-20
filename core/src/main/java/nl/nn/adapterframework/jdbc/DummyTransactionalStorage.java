/*
   Copyright 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.jdbc;

import java.io.Serializable;
import java.util.Date;

import nl.nn.adapterframework.core.SenderException;

/**
 * Class for a messageLog element to be used in combination with a
 * {@link MessageStoreSender} who's messaged are processed by a
 * {@link MessageStoreListener}.
 * 
 * @author Jaco de Groot
 */
public class DummyTransactionalStorage extends JdbcTransactionalStorage {

	@Override
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, Serializable message) throws SenderException {
		return null;
	}

}
