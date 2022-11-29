/*
   Copyright 2022 WeAreFrank!

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

import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.IStreamingSender;
import nl.nn.adapterframework.stream.Message;

/**
 * Marker interface for results of Senders and Pipes.
 * @see IStreamingSender

 * @author Gerrit van Brakel
 *
 */
public interface ProcessBlockResult {

	public boolean isSuccess();
	public Message getResult();
	public void setResult(Message result);

	default String getForwardName() {
		PipeRunResult pipeRunResult = (PipeRunResult)this;
		return pipeRunResult.getPipeForward()!=null?pipeRunResult.getPipeForward().getName():null;
	}

	default SenderResult asSenderResult() {
		if (this instanceof SenderResult) {
			return (SenderResult)this;
		}
		// if a SenderResult is needed, a next pipe to stream to cannot have been supplied, so the result can only be local.
		return new SenderResult(isSuccess(),getResult(),null, getForwardName());
	}

	default PipeRunResult asPipeRunResult(FixedForwardPipe caller) throws SenderException {
		if (this instanceof PipeRunResult) {
			return (PipeRunResult)this;
		}
		SenderResult senderResult = (SenderResult)this;
		PipeForward forward = caller.findForward(senderResult.getForwardName());
		if (forward==null) {
			forward = isSuccess() ? caller.getSuccessForward() : caller.findForward(PipeForward.EXCEPTION_FORWARD_NAME);
		}
		if (forward==null && !isSuccess()) {
			throw new SenderException(senderResult.getErrorMessage());
		}
		return new PipeRunResult(forward, senderResult.getResult());
	}
}
