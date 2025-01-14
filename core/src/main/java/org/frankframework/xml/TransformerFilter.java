/*
   Copyright 2019-2022 WeAreFrank!

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
package org.frankframework.xml;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import lombok.Getter;

import org.frankframework.threading.ThreadConnector;

public class TransformerFilter extends FullXmlFilter {

	private final TransformerHandler transformerHandler;
	private final @Getter ErrorListener errorListener;
	private ThreadConnectingFilter threadConnectingFilter;

	public TransformerFilter(ThreadConnector threadConnector, TransformerHandler transformerHandler, ContentHandler handler, boolean removeNamespacesFromInput, boolean handleLexicalEvents) {
		super();
		if (threadConnector != null) {
			handler = threadConnectingFilter = new ThreadConnectingFilter(threadConnector, handler);
		}
		SAXResult transformedStream = new SAXResult();
		transformedStream.setHandler(handler);
		if (handler instanceof LexicalHandler lexicalHandler) {
			transformedStream.setLexicalHandler(lexicalHandler);
		}
		this.transformerHandler=transformerHandler;
		transformerHandler.setResult(transformedStream);
		errorListener = transformerHandler.getTransformer().getErrorListener();
		ContentHandler inputHandler = transformerHandler;
		if (threadConnectingFilter != null) {
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
						// this throw is necessary, although it causes log messages like 'Exception in thread "main/Thread-0"'
						// If absent, Xslt tests fail.
						throw e;
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
						// this throw is necessary, although it causes log messages like 'Exception in thread "main/Thread-0"'
						// If absent, Xslt tests fail.
						throw e;
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
						// this throw is necessary, although it causes log messages like 'Exception in thread "main/Thread-0"'
						// If absent, Xslt tests fail.
						throw e;
					}

				}
			});
		}
		if (removeNamespacesFromInput) {
			inputHandler = new NamespaceRemovingFilter(inputHandler);
		}
		setContentHandler(inputHandler);
		if (!handleLexicalEvents) {
			setLexicalHandler(null);
		}
	}


	public Transformer getTransformer() {
		return transformerHandler.getTransformer();
	}
}
