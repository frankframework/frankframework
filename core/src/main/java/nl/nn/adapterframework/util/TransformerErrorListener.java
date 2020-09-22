/*
   Copyright 2017,2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.io.IOException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;

/**
 * Generic Transformer ErrorListener.
 * 
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 */
public class TransformerErrorListener implements ErrorListener {
	static Logger log = LogUtil.getLogger(TransformerErrorListener.class);

	private boolean throwException;
	private TransformerException fatalTransformerException;
	private IOException fatalIOException;

	public TransformerErrorListener() {
		this(true);
	}

	public TransformerErrorListener(boolean throwException) {
		this.throwException = throwException;
	}

	@Override
	public void error(TransformerException transformerException) throws TransformerException {
		if (throwException) {
			throw transformerException;
		}
		log.warn("Nonfatal transformation error: " + transformerException.getMessageAndLocation());
	}

	@Override
	public void fatalError(TransformerException transformerException) throws TransformerException {
		log.warn("Fatal transformation error: " + transformerException.getMessageAndLocation());
		this.setFatalTransformerException(transformerException);
		if (throwException) {
			throw transformerException;
		}
	}

	@Override
	public void warning(TransformerException transformerException) throws TransformerException {
		log.warn("Nonfatal transformation warning: " + transformerException.getMessageAndLocation());
	}

	public void setFatalTransformerException(TransformerException fatalTransformerException) {
		this.fatalTransformerException = fatalTransformerException;
	}
	public TransformerException getFatalTransformerException() {
		return fatalTransformerException;
	}

	public void setFatalIOException(IOException fatalIOException) {
		if (this.fatalIOException!=null && this.fatalIOException!=fatalIOException) {
			log.warn("replacing fatalIOException",this.fatalIOException);
		}
		this.fatalIOException = fatalIOException;
	}
	public IOException getFatalIOException() {
		return fatalIOException;
	}

//	@Override
//	public void error(SAXParseException e) throws SAXException {
//		log.error("SAX error",e);
//	}
//
//	@Override
//	public void fatalError(SAXParseException e) throws SAXException {
//		log.error("SAX fatalError",e);
//	}
//
//	@Override
//	public void warning(SAXParseException e) throws SAXException {
//		log.warn("SAX warning",e);
//	}
}
