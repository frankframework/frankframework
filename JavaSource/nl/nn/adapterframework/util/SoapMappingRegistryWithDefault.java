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
 * $Log: SoapMappingRegistryWithDefault.java,v $
 * Revision 1.3  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/04/26 09:34:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SoapMappingRegistryWithDefault
 *
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
