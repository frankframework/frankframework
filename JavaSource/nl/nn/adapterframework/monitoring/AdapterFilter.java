/*
 * $Log: AdapterFilter.java,v $
 * Revision 1.3  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/05/13 08:18:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
