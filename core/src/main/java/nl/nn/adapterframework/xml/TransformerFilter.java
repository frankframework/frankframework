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
import nl.nn.adapterframework.stream.ThreadCreationEventListener;
import nl.nn.adapterframework.util.TransformerErrorListener;

public class TransformerFilter extends FullXmlFilter {

	private FullXmlFilter lastFilter;
	private TransformerHandler transformerHandler;
	
	public TransformerFilter(INamedObject owner, TransformerHandler transformerHandler, ThreadCreationEventListener threadCreationEventListener, String correlationID) {
		super();
		ThreadConnectingFilter threadConnectingFilter = new ThreadConnectingFilter(owner, threadCreationEventListener, correlationID);
		lastFilter=threadConnectingFilter;
		SAXResult transformedStream = new SAXResult();
		transformedStream.setHandler(threadConnectingFilter);
		transformedStream.setLexicalHandler((LexicalHandler)threadConnectingFilter);
		this.transformerHandler=transformerHandler;
		transformerHandler.setResult(transformedStream);
		super.setContentHandler(transformerHandler);
	}


	@Override
	public void setContentHandler(ContentHandler handler) {
		lastFilter.setContentHandler(handler);
	}
	
	public TransformerErrorListener getErrorListener() {
		return (TransformerErrorListener)transformerHandler.getTransformer().getErrorListener();
	}
	
	public Transformer getTransformer() {
		return transformerHandler.getTransformer();
	}
}