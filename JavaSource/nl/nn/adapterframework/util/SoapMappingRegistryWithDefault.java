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
 * @version Id
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
