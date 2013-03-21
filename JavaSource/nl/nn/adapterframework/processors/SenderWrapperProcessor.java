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
 * $Log: SenderWrapperProcessor.java,v $
 * Revision 1.3  2011-11-30 13:51:53  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/09/13 14:02:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * split SenderWrapper-processing in chain of processors
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.SenderWrapperBase;

/**
 * Interface for handlers in SenderWrapper processor chain.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version $Id$
 */
public interface SenderWrapperProcessor {

	public String sendMessage(SenderWrapperBase senderWrapperBase, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException;

}
