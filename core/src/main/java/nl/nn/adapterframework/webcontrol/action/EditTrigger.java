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
package nl.nn.adapterframework.webcontrol.action;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.DynaActionForm;



/**
 * Edit a Monitor - display the form.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class EditTrigger extends EditMonitor {

	public static final String LABEL_FILTER_EVENTS2ADAPTERS  =   "Events -> Adapters";
	public static final String LABEL_FILTER_EVENTS2SOURCES   =   "Events -> Sources";
	public static final String LABEL_FILTER_ADAPTERS2EVENTS  = "Adapters -> Events";
	public static final String LABEL_FILTER_ADAPTERS2SOURCES = "Adapters -> Sources";
	public static final String LABEL_FILTER_SOURCES2EVENTS   =  "Sources -> Events";
	public static final String LABEL_FILTER_SOURCES2ADAPTERS =  "Sources -> Adapters";

	@Override
	public String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
		return null;
	}
}
