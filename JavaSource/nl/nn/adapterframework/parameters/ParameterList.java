/*
 * $Log: ParameterList.java,v $
 * Revision 1.5  2011-11-30 13:52:03  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2006/10/13 08:15:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added findParameter()
 *
 * Revision 1.2  2004/10/12 15:07:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configure()-method
 *
 * Revision 1.1  2004/10/05 09:52:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved parameter code  to package parameters
 *
 * Revision 1.1  2004/05/21 07:58:47  unknown <unknown@ibissource.org>
 * Moved PipeParameter to core
 *
 */
package nl.nn.adapterframework.parameters;

import java.util.ArrayList;
import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;


/**
 * List of parameters.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class ParameterList extends ArrayList {
	
	public ParameterList() {
		super();
	}

	public ParameterList(int i) {
		super(i);
	}
	
	public void configure() throws ConfigurationException {
		for (int i=0; i<size(); i++) {
			getParameter(i).configure();
		}
	}
	
	public Parameter getParameter(int i) {
		return (Parameter)get(i);
	}
	
	public Parameter findParameter(String name) {
		for (Iterator it=iterator();it.hasNext();) {
			Parameter p = (Parameter)it.next();
			if (p!=null && p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}
}
