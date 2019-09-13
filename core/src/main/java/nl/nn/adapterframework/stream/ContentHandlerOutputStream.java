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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class ContentHandlerOutputStream extends PipedOutputStream {
	protected Logger log = LogUtil.getLogger(this);

	private ContentHandler handler;
	
	private PipedInputStream pipedInputStream=new PipedInputStream();
	private Exception exception;
	
	public ContentHandlerOutputStream(ContentHandler handler) throws StreamingException {
		this.handler=handler;
		try {
			pipedInputStream=new PipedInputStream();
			connect(pipedInputStream);
			pipeReader.start();
		} catch (IOException e) {
			throw new StreamingException(e);
		}
	}

	final Thread pipeReader=new Thread() {

		@Override
		public void run() {
			try {
				boolean namespaceAware=true;
				boolean resolveExternalEntities=false;
				InputSource inputSource = new InputSource(pipedInputStream);
				XMLReader xmlReader = XmlUtils.getXMLReader(namespaceAware, resolveExternalEntities);
				xmlReader.setContentHandler(handler);
				xmlReader.parse(inputSource);
			} catch (Exception e) {
				setException(e);
			}
		}
	};
	
	@Override
	public void close() throws IOException {
		super.close();
		try {
			pipeReader.join();
			if (getException()!=null) {
				throw new IOException(getException());
			}
		} catch (InterruptedException e) {
			log.warn(e);
		}
	}
	
	public void setException(Exception exception) {
		this.exception = exception;
	}
	public Exception getException() {
		return exception;
	}
	
}
