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
package nl.nn.adapterframework.xml;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.ThreadCreationEventListener;

public class ThreadConnectingFilter extends FullXmlFilter {

	private ThreadCreationEventListener threadCreationEventListener;
	private Object threadInfo;
	
	public ThreadConnectingFilter(ThreadCreationEventListener threadCreationEventListener, String correlationID) {
		super();
		this.threadCreationEventListener=threadCreationEventListener;
		threadInfo=threadCreationEventListener!=null?threadCreationEventListener.announceChildThread(this, correlationID):null;
	}

	@Override
	public void startDocument() throws SAXException {
		if (threadCreationEventListener!=null) {
			threadCreationEventListener.threadCreated(threadInfo);
		}
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		if (threadCreationEventListener!=null) {
			threadCreationEventListener.threadEnded(threadInfo,null);
		}
	}
	
}
