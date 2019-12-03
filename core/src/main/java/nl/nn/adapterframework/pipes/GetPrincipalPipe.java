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
package nl.nn.adapterframework.pipes;

import java.security.Principal;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Returns the name of the user executing the request.
 *
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class GetPrincipalPipe extends FixedForwardPipe {
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException{
		Principal principal=session.getPrincipal();
		String principalName = "";
		if (principal==null) {
			log.warn(getLogPrefix(session)+"no principal found");
		} else {
			try {
				principalName = principal.getName();
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception getting name from principal",e);
			}
		}

		return new PipeRunResult(getForward(),principalName);
	}
}
