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
package nl.nn.adapterframework.extensions.sap.jco2;

import com.sap.mw.jco.JCO;
import com.sap.mw.idoc.IDoc;

/**
 * The interface clients (users) of a SAP function must implement.
 *
 * @author  Gerrit van Brakel
 * @version $Id$
 */
public interface SapFunctionHandler {

	public void processFunctionCall(JCO.Function function) throws SapException;
	public void processIDoc(IDoc.Document idoc) throws SapException;
}
