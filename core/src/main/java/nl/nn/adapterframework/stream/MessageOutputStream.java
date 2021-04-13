/*
   Copyright 2019-2021 WeAreFrank!

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

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.json.JsonTee;
import nl.nn.adapterframework.stream.json.JsonWriter;
import nl.nn.adapterframework.stream.xml.XmlTee;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.XmlWriter;

public class MessageOutputStream implements AutoCloseable {
	protected Logger log = LogUtil.getLogger(this);
	
	private INamedObject owner;
	protected Object requestStream;
	private Message response;
	private PipeForward forward;
	
	private MessageOutputStream nextStream;
	private MessageOutputStream tail;
	
	
	private ThreadConnector threadConnector;
	
	protected MessageOutputStream(INamedObject owner, IForwardTarget next) {
		this.owner=owner;
		tail=this;
		setForward(new PipeForward("success", next==null?null:next.getName()));
	}
	protected MessageOutputStream(INamedObject owner, MessageOutputStream nextStream) {
		this.owner=owner;
		connect(nextStream);
	}
	
	public MessageOutputStream(INamedObject owner, OutputStream stream, IForwardTarget next) {
		this(owner, next);
		this.requestStream=stream;
	}
	public MessageOutputStream(INamedObject owner, OutputStream stream, MessageOutputStream nextStream) {
		this(owner, nextStream);
		this.requestStream=stream;
	}
	
	public MessageOutputStream(INamedObject owner, Writer writer, IForwardTarget next) {
		this(owner, next);
		this.requestStream=writer;
	}
	public MessageOutputStream(INamedObject owner, Writer writer, MessageOutputStream nextStream) {
		this(owner, nextStream);
		this.requestStream=writer;
	}
	
	public MessageOutputStream(INamedObject owner, ContentHandler handler, IForwardTarget next, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, PipeLineSession session) {
		this(owner, next);
		this.requestStream=handler;
		threadConnector = new ThreadConnector(owner, threadLifeCycleEventListener, session);
	}
	public MessageOutputStream(INamedObject owner, ContentHandler handler, MessageOutputStream nextStream, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, PipeLineSession session) {
		this(owner, nextStream);
		this.requestStream=handler;
		threadConnector = new ThreadConnector(owner, threadLifeCycleEventListener, session);
	}
	
	public MessageOutputStream(INamedObject owner, JsonEventHandler handler, IForwardTarget next, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, PipeLineSession session) {
		this(owner, next);
		this.requestStream=handler;
		threadConnector = new ThreadConnector(owner, threadLifeCycleEventListener, session);
	}
	public MessageOutputStream(INamedObject owner, JsonEventHandler handler, MessageOutputStream nextStream, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, PipeLineSession session) {
		this(owner, nextStream);
		this.requestStream=handler;
		threadConnector = new ThreadConnector(owner, threadLifeCycleEventListener, session);
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
	public void afterClose() throws Exception {
		// can be overridden when necessary
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
				afterClose();
			}
		}
	}

	public boolean isBinary() {
		return requestStream instanceof OutputStream;
	}

	public Object asNative() {
		return requestStream;
	}

	private String getLogPrefix() {
		if (owner==null) {
			return "";
		}
		return "MessageOutputStream of ("+owner.getClass().getName()+") ["+owner.getName()+"] ";
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
			return new WriterOutputStream((Writer) requestStream, StringUtils.isNotEmpty(charset)?charset:StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
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
				return new OutputStreamWriter((OutputStream) requestStream, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
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

	public JsonEventHandler asJsonEventHandler() throws StreamingException {
		if (requestStream instanceof JsonEventHandler) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning JsonEventHandler as JsonEventHandler");
			return (JsonEventHandler) requestStream;
		}
		if (requestStream instanceof ContentHandler) {
			throw new StreamingException("Cannot handle JSON as XML");
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
		if (requestStream instanceof Writer) {
			requestStream = StreamUtil.captureWriter((Writer)requestStream, writer, maxSize);
			return;
		}
		if (requestStream instanceof ContentHandler) {
			requestStream = new XmlTee((ContentHandler)requestStream, new PrettyPrintFilter(new XmlWriter(StreamUtil.limitSize(writer, maxSize))));
			return;
		}
		if (requestStream instanceof JsonEventHandler) {
			requestStream = new JsonTee((JsonEventHandler)requestStream, new JsonWriter(StreamUtil.limitSize(writer, maxSize)));
			return;
		}
		if (requestStream instanceof OutputStream) {
			requestStream = StreamUtil.captureOutputStream((OutputStream)requestStream, new WriterOutputStream(writer,StreamUtil.DEFAULT_CHARSET), maxSize);
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
		if (requestStream instanceof OutputStream) {
			requestStream = StreamUtil.captureOutputStream((OutputStream)requestStream, outputStream, maxSize);
			return;
		}
		if (requestStream instanceof ContentHandler) {
			requestStream = new XmlTee((ContentHandler)requestStream, new PrettyPrintFilter(new XmlWriter(StreamUtil.limitSize(outputStream, maxSize))));
			return;
		}
		if (requestStream instanceof JsonEventHandler) {
			requestStream = new JsonTee((JsonEventHandler)requestStream, new JsonWriter(StreamUtil.limitSize(outputStream, maxSize)));
			return;
		}
		if (requestStream instanceof Writer) {
			requestStream = StreamUtil.captureWriter((Writer)requestStream, new OutputStreamWriter(outputStream,StreamUtil.DEFAULT_CHARSET), maxSize);
			return;
		}
		log.warn("captureBinaryStream() called before stream is installed.");
	}
	
	/**
	 * Response message, e.g. the filename, of the {IOutputStreamTarget target}
	 * after processing the stream. It is the responsability of the
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
		if (next!=null && next instanceof IOutputStreamingSupport) {
			nextProvider = (IOutputStreamingSupport)next;
			if (next instanceof StreamingPipe && !((StreamingPipe)next).isStreamingActive()) {
				nextProvider=null;
			}
		}
		MessageOutputStream target = nextProvider==null ? null : nextProvider.provideOutputStream(session, null);
		if (target==null) {
			target=new MessageOutputStreamCap(owner, next);
		}
		return target;
	}
}
