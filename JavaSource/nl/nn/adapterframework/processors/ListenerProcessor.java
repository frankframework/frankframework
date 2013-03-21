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
 * $Log: ListenerProcessor.java,v $
 * Revision 1.5  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.4  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.TimeOutException;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public interface ListenerProcessor {

	public String getMessage(ICorrelatedPullingListener listener,
			String correlationID, IPipeLineSession pipeLineSession
			) throws ListenerException, TimeOutException;

}
