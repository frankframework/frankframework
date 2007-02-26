/*
 * $Log: BasicAttributeBean.java,v $
 * Revision 1.1  2007-02-26 15:56:37  europe\L190409
 * update of LDAP code, after a snapshot from Ibis4Toegang
 *
 */
package nl.nn.adapterframework.ldap;

import javax.naming.directory.BasicAttribute;

public class BasicAttributeBean extends BasicAttribute
{

	/**
	 * @param id = name of the attribute
	 */
	public BasicAttributeBean(String id) {
		super(id);
	}

	public String getAttrID()
	{
		return attrID;	
	}

	public void setAttrID(String id)
	{
		this.attrID = id;	
	}
}