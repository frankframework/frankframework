/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2024 WeAreFrank

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
package org.frankframework.pipes;

import java.security.Principal;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.stream.Message;

/**
 * Returns the name of the user executing the request.
 *
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@ElementType(ElementTypes.SESSION)
public class GetPrincipalPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException{
		Principal principal=session.getSecurityHandler().getPrincipal();
		String principalName = "";
		if (principal==null) {
			log.warn("no principal found");
		} else {
			try {
				principalName = principal.getName();
			} catch (Throwable e) {
				throw new PipeRunException(this,"got exception getting name from principal",e);
			}
		}

		return new PipeRunResult(getSuccessForward(),principalName);
	}
}
