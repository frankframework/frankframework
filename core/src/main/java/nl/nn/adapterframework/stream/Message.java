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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

public class Message {
	protected Logger log = LogUtil.getLogger(this);

	private Object request;
	
	public Message(Object request) {
		this.request=request;
	}

	public void preserveInputStream() throws IOException {
		if (request==null) {
			return;
		}
		if (request instanceof Reader) {
    		log.debug("preserving Reader as String");
			request = StreamUtil.readerToString((Reader)request, null);
			return;
		}
		if (request instanceof InputStream) {
    		log.debug("preserving InputStream as byte[]");
			request = StreamUtil.streamToByteArray((InputStream)request, false);
			return;
		}
	}
	

	public Reader asReader() {
		if (request==null) {
			return null;
		}
		if (request instanceof Reader) {
    		log.debug("returning Reader as Reader");
			return(Reader)request;
		} 
		if (request instanceof InputStream) {
			try {
	    		log.debug("returning InputStream as Reader");
				return new InputStreamReader((InputStream)request,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				log.warn(e);;
				return null;
			}
		}
		if (request instanceof byte[]) {
			try {
	    		log.debug("returning byte[] as Reader");
				return new InputStreamReader(new ByteArrayInputStream((byte[])request),StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				log.warn(e);
				return null;
			}
		}
		log.debug("returning String as Reader");
		return new StringReader(request.toString());
	}

	public InputStream asInputStream() {
		if (request==null) {
			return null;
		}
		if (request instanceof InputStream) {
			log.debug("returning InputStream as InputStream");
			return(InputStream)request;
		} 
		if (request instanceof Reader) {
			log.debug("returning Reader as InputStream");
			return new ReaderInputStream((Reader)request,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as InputStream");
			return new ByteArrayInputStream((byte[])request);
		}
		try {
			log.debug("returning String as InputStream");
			return new ByteArrayInputStream(request.toString().getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
		} catch (UnsupportedEncodingException e) {
			log.warn(e);;
			return null;
		}
	}
	
	public InputSource asInputSource() {
		if (request==null) {
			return null;
		}
		if (request instanceof InputSource) {
			log.debug("returning InputSource as InputSource");
			return(InputSource)request;
		} 
		if (request instanceof InputStream) {
			log.debug("returning InputStream as InputSource");
			return(new InputSource((InputStream)request));
		} 
		if (request instanceof Reader) {
			log.debug("returning Reader as InputSource");
			return(new InputSource((Reader)request));
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as InputSource");
			return(new InputSource(new ByteArrayInputStream((byte[])request)));
		}
		log.debug("returning String as InputSource");
		return(new InputSource(new StringReader(request.toString())));
	}

	public Source asSource() {
		if (request==null) {
			return null;
		}
		if (request instanceof Source) {
			log.debug("returning Source as Source");
			return(Source)request;
		} 
		if (request instanceof InputStream) {
			log.debug("returning InputStream as InputSource");
			return(new StreamSource((InputStream)request));
		} 
		if (request instanceof Reader) {
			log.debug("returning Reader as InputSource");
			return(new StreamSource((Reader)request));
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as InputSource");
			return(new StreamSource(new ByteArrayInputStream((byte[])request)));
		}
		log.debug("returning String as InputSource");
		return(new StreamSource(new StringReader(request.toString())));
	}

	public String asString() throws IOException {
		if (request==null) {
			return null;
		}
		if (request instanceof String) {
			return (String)request;
		}
		return StreamUtil.readerToString(asReader(),null);
	}
	
}
