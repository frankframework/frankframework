/*
   Copyright 2019-2021 WeAreFrank!

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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import lombok.Getter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;

public class TransformerFilter extends FullXmlFilter {

	private TransformerHandler transformerHandler;
	private @Getter ErrorListener errorListener;
	
	public TransformerFilter(INamedObject owner, TransformerHandler transformerHandler, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, IPipeLineSession session, boolean expectChildThreads, ContentHandler handler) {
		super();
		if (expectChildThreads) {
			handler = new ThreadConnectingFilter(owner, threadLifeCycleEventListener, session, handler);
		}
		SAXResult transformedStream = new SAXResult();
		transformedStream.setHandler(handler);
		if (handler instanceof LexicalHandler) {
			transformedStream.setLexicalHandler((LexicalHandler)handler);
		}
		this.transformerHandler=transformerHandler;
		transformerHandler.setResult(transformedStream);
		errorListener = transformerHandler.getTransformer().getErrorListener();
		ContentHandler inputHandler = transformerHandler;
		if (expectChildThreads) {
			/*
			 * If XSLT processing is done in another thread than the SAX events are provided, which is the 
			 * case if streaming XSLT is used, then exceptions in the processing part do not travel up
			 * through the transformerHandler automatically.
			 * Here we set up a ExceptionInsertingFilter and ErrorListener that provide this.
			 */
			ExceptionInsertingFilter exceptionInsertingFilter = new ExceptionInsertingFilter(inputHandler);
			inputHandler = exceptionInsertingFilter;
			transformerHandler.getTransformer().setErrorListener(new ErrorListener() {
				
				@Override
				public void error(TransformerException paramTransformerException) throws TransformerException {
					try {
						if (errorListener!=null) {
							errorListener.error(paramTransformerException);
						}
					} catch (TransformerException e) {
						exceptionInsertingFilter.insertException(new SaxException(e));
					}
				}

				@Override
				public void fatalError(TransformerException paramTransformerException) throws TransformerException {
					try {
						if (errorListener!=null) {
							errorListener.fatalError(paramTransformerException);
						}
					} catch (TransformerException e) {
						exceptionInsertingFilter.insertException(new SaxException(e));
					}
					
				}

				@Override
				public void warning(TransformerException paramTransformerException) throws TransformerException {
					try {
						if (errorListener!=null) {
							errorListener.warning(paramTransformerException);
						}
					} catch (TransformerException e) {
						exceptionInsertingFilter.insertException(new SaxException(e));
					}
					
				}
			});
		}
		setContentHandler(inputHandler);
	}


	public Transformer getTransformer() {
		return transformerHandler.getTransformer();
	}
}