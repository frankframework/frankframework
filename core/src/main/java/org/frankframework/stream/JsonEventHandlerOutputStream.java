/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.stream;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.logging.log4j.Logger;

import org.frankframework.stream.json.JsonUtils;
import org.frankframework.util.LogUtil;

/**
 * Helper class to convert character or byte based OutputStreams into a JSON SAX event stream.
 * Uses OS pipes to convert an OutputStream into an InputStream
 *
 * @author Gerrit van Brakel
 */
public class JsonEventHandlerOutputStream extends PipedOutputStream implements Thread.UncaughtExceptionHandler {
	protected Logger log = LogUtil.getLogger(this);

	private final JsonEventHandler handler;

	private final ThreadConnector threadConnector;

	private PipedInputStream pipedInputStream=new PipedInputStream();
	private final EventConsumer pipeReader=new EventConsumer();
	private Throwable exception;

	public JsonEventHandlerOutputStream(JsonEventHandler handler, ThreadConnector threadConnector) throws StreamingException {
		this.handler=handler;
		this.threadConnector=threadConnector;
		try {
			pipedInputStream=new PipedInputStream();
			connect(pipedInputStream);
			pipeReader.setUncaughtExceptionHandler(this);
			pipeReader.start();
		} catch (IOException e) {
			throw new StreamingException(e);
		}
	}

	private class EventConsumer extends Thread {

		@Override
		public void run() {
			try {
				threadConnector.startThread(null);
				JsonUtils.parseJson(pipedInputStream, handler);
				threadConnector.endThread(null);
			} catch (Exception e) {
				Throwable t = threadConnector.abortThread(e);
				StreamingException se = new StreamingException(t);
				setException(se);
			}
		}
	}

	@Override
	public void close() throws IOException {
		try {
			super.close();
		} finally {
			try {
				pipeReader.join();
				if (getException()!=null) {
					throw new IOException(getException());
				}
			} catch (InterruptedException e) {
				log.warn("thread interrupted", e);
			} finally {
				threadConnector.close();
			}
		}
	}

	@Override
	public void uncaughtException(Thread arg0, Throwable t) {
		setException(t);
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}
	public Throwable getException() {
		return exception;
	}
}
