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
package nl.nn.adapterframework.util;

import org.apache.soap.encoding.SOAPMappingRegistry;
import org.apache.soap.encoding.soapenc.StringDeserializer;
import org.apache.soap.util.xml.Deserializer;
import org.apache.soap.util.xml.QName;

/**
 * SoapMappingRegistry that returns StringDeserializer for each unknown element.
 * 
 * @author Gerrit van Brakel
 * @version $Id$
 */
public class SoapMappingRegistryWithDefault extends SOAPMappingRegistry {
	
	protected Deserializer queryDeserializer_(QName elementType, java.lang.String encodingStyleURI)
	{
		Deserializer result = super.queryDeserializer_(elementType,encodingStyleURI);
		if (result==null) {
			result=new StringDeserializer();
		}
		return result;
	}

}
