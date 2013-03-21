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
package nl.nn.adapterframework.extensions.ifsa;

import java.util.Iterator;
import java.util.Map;
import java.util.List;

import org.apache.commons.lang.enums.Enum;
/**
 * Enumeration of IFSA message protocols.
 *
 * @author Johan Verrips IOS
 * @version $Id$
 */
public class IfsaMessageProtocolEnum extends Enum {
	
   public static final IfsaMessageProtocolEnum REQUEST_REPLY = new IfsaMessageProtocolEnum("RR");
   public static final IfsaMessageProtocolEnum FIRE_AND_FORGET = new IfsaMessageProtocolEnum("FF");

	protected IfsaMessageProtocolEnum(String arg1) {
		super(arg1);
	}
	
	public static IfsaMessageProtocolEnum getEnum(String messageProtocol) {
		return (IfsaMessageProtocolEnum)getEnum(
			IfsaMessageProtocolEnum.class,
			messageProtocol);
	}
	
	public static List getEnumList() {
		return getEnumList(IfsaMessageProtocolEnum.class);
	}
	
	public static Map getEnumMap() {
		return getEnumMap(IfsaMessageProtocolEnum.class);
	}
	
	public static String getNames() {
		String result = "[";
		for (Iterator i = iterator(IfsaMessageProtocolEnum.class); i.hasNext();) {
			IfsaMessageProtocolEnum c = (IfsaMessageProtocolEnum)i.next();
			result += c.getName();
			if (i.hasNext()) {
				result += ",";
			}
		}
		result += "]";
		return result;
	}

	public static Iterator iterator() {
		return iterator(IfsaMessageProtocolEnum.class);
	}
}
