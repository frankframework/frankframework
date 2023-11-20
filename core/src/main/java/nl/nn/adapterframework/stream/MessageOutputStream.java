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
package nl.nn.adapterframework.stream;

import java.io.ByteArrayOutputStream;
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
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.stream.document.Json2XmlHandler;
import nl.nn.adapterframework.stream.json.JsonTee;
import nl.nn.adapterframework.stream.json.JsonWriter;
import nl.nn.adapterframework.stream.xml.XmlTee;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamCaptureUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.ThreadConnectingFilter;
import nl.nn.adapterframework.xml.XmlWriter;

public class MessageOutputStream implements AutoCloseable {
	protected static Logger log = LogUtil.getLogger(MessageOutputStream.class);

	private INamedObject owner;
	protected Object requestStream;
	private Message response;
	private PipeForward forward;
	private String conversionCharset;

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
		if (requestStream instanceof Closeable) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "closing stream");
			((Closeable) requestStream).close();
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
		if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning native["+ClassUtils.nameOf(requestStream)+"]");
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
		if (requestStream instanceof OutputStream) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning OutputStream as OutputStream");
			return (OutputStream) requestStream;
		}
		if (requestStream instanceof Writer) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning Writer as OutputStream");
			return new WriterOutputStream((Writer) requestStream, StringUtils.isNotEmpty(charset)?charset:StringUtils.isNotEmpty(conversionCharset)?conversionCharset:StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
		if (requestStream instanceof ContentHandler) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning ContentHandler as OutputStream");
			return new ContentHandlerOutputStream((ContentHandler) requestStream, threadConnector);
		}
		if (requestStream instanceof JsonEventHandler) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning JsonEventHandler as OutputStream");
			return new JsonEventHandlerOutputStream((JsonEventHandler) requestStream, threadConnector);
		}
		return null;
	}

	public Writer asWriter() throws StreamingException {
		if (requestStream instanceof Writer) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning Writer as Writer");
			return (Writer) requestStream;
		}
		if (requestStream instanceof OutputStream) {
			try {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning OutputStream as Writer");
				return new OutputStreamWriter((OutputStream) requestStream, StringUtils.isNotEmpty(conversionCharset)?conversionCharset:StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
		}
		if (requestStream instanceof ContentHandler) {
			try {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning ContentHandler as Writer");
				return new OutputStreamWriter(new ContentHandlerOutputStream((ContentHandler) requestStream, threadConnector), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
		}
		if (requestStream instanceof JsonEventHandler) {
			try {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning JsonEventHandler as Writer");
				return new OutputStreamWriter(new JsonEventHandlerOutputStream((JsonEventHandler) requestStream, threadConnector), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
		}
		return null;
	}

	public ContentHandler asContentHandler() throws StreamingException {
		if (requestStream instanceof ContentHandler) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning ContentHandler as ContentHandler");
			return (ContentHandler) requestStream;
		}
		if (requestStream instanceof JsonEventHandler) {
			throw new StreamingException("Cannot handle XML as JSON");
		}
		if (requestStream instanceof OutputStream) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning OutputStream as ContentHandler");
			XmlWriter xmlWriter = new XmlWriter((OutputStream) requestStream);
			xmlWriter.setIncludeXmlDeclaration(true);
			return xmlWriter;
		}
		if (requestStream instanceof Writer) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning Writer as ContentHandler");
			return new XmlWriter((Writer) requestStream);
		}
		return null;
	}

	public JsonEventHandler asJsonEventHandler() {
		if (requestStream instanceof JsonEventHandler) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning JsonEventHandler as JsonEventHandler");
			return (JsonEventHandler) requestStream;
		}
		if (requestStream instanceof ContentHandler) {
			return new Json2XmlHandler((ContentHandler) requestStream, false);
		}
		if (requestStream instanceof OutputStream) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning OutputStream as JsonEventHandler");
			return new JsonWriter((OutputStream) requestStream);
		}
		if (requestStream instanceof Writer) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning Writer as JsonEventHandler");
			return new JsonWriter((Writer) requestStream);
		}
		return null;
	}

	public StringWriter captureCharacterStream() {
		StringWriter result = new StringWriter();
		captureCharacterStream(result);
		return result;
	}
	public void captureCharacterStream(Writer writer) {
		captureCharacterStream(writer, 10000);
	}
	@SuppressWarnings("resource")
	public void captureCharacterStream(Writer writer, int maxSize) {
		log.debug("creating capture of "+ClassUtils.nameOf(requestStream));
		closeOnClose(writer);
		if (requestStream instanceof Writer) {
			requestStream = StreamCaptureUtils.captureWriter((Writer)requestStream, writer, maxSize);
			return;
		}
		if (requestStream instanceof ContentHandler) {
			requestStream = new XmlTee((ContentHandler)requestStream, new PrettyPrintFilter(new XmlWriter(StreamCaptureUtils.limitSize(writer, maxSize))));
			return;
		}
		if (requestStream instanceof JsonEventHandler) {
			requestStream = new JsonTee((JsonEventHandler)requestStream, new JsonWriter(StreamCaptureUtils.limitSize(writer, maxSize)));
			return;
		}
		if (requestStream instanceof OutputStream) {
			requestStream = StreamCaptureUtils.captureOutputStream((OutputStream)requestStream, new WriterOutputStream(writer,StreamUtil.DEFAULT_CHARSET), maxSize);
			return;
		}
		log.warn("captureCharacterStream() called before stream is installed.");
	}

	public ByteArrayOutputStream captureBinaryStream() {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		captureBinaryStream(result);
		return result;
	}
	public void captureBinaryStream(OutputStream outputStream) {
		captureBinaryStream(outputStream, 10000);
	}
	@SuppressWarnings("resource")
	public void captureBinaryStream(OutputStream outputStream, int maxSize) {
		log.debug("creating capture of "+ClassUtils.nameOf(requestStream));
		closeOnClose(outputStream);
		if (requestStream instanceof OutputStream) {
			requestStream = StreamCaptureUtils.captureOutputStream((OutputStream)requestStream, outputStream, maxSize);
			return;
		}
		if (requestStream instanceof ContentHandler) {
			requestStream = new XmlTee((ContentHandler)requestStream, new PrettyPrintFilter(new XmlWriter(StreamCaptureUtils.limitSize(outputStream, maxSize))));
			return;
		}
		if (requestStream instanceof JsonEventHandler) {
			requestStream = new JsonTee((JsonEventHandler)requestStream, new JsonWriter(StreamCaptureUtils.limitSize(outputStream, maxSize)));
			return;
		}
		if (requestStream instanceof Writer) {
			requestStream = StreamCaptureUtils.captureWriter((Writer)requestStream, new OutputStreamWriter(outputStream,StreamUtil.DEFAULT_CHARSET), maxSize);
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
		IOutputStreamingSupport nextProvider=null;
		if (next instanceof IOutputStreamingSupport) {
			nextProvider = (IOutputStreamingSupport)next;
			if (next instanceof StreamingPipe && !((StreamingPipe)next).isStreamingActive()) {
				nextProvider=null;
			}
		}
		MessageOutputStream target = nextProvider==null ? null : nextProvider.provideOutputStream(session, null);
		if (target!=null) {
			log.debug("OutputStream for {} is provided by {}", ()->ClassUtils.nameOf(owner), ()->ClassUtils.nameOf(next));
			return target;
		}
		log.debug("providing MessageOutputStreamCap for {}", ()->ClassUtils.nameOf(owner));
		return new MessageOutputStreamCap(owner, next);
	}
}
