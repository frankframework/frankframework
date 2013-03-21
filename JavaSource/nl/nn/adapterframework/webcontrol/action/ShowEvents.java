/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: ShowEvents.java,v $
 * Revision 1.3  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/08/12 16:05:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @version $Id$
 */
public class ShowEvents extends ShowMonitors {

	public String determineExitForward(DynaActionForm monitorForm) {
		return "showmonitors";
	}

}
