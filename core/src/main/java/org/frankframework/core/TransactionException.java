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

/**
 * Wrapper for numerous transaction handling related exceptions.
 *
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class TransactionException extends IbisException {

	public TransactionException() {
		super();
	}
	public TransactionException(String errMsg) {
		super(errMsg);
	}
	public TransactionException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public TransactionException(Throwable t) {
		super(t);
	}

}
