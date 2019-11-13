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

import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;

public class ThreadConnectingFilter extends FullXmlFilter {

	private ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;
	private Object threadInfo;
	
	public ThreadConnectingFilter(Object owner, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, String correlationID) {
		super();
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
		threadInfo=threadLifeCycleEventListener!=null?threadLifeCycleEventListener.announceChildThread(owner, correlationID):null;
	}

	@Override
	public void startDocument() throws SAXException {
		if (threadLifeCycleEventListener!=null) {
			threadLifeCycleEventListener.threadCreated(threadInfo);
		}
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		if (threadLifeCycleEventListener!=null) {
			threadLifeCycleEventListener.threadEnded(threadInfo,null);
		}
	}
	
}
