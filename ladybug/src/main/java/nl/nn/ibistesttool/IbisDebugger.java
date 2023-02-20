/*
   Copyright 2018 Nationale-Nederlanden, 2021-2022 WeAreFrank!

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
package nl.nn.ibistesttool;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

/**
 * Interface to be implemented by an Ibis debugger. The debugger will be
 * notified of events by a call to the appropriate method for that event.
 * 
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */
public interface IbisDebugger {

	public void setIbisManager(IbisManager ibisManager);
	public IbisManager getIbisManager();

	public Message pipeLineInput(PipeLine pipeLine, String correlationId, Message input);
	public Message pipeLineOutput(PipeLine pipeLine, String correlationId, Message output);
	public Throwable pipeLineAbort(PipeLine pipeLine, String correlationId, Throwable throwable);

	public Object pipeLineSessionKey(String correlationId, String sessionKey, Object sessionValue);

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

	public Object parameterResolvedTo(Parameter parameter, String correlationId, Object value);
	public <T> T showValue(String correlationId, String label, T value);

	public boolean stubSender(ISender sender, String correlationId);

	public boolean stubReplyListener(IListener<?> listener, String correlationId);

}
