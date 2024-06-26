/*
   Copyright 2019-2022 WeAreFrank!

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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.IForwardTarget;
import org.frankframework.core.INamedObject;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.jta.IThreadConnectableTransactionManager;
import org.frankframework.stream.document.Json2XmlHandler;
import org.frankframework.stream.json.JsonTee;
import org.frankframework.stream.json.JsonWriter;
import org.frankframework.stream.xml.XmlTee;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamCaptureUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.ThreadConnectingFilter;
import org.frankframework.xml.XmlWriter;
import org.xml.sax.ContentHandler;

public class MessageOutputStream implements AutoCloseable {
	protected static Logger log = LogUtil.getLogger(MessageOutputStream.class);

	private final INamedObject owner;
	protected Object requestStream;
	private Message response;
	private PipeForward forward;
	private final String conversionCharset;

	private MessageOutputStream nextStream;
	private MessageOutputStream tail;

	private Set<AutoCloseable> resourcesToClose;

	private ThreadConnector<?> threadConnector;
	private ThreadConnector<?> targetThreadConnector;

	protected MessageOutputStream(INamedObject owner, IForwardTarget next, String conversionCharset) {
		this.owner=owner;
		this.conversionCharset=conversionCharset;
		tail=this;
		setForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, next==null?null:next.getName()));
	}
	protected MessageOutputStream(INamedObject owner, MessageOutputStream nextStream, String conversionCharset) {
		this.owner=owner;
		this.conversionCharset=conversionCharset;
		connect(nextStream);
	}

	public MessageOutputStream(INamedObject owner, OutputStream stream, IForwardTarget next) {
		this(owner, stream, next, null);
	}
	public MessageOutputStream(INamedObject owner, OutputStream stream, MessageOutputStream nextStream) {
		this(owner, stream, nextStream, null);
	}
	public MessageOutputStream(INamedObject owner, OutputStream stream, IForwardTarget next, String conversionCharset) {
		this(owner, next, conversionCharset);
		this.requestStream=stream;
	}
	public MessageOutputStream(INamedObject owner, OutputStream stream, MessageOutputStream nextStream, String conversionCharset) {
		this(owner, nextStream, conversionCharset);
		this.requestStream=stream;
	}

	public MessageOutputStream(INamedObject owner, Writer writer, IForwardTarget next) {
		this(owner, writer, next, null);
	}
	public MessageOutputStream(INamedObject owner, Writer writer, MessageOutputStream nextStream) {
		this(owner, writer, nextStream, null);
	}
	public MessageOutputStream(INamedObject owner, Writer writer, IForwardTarget next, String conversionCharset) {
		this(owner, next, conversionCharset);
		this.requestStream=writer;
	}
	public MessageOutputStream(INamedObject owner, Writer writer, MessageOutputStream nextStream, String conversionCharset) {
		this(owner, nextStream, conversionCharset);
		this.requestStream=writer;
	}

	// this constructor for testing only
	<T> MessageOutputStream(ContentHandler handler) {
		this(null, (IForwardTarget)null, null);
		threadConnector = new ThreadConnector<T>(null, null, null, null, (PipeLineSession)null);
		this.requestStream=new ThreadConnectingFilter(threadConnector, handler);
	}
	public <T> MessageOutputStream(INamedObject owner, ContentHandler handler, MessageOutputStream nextStream, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, IThreadConnectableTransactionManager txManager, PipeLineSession session, ThreadConnector<?> targetThreadConnector) {
		this(owner, nextStream, null);
		threadConnector = new ThreadConnector<T>(owner, "ContentHandler-MessageOutputStream", threadLifeCycleEventListener, txManager, session);
		this.requestStream=new ThreadConnectingFilter(threadConnector, handler);
		this.targetThreadConnector = targetThreadConnector;
	}

	// this constructor for testing only
	<T> MessageOutputStream(JsonEventHandler handler) {
		this(null, (IForwardTarget)null, null);
		this.requestStream=handler;
		threadConnector = new ThreadConnector<T>(null, null, null, null, (PipeLineSession)null);
	}
	public <T> MessageOutputStream(INamedObject owner, JsonEventHandler handler, MessageOutputStream nextStream, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, IThreadConnectableTransactionManager txManager, PipeLineSession session, ThreadConnector<?> targetThreadConnector) {
		this(owner, nextStream, null);
		this.requestStream=handler;
		threadConnector = new ThreadConnector<T>(owner, "JsonEventHandler-MessageOutputStream", threadLifeCycleEventListener, txManager, session);
		this.targetThreadConnector = targetThreadConnector;
		//TODO apply ThreadConnectingFilter, to make sure transaction is properly resumed in child thread
	}



	private void connect(MessageOutputStream nextStream) {
		this.nextStream=nextStream;
		if (nextStream==null) {
			tail=this;
		} else {
			tail=nextStream.tail;
		}
	}

	protected void setRequestStream(Object requestStream) {
		this.requestStream = requestStream;
	}

	public void closeRequestStream() throws IOException {
		if (requestStream instanceof Closeable closeable) {
			if (log.isDebugEnabled()) log.debug("{}closing stream", getLogPrefix());
			closeable.close();
		}
	}

	/**
	 * can be overridden in descender classes to release resources, after the chain has been closed.
	 */
	public void afterClose() throws SQLException {
		// can be overridden when necessary
	}

	public void closeOnClose(AutoCloseable resource) {
		if (resourcesToClose==null) {
			resourcesToClose = new LinkedHashSet<>();
		}
		resourcesToClose.add(resource);
	}

	@Override
	public final void close() throws Exception {
		try {
			closeRequestStream();
		} finally {
			try {
				if (nextStream!=null) {
					nextStream.close();
				}
			} finally {
				try {
					try {
						if (targetThreadConnector!=null) {
							targetThreadConnector.close();
						}
					} finally {
						if (threadConnector!=null) {
							threadConnector.close();
						}
					}
				} finally {
					try {
						afterClose();
					} finally {
						if (resourcesToClose!=null) {
							resourcesToClose.forEach(r -> {
								try {
									r.close();
								} catch (Exception e) {
									log.warn("Could not close resource", e);
								}
							});
						}
					}
				}
			}
		}
	}

	public boolean isBinary() {
		return requestStream instanceof OutputStream;
	}

	public Object asNative() {
		if (log.isDebugEnabled()) log.debug("{}returning native[{}]", getLogPrefix(), ClassUtils.nameOf(requestStream));
		return requestStream;
	}

	private String getLogPrefix() {
		if (owner==null) {
			return "";
		}
		return "MessageOutputStream of "+ClassUtils.nameOf(owner)+" ";
	}

	public OutputStream asStream() throws StreamingException {
		return asStream(null);
	}

	public OutputStream asStream(String charset) throws StreamingException {
		if (requestStream instanceof OutputStream stream) {
			if (log.isDebugEnabled()) log.debug("{}returning OutputStream as OutputStream", getLogPrefix());
			return stream;
		}
		if (requestStream instanceof Writer writer) {
			if (log.isDebugEnabled()) log.debug("{}returning Writer as OutputStream", getLogPrefix());
			return new WriterOutputStream(writer, StringUtils.isNotEmpty(charset)?charset:StringUtils.isNotEmpty(conversionCharset)?conversionCharset:StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
		if (requestStream instanceof ContentHandler handler) {
			if (log.isDebugEnabled()) log.debug("{}returning ContentHandler as OutputStream", getLogPrefix());
			return new ContentHandlerOutputStream(handler, threadConnector);
		}
		if (requestStream instanceof JsonEventHandler handler) {
			if (log.isDebugEnabled()) log.debug("{}returning JsonEventHandler as OutputStream", getLogPrefix());
			return new JsonEventHandlerOutputStream(handler, threadConnector);
		}
		return null;
	}

	public Writer asWriter() throws StreamingException {
		if (requestStream instanceof Writer writer) {
			if (log.isDebugEnabled()) log.debug("{}returning Writer as Writer", getLogPrefix());
			return writer;
		}
		if (requestStream instanceof OutputStream stream) {
			try {
				if (log.isDebugEnabled()) log.debug("{}returning OutputStream as Writer", getLogPrefix());
				return new OutputStreamWriter(stream, StringUtils.isNotEmpty(conversionCharset)?conversionCharset:StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
		}
		if (requestStream instanceof ContentHandler handler) {
			try {
				if (log.isDebugEnabled()) log.debug("{}returning ContentHandler as Writer", getLogPrefix());
				return new OutputStreamWriter(new ContentHandlerOutputStream(handler, threadConnector), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
		}
		if (requestStream instanceof JsonEventHandler handler) {
			try {
				if (log.isDebugEnabled()) log.debug("{}returning JsonEventHandler as Writer", getLogPrefix());
				return new OutputStreamWriter(new JsonEventHandlerOutputStream(handler, threadConnector), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
		}
		return null;
	}

	public ContentHandler asContentHandler() throws StreamingException {
		if (requestStream instanceof ContentHandler handler) {
			if (log.isDebugEnabled()) log.debug("{}returning ContentHandler as ContentHandler", getLogPrefix());
			return handler;
		}
		if (requestStream instanceof JsonEventHandler) {
			throw new StreamingException("Cannot handle XML as JSON");
		}
		if (requestStream instanceof OutputStream stream) {
			if (log.isDebugEnabled()) log.debug("{}returning OutputStream as ContentHandler", getLogPrefix());
			XmlWriter xmlWriter = new XmlWriter(stream);
			xmlWriter.setIncludeXmlDeclaration(true);
			return xmlWriter;
		}
		if (requestStream instanceof Writer writer) {
			if (log.isDebugEnabled()) log.debug("{}returning Writer as ContentHandler", getLogPrefix());
			return new XmlWriter(writer);
		}
		return null;
	}

	public JsonEventHandler asJsonEventHandler() {
		if (requestStream instanceof JsonEventHandler handler) {
			if (log.isDebugEnabled()) log.debug("{}returning JsonEventHandler as JsonEventHandler", getLogPrefix());
			return handler;
		}
		if (requestStream instanceof ContentHandler handler) {
			return new Json2XmlHandler(handler, false);
		}
		if (requestStream instanceof OutputStream stream) {
			if (log.isDebugEnabled()) log.debug("{}returning OutputStream as JsonEventHandler", getLogPrefix());
			return new JsonWriter(stream);
		}
		if (requestStream instanceof Writer writer) {
			if (log.isDebugEnabled()) log.debug("{}returning Writer as JsonEventHandler", getLogPrefix());
			return new JsonWriter(writer);
		}
		return null;
	}

	public StringWriter captureCharacterStream() {
		StringWriter result = new StringWriter();
		captureCharacterStream(result, 10_000);
		return result;
	}

	@SuppressWarnings("resource")
	public void captureCharacterStream(Writer writer, int maxSize) {
		log.debug("creating capture of {}", ClassUtils.nameOf(requestStream));
		closeOnClose(writer);
		if (requestStream instanceof Writer writer1) {
			requestStream = StreamCaptureUtils.captureWriter(writer1, writer, maxSize);
			return;
		}
		if (requestStream instanceof ContentHandler handler) {
			requestStream = new XmlTee(handler, new PrettyPrintFilter(new XmlWriter(StreamCaptureUtils.limitSize(writer, maxSize))));
			return;
		}
		if (requestStream instanceof JsonEventHandler handler) {
			requestStream = new JsonTee(handler, new JsonWriter(StreamCaptureUtils.limitSize(writer, maxSize)));
			return;
		}
		if (requestStream instanceof OutputStream stream) {
			requestStream = StreamCaptureUtils.captureOutputStream(stream, new WriterOutputStream(writer,StreamUtil.DEFAULT_CHARSET), maxSize);
			return;
		}
		log.warn("captureCharacterStream() called before stream is installed.");
	}

	@SuppressWarnings("resource")
	public void captureBinaryStream(OutputStream outputStream, int maxSize) {
		log.debug("creating capture of {}", ClassUtils.nameOf(requestStream));
		closeOnClose(outputStream);
		if (requestStream instanceof OutputStream stream) {
			requestStream = StreamCaptureUtils.captureOutputStream(stream, outputStream, maxSize);
			return;
		}
		if (requestStream instanceof ContentHandler handler) {
			requestStream = new XmlTee(handler, new PrettyPrintFilter(new XmlWriter(StreamCaptureUtils.limitSize(outputStream, maxSize))));
			return;
		}
		if (requestStream instanceof JsonEventHandler handler) {
			requestStream = new JsonTee(handler, new JsonWriter(StreamCaptureUtils.limitSize(outputStream, maxSize)));
			return;
		}
		if (requestStream instanceof Writer writer) {
			requestStream = StreamCaptureUtils.captureWriter(writer, new OutputStreamWriter(outputStream,StreamUtil.DEFAULT_CHARSET), maxSize);
			return;
		}
		log.warn("captureBinaryStream() called before stream is installed.");
	}

	/**
	 * Response message, e.g. the filename, of the {IOutputStreamTarget target}
	 * after processing the stream. It is the responsibility of the
	 * {@link MessageOutputStream target} to set this message.
	 */
	public void setResponse(Message response) {
		this.response = response;
	}
	public Message getResponse() {
		return response;
	}

	public void setForward(PipeForward forward) {
		this.forward = forward;
	}


	public PipeRunResult getPipeRunResult() {
		return new PipeRunResult(getForward(), tail.getResponse());
	}

	public PipeForward getForward() {
		if (nextStream!=null) {
			PipeForward result = nextStream.getForward();
			if (result!=null) {
				return result;
			}
		}
		return forward;
	}

	/**
	 * Provides a non-null MessageOutputStream, that the caller can use to obtain a Writer, OutputStream or ContentHandler.
	 */
	public static MessageOutputStream getTargetStream(INamedObject owner, PipeLineSession session, IForwardTarget next) throws StreamingException {
		log.debug("providing MessageOutputStreamCap for {}", ()->ClassUtils.nameOf(owner));
		return new MessageOutputStreamCap(owner, next);
	}
}
