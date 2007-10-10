/*
 * $Log: ParameterValueList.java,v $
 * Revision 1.3.4.1  2007-10-10 14:30:45  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.5  2007/10/08 13:31:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.4  2007/10/08 12:21:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.3  2005/10/24 09:59:23  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.2  2004/10/12 15:09:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * only nested map and list
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.core.ParameterException;

/**
 * List of parametervalues.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class ParameterValueList {
	
	List list;
	Map   map;
	
	public ParameterValueList() {
		super();
		list = new ArrayList();
		map  = new HashMap();
	}

	public ParameterValueList(int i) {
		super();
		list = new ArrayList(i);
		map  = new HashMap();
	}
	
	public void add(ParameterValue pv) {
		list.add(pv);
		map.put(pv.getDefinition().getName(),pv);
	}
	
	public ParameterValue getParameterValue(int i) {
		return (ParameterValue)(list.get(i));
	}

	public ParameterValue getParameterValue(String name) {
		return (ParameterValue)(map.get(name));
	}
	
	public boolean parameterExists(String name) {
		return map.get(name)!=null;
	}

	public int size() {
		return list.size();
	}
	
	Map getParameterValueMap() {
		return map;
	}

	/*
	 * Helper routine for quickly iterating through the resolved parameters
	 * in the order in which they are defined 
	 */
	public void forAllParameters(IParameterHandler handler) throws ParameterException {
		for (Iterator param = list.iterator(); param.hasNext();) {
			ParameterValue paramValue = (ParameterValue)param.next();
			handler.handleParam(paramValue.getDefinition().getName(), paramValue.getValue());
		}
	}
}
