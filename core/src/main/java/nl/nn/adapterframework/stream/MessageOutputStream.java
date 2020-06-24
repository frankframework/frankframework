/*
   Copyright 2019, 2020 WeAreFrank!

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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.logging.log4j.Logger;
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.xml.XmlWriter;

public class MessageOutputStream implements AutoCloseable {
	protected Logger log = LogUtil.getLogger(this);
	
	private INamedObject owner;
	protected Object requestStream;
	private Object response;
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
	
	public MessageOutputStream(INamedObject owner, ContentHandler handler, IForwardTarget next, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, IPipeLineSession session) {
		this(owner, next);
		this.requestStream=handler;
		threadConnector = new ThreadConnector(owner, threadLifeCycleEventListener, session);
	}
	public MessageOutputStream(INamedObject owner, ContentHandler handler, MessageOutputStream nextStream, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, IPipeLineSession session) {
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
		if (requestStream instanceof OutputStream) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning OutputStream as OutputStream");
			return (OutputStream) requestStream;
		}
		if (requestStream instanceof Writer) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning Writer as OutputStream");
			return new WriterOutputStream((Writer) requestStream, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
		if (requestStream instanceof ContentHandler) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "returning ContentHandler as OutputStream");
			return new ContentHandlerOutputStream((ContentHandler) requestStream, threadConnector);
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
		return null;
	}

	public ContentHandler asContentHandler() throws StreamingException {
		if (requestStream instanceof ContentHandler) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"returning ContentHandler as ContentHandler");
			return (ContentHandler) requestStream;
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

	/**
	 * Response message, e.g. the filename, of the {IOutputStreamTarget target}
	 * after processing the stream. It is the responsability of the
	 * {@link MessageOutputStream target} to set this message.
	 */
	public void setResponse(Object response) {
		this.response = response;
	}
	public Object getResponse() {
		return response;
	}

	public void setForward(PipeForward forward) {
		this.forward = forward;
	}


	public PipeRunResult getPipeRunResult() {
		Object response = tail.getResponse();
		return new PipeRunResult(getForward(), response);
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
	public static MessageOutputStream getTargetStream(INamedObject owner, IPipeLineSession session, IForwardTarget next) throws StreamingException {
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
