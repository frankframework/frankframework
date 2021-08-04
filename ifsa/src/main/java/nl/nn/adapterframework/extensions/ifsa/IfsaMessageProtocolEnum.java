/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.util.EnumUtils;

public enum IfsaMessageProtocolEnum implements DocumentedEnum {

	@EnumLabel("RR") REQUEST_REPLY, 
	@EnumLabel("FF") FIRE_AND_FORGET;

	public static IfsaMessageProtocolEnum getEnum(String messageProtocol) {
		return EnumUtils.parse(IfsaMessageProtocolEnum.class, messageProtocol);
	}

	public static String getNames() {
		return "[RR,FF]";
	}
}
