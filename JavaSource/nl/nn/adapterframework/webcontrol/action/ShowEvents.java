/*
 * $Log: ShowEvents.java,v $
 * Revision 1.1  2008-08-12 16:05:10  europe\L190409
 * added feature to show events in console
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import org.apache.struts.action.DynaActionForm;

/**
 * Edit a Monitor - display the form.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class ShowEvents extends ShowMonitors {

	public String determineExitForward(DynaActionForm monitorForm) {
		return "showmonitors";
	}

}
