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

import nl.nn.adapterframework.stream.Message;

/**
 * Runnable object for calling a request reply service. When a
 * <code>Throwable</code> has been thrown during execution it should be returned
 * by getThrowable() otherwise the reply should be returned by getReply().
 *
 * @author Jaco de Groot
 */
public abstract class RequestReplyExecutor implements Runnable {
	protected String correlationID;
	protected Message request;
	protected Message reply;
	protected Throwable throwable;

	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}

	public String getCorrelationID() {
		return correlationID;
	}

	public void setRequest(Message request) {
		this.request = request;
	}

	public Message getRequest() {
		return request;
	}

	public void setReply(Message reply) {
		this.reply = reply;
	}

	public Message getReply() {
		return reply;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public Throwable getThrowable() {
		return throwable;
	}

}
