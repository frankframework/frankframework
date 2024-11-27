/*
   Copyright 2020 Nationale-Nederlanden

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


/**
 * PullingListener extension that checks for available messages to retrieve
 * without starting a XA transaction. With a MS SQL database and a high load it
 * has been noticed that the application and database transaction coordinator
 * were out of sync which resulted in hanging distributed transactions.
 **/

public interface IPeekableListener<M> extends IPullingListener<M> {

	boolean hasRawMessageAvailable() throws ListenerException;

	/**
	 * when true, then PollingListener container will execute getRawMessage() only when hasRawMessageAvailable() has returned true. This avoids rolling back a lot of XA transactions, that appears to be problematic on MS SQL Server
	 * @ff.default true
	 */
	void setPeekUntransacted(boolean b);

	boolean isPeekUntransacted();
}
