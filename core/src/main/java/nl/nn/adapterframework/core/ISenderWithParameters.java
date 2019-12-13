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
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.parameters.ParameterResolutionContext;


/**
 * The <code>ISenderWithParameters</code> allows Senders to declare that they accept and may use {@link nl.nn.adapterframework.parameters.Parameter parameters} 
 * 
 * @author  Gerrit van Brakel
 */
public interface ISenderWithParameters extends ISender, IWithParameters {
	
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException;
}
