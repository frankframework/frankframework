/*
 * $Log: AttributeCheckingRule.java,v $
 * Revision 1.1  2007-05-11 09:36:59  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.configuration;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Helper class to check that each attribute set from the configuration is available on the 
 * object being configured. 
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class AttributeCheckingRule extends Rule {
	protected Logger log = LogUtil.getLogger(this);

	private String getObjectName(Object o) {
		String result=o.getClass().getName();
		if (o instanceof INamedObject) {
			result+=" ["+((INamedObject)o).getName()+"]";
		}
		return result;
	}

	public void begin(String uri, String elementName, Attributes attributes) throws Exception {

		Object top = digester.peek();

		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			if ("".equals(name)) {
				name = attributes.getQName(i);
			}
			if (name!=null && !name.equals("className")) {
//				if (log.isDebugEnabled()) {
//					log.debug(getObjectName(top)+" checking for setter for attribute ["+name+"]");
//				}
				PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(top, name);
				Method m=null;
				if (pd!=null) {
					m = PropertyUtils.getWriteMethod(pd);
				}
				if (m==null) {
					Locator loc = digester.getDocumentLocator();
					log.warn("line "+loc.getLineNumber()+", col "+loc.getColumnNumber()+": "+getObjectName(top)+" does not have an attribute ["+name+"]");
				}
			}
		}
		
	}


}
