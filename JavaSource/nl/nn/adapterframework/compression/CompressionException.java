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
 * $Log: CompressionException.java,v $
 * Revision 1.3  2011-11-30 13:51:57  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/01/06 17:57:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * classes for reading and writing zip archives
 *
 */
package nl.nn.adapterframework.compression;

import nl.nn.adapterframework.core.IbisException;

/**
 * Wrapper for compression related exceptions.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.10  
 * @version $Id$
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
