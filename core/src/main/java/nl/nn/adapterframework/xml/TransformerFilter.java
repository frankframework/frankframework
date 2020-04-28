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

import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;

public class TransformerFilter extends FullXmlFilter {

	private FullXmlFilter lastFilter;
	private TransformerHandler transformerHandler;
	
	public TransformerFilter(INamedObject owner, TransformerHandler transformerHandler, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, IPipeLineSession session, boolean expectChildThreads) {
		super();
		if (expectChildThreads) {
			lastFilter = new ThreadConnectingFilter(owner, threadLifeCycleEventListener, session);
		} else {
			lastFilter = new FullXmlFilter();
		}
		SAXResult transformedStream = new SAXResult();
		transformedStream.setHandler(lastFilter);
		transformedStream.setLexicalHandler((LexicalHandler)lastFilter);
		this.transformerHandler=transformerHandler;
		transformerHandler.setResult(transformedStream);
		super.setContentHandler(transformerHandler);
	}


	@Override
	public void setContentHandler(ContentHandler handler) {
		lastFilter.setContentHandler(handler);
	}
	
	public Transformer getTransformer() {
		return transformerHandler.getTransformer();
	}
}