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
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class InputOutputPipeProcessor extends PipeProcessorBase {

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, String messageId, Object message, IPipeLineSession pipeLineSession) throws PipeRunException {
		Object preservedObject = message;
		PipeRunResult pipeRunResult;
		INamedObject owner = pipeLine.getOwner();

		IExtendedPipe pe=null;
			
		if (pipe instanceof IExtendedPipe) {
			pe = (IExtendedPipe)pipe;
		}
		
		if (pe!=null) {
			if (StringUtils.isNotEmpty(pe.getGetInputFromSessionKey())) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] replacing input for pipe ["+pe.getName()+"] with contents of sessionKey ["+pe.getGetInputFromSessionKey()+"]");
				message=pipeLineSession.get(pe.getGetInputFromSessionKey());
			}
			if (StringUtils.isNotEmpty(pe.getGetInputFromFixedValue())) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] replacing input for pipe ["+pe.getName()+"] with fixed value ["+pe.getGetInputFromFixedValue()+"]");
				message=pe.getGetInputFromFixedValue();
			}
		}

		pipeRunResult=pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		if (pipeRunResult==null){
			throw new PipeRunException(pipe, "Pipeline of ["+pipeLine.getOwner().getName()+"] received null result from pipe ["+pipe.getName()+"]d");
		}

		if (pe !=null) {
			if (StringUtils.isNotEmpty(pe.getStoreResultInSessionKey())) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] storing result for pipe ["+pe.getName()+"] under sessionKey ["+pe.getStoreResultInSessionKey()+"]");
				Object result = pipeRunResult.getResult();
				pipeLineSession.put(pe.getStoreResultInSessionKey(),result);
			}
			if (pe.isPreserveInput()) {
				pipeRunResult.setResult(preservedObject);
			}
		}

		return pipeRunResult;
	}

}
