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
package nl.nn.adapterframework.xml;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.ThreadConnector;

public class ThreadConnectingFilter extends ExceptionCatchingFilter {

	private ThreadConnector threadConnector;

	public ThreadConnectingFilter(ThreadConnector threadConnector, ContentHandler handler) {
		super(handler);
		this.threadConnector=threadConnector;
	}

	@Override
	protected void handleException(Exception e) throws SAXException {
		Throwable t = threadConnector.abortThread(e);
		if (t instanceof SAXException) {
			throw (SAXException) t;
		}
		if (t instanceof Exception) {
			throw new SaxException((Exception)t);
		}
		throw new RuntimeException(t);
	}

	@Override
	public void startDocument() throws SAXException {
		try {
			threadConnector.startThread(null);
		} catch(Exception e) {
			handleException(e); //Cleanup dangling threads, creates better Ladybug reports
		}
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		threadConnector.endThread(null);
	}
}
