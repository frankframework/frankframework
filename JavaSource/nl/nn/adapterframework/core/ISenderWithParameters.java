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
 * $Log: ISenderWithParameters.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:57:40  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2004/10/19 06:39:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified parameter handling, introduced IWithParameters
 *
 * Revision 1.1  2004/10/05 10:03:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IParameterizedSender
 *
 * Revision 1.5  2004/03/30 07:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.4  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.parameters.ParameterResolutionContext;


/**
 * The <code>IParameterizedSender</code> allows Senders to declare that they accept and may use {@link nl.nn.adapterframework.parameters.Parameter parameters} 
 * 
 * @version HasSender.java,v 1.3 2004/03/23 16:42:59 L190409 Exp $
 * @author  Gerrit van Brakel
 */
public interface ISenderWithParameters extends ISender, IWithParameters {
	
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException;
}
