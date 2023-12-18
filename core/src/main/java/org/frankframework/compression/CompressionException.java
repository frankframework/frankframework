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
package org.frankframework.compression;

import org.frankframework.core.IbisException;

/**
 * Wrapper for compression related exceptions.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class CompressionException extends IbisException {

	public CompressionException() {
		super();
	}

	public CompressionException(String msg) {
		super(msg);
	}

	public CompressionException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public CompressionException(Throwable cause) {
		super(cause);
	}

}
