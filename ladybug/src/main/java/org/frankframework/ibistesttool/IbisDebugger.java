/*
   Copyright 2018 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.ibistesttool;

import org.frankframework.configuration.IbisManager;
import org.frankframework.core.IListener;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLine;
import org.frankframework.parameters.IParameter;
import org.frankframework.stream.Message;

/**
 * Interface to be implemented by an Ibis debugger. The debugger will be
 * notified of events by a call to the appropriate method for that event.
 *
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */
public interface IbisDebugger {

	public void setIbisManager(IbisManager ibisManager);
	public IbisManager getIbisManager();

	public Message pipelineInput(PipeLine pipeLine, String correlationId, Message input);
	public Message pipelineOutput(PipeLine pipeLine, String correlationId, Message output);
	public Message pipelineAbort(PipeLine pipeLine, String correlationId, Message output);
	public Throwable pipelineAbort(PipeLine pipeLine, String correlationId, Throwable throwable);

	public Object pipelineSessionKey(String correlationId, String sessionKey, Object sessionValue);

	public <T> T pipeInput(PipeLine pipeLine, IPipe pipe, String correlationId, T input);
	public <T> T pipeOutput(PipeLine pipeLine, IPipe pipe, String correlationId, T output);
	public Throwable pipeAbort(PipeLine pipeLine, IPipe pipe, String correlationId, Throwable throwable);

	public <T> T senderInput(ISender sender, String correlationId, T input);
	public <T> T senderOutput(ISender sender, String correlationId, T output);
	public Throwable senderAbort(ISender sender, String correlationId, Throwable throwable);

	public String replyListenerInput(IListener<?> listener, String messageId, String correlationId);
	public <M> M replyListenerOutput(IListener<M> listener, String correlationId, M output);
	public Throwable replyListenerAbort(IListener<?> listener, String correlationId, Throwable throwable);

	public void createThread(Object sourceObject, String threadId, String correlationId);
	public void cancelThread(Object sourceObject, String threadId, String correlationId);
	public Object startThread(Object sourceObject, String threadId, String correlationId, Object input);
	public Object endThread(Object sourceObject, String correlationId, Object output);
	public Throwable abortThread(Object sourceObject, String correlationId, Throwable throwable);

	public Object getInputFromSessionKey(String correlationId, String sessionKey, Object sessionValue);
	public Object getInputFromFixedValue(String correlationId, Object fixedValue);
	public Object getEmptyInputReplacement(String correlationId, Object replacementValue);
	public Object storeInSessionKey(String correlationId, String sessionKey, Object result);
	public Message preserveInput(String correlationId, Message input);

	public Object parameterResolvedTo(IParameter parameter, String correlationId, Object value);
	public <T> T showInputValue(String correlationId, String label, T value);
	public <T> T showOutputValue(String correlationId, String label, T value);

	public boolean stubSender(ISender sender, String correlationId);

	public boolean stubReplyListener(IListener<?> listener, String correlationId);

}
