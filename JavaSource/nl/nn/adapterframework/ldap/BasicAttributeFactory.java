/*
 * $Log: BasicAttributeFactory.java,v $
 * Revision 1.3  2011-11-30 13:52:05  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2007/02/26 15:56:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update of LDAP code, after a snapshot from Ibis4Toegang
 *
 */
package nl.nn.adapterframework.ldap;

import javax.naming.directory.BasicAttribute;

import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.xml.sax.Attributes;

public class BasicAttributeFactory extends AbstractObjectCreationFactory
{

	public Object createObject(Attributes arg0) throws Exception {
		return new BasicAttribute(arg0.getValue(0));
	}
		
}