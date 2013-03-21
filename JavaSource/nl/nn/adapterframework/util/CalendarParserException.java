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
 * $Log: CalendarParserException.java,v $
 * Revision 1.3  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/06/03 15:53:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of CalendarParser
 *
 */
package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception for CalendarParser.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public class CalendarParserException extends IbisException {

	public CalendarParserException(String msg) {
		super(msg);
	}

	public CalendarParserException(String msg, Throwable t) {
		super(msg,t);
	}
}
