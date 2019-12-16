/*
   Copyright 2019 Integration Partners

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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.xml.XmlWriter;

public class MessageOutputStream {
	protected Logger log = LogUtil.getLogger(this);
	
	private Object requestStream;
	private Object response;
	
//	private MessageOutputStream next;
	private MessageOutputStream tail;

	private ThreadConnector threadConnector;

	public MessageOutputStream(OutputStream stream, MessageOutputStream next) {
		this.requestStream=stream;
		connect(next);
	}
	
	public MessageOutputStream(Writer writer, MessageOutputStream next) {
		this.requestStream=writer;
		connect(next);
	}
	
	public MessageOutputStream(ContentHandler handler, MessageOutputStream next, Object owner, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, String correlationID) {
		this.requestStream=handler;
		threadConnector = new ThreadConnector(owner, threadLifeCycleEventListener, correlationID);
		connect(next);
	}
	
	public MessageOutputStream(OutputStream stream, MessageOutputStream next, Object response) {
		this(stream,next);
		this.response=response;
	}
	
	public MessageOutputStream(Writer writer, MessageOutputStream next, Object response) {
		this(writer,next);
		this.response=response;
	}
	
	public MessageOutputStream(ContentHandler handler, MessageOutputStream next, Object response, Object owner, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, String correlationID) {
		this(handler,next,owner,threadLifeCycleEventListener,correlationID);
		this.response=response;
	}

	private void connect(MessageOutputStream next) {
//		this.next=next;
		if (next==null) {
			tail=this;			
		} else {
			tail=next.tail;
		}
	}

	public Object asNative() {
		return requestStream;
	}

	public OutputStream asStream() throws StreamingException {
    	if (requestStream instanceof OutputStream) {
    		log.debug("returning OutputStream as OutputStream");
    		return (OutputStream)requestStream;
    	}
    	if (requestStream instanceof Writer) {
    		log.debug("returning Writer as OutputStream");
    		return new WriterOutputStream((Writer)requestStream,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
    	}
    	if (requestStream instanceof ContentHandler) {
    		log.debug("returning ContentHandler as OutputStream");
    		return new ContentHandlerOutputStream((ContentHandler)requestStream, threadConnector);
    	}
    	return null;
	}
	
	public Writer asWriter() throws StreamingException {
    	if (requestStream instanceof Writer) {
    		log.debug("returning Writer as Writer");
    		return (Writer)requestStream;
    	}
    	if (requestStream instanceof OutputStream) {
    		try {
        		log.debug("returning OutputStream as Writer");
				return new OutputStreamWriter((OutputStream)requestStream,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
    	}
    	if (requestStream instanceof ContentHandler) {
    		try {
        		log.debug("returning ContentHandler as Writer");
    	   		return new OutputStreamWriter(new ContentHandlerOutputStream((ContentHandler)requestStream, threadConnector),StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new StreamingException(e);
			}
    	}
    	return null;
	}

	public ContentHandler asContentHandler() throws StreamingException {
    	if (requestStream instanceof ContentHandler) {
    		log.debug("returning ContentHandler as ContentHandler");
    		return (ContentHandler)requestStream;
    	}
    	if (requestStream instanceof OutputStream) {
    		log.debug("returning OutputStream as ContentHandler");
    		XmlWriter xmlWriter = new XmlWriter((OutputStream)requestStream);
    		xmlWriter.setIncludeXmlDeclaration(true);
    		return xmlWriter;
    	}
    	if (requestStream instanceof Writer) {
    		log.debug("returning Writer as ContentHandler");
    		return new XmlWriter((Writer)requestStream);
    	}
    	return null;
		
	}


    /**
     * Response message, e.g. the filename, of the {IOutputStreamTarget target} after processing the stream. 
     * It is the responsability of the {@link MessageOutputStream target} to set this message.
     */
	public void setResponse(Object response) {
		this.response = response;
	}
    
    public Object getResponse() {
		return tail.response;
	}

    public String getResponseAsString() {
		return getResponse()==null?null:getResponse().toString();
	}

}
