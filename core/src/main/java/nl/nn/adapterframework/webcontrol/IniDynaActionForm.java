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
package nl.nn.adapterframework.webcontrol;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * DynaActionForm to provide backwards Struts 1.1b-compatibility.
 *
 * Extends the DynaActionForm to implement the Struts 1.1b behaviour, which was that on a {@link #reset(ActionMapping, HttpServletRequest) reset}
 * the defaults of the formvalue's are restored.
 * @version $Id$
 * @author  Johan Verrips
 * @since 4.0
 * <p>Date: Nov 15, 2003
 * Time: 3:39:06 PM</p>
 * 
 */
public class IniDynaActionForm extends DynaActionForm {
	
    public IniDynaActionForm() {
        super();
    }
    public void reset(ActionMapping mapping, HttpServletRequest request ){
        super.reset(mapping, request);
        this.initialize(mapping);
    }
    public void reset(ActionMapping mapping, ServletRequest request) {
        super.reset(mapping,request);
        this.initialize(mapping);
    }
}
