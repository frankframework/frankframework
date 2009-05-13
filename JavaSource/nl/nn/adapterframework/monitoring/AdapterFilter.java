/*
 * $Log: AdapterFilter.java,v $
 * Revision 1.1  2009-05-13 08:18:50  L190409
 * improved monitoring: triggers can now be filtered multiselectable on adapterlevel
 *
 */
package nl.nn.adapterframework.monitoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter on Adapters, used by Triggers.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.8
 * @version Id
 */
public class AdapterFilter {
	
	private String adapter;
	
	private List subObjectList=new ArrayList();

	/**
	 * Set the name of the Adapter that this AdapterFilter filters on.
	 */
	public void setAdapter(String string) {
		adapter = string;
	}
	public String getAdapter() {
		return adapter;
	}

	/**
	 * Register the name of a SubObject (such as a Pipe) to be included in the filter.
	 */
	public void registerSubOject(String name) {
		subObjectList.add(name);
	}
	/**
	 * Get the list of registered names of SubObjects included in the filter.
	 */
	public List getSubObjectList() {
		return subObjectList;
	}
}
