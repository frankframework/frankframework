/*
 * $Log: BasicAttributeFactory.java,v $
 * Revision 1.1  2007-02-26 15:56:37  europe\L190409
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