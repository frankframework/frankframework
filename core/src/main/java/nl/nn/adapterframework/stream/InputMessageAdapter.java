/*
   Copyright 2019 Nationale-Nederlanden

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

public class InputMessageAdapter {
	protected Logger log = LogUtil.getLogger(this);

	private Object request;
	
	public InputMessageAdapter(Object request) {
		this.request=request;
	}
	
	public Reader asReader() {
		if (request==null) {
			return null;
		}
		if (request instanceof Reader) {
			return(Reader)request;
		} 
		if (request instanceof InputStream) {
			try {
				return new InputStreamReader((InputStream)request,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				log.warn(e);;
				return null;
			}
		}
		if (request instanceof byte[]) {
			try {
				return new InputStreamReader(new ByteArrayInputStream((byte[])request),StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			} catch (UnsupportedEncodingException e) {
				log.warn(e);
				return null;
			}
		}
		return new StringReader(request.toString());
	}

	public InputStream asInputStream() {
		if (request==null) {
			return null;
		}
		if (request instanceof InputStream) {
			return(InputStream)request;
		} 
		if (request instanceof Reader) {
			return new ReaderInputStream((Reader)request,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
		if (request instanceof byte[]) {
			return new ByteArrayInputStream((byte[])request);
		}
		try {
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
			return(InputSource)request;
		} 
		if (request instanceof InputStream) {
			return(new InputSource((InputStream)request));
		} 
		if (request instanceof Reader) {
			return(new InputSource((Reader)request));
		}
		if (request instanceof byte[]) {
			return(new InputSource(new ByteArrayInputStream((byte[])request)));
		}
		return(new InputSource(new StringReader(request.toString())));
	}
}
