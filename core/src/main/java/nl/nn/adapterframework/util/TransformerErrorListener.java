/*
   Copyright 2017 Nationale-Nederlanden

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

import org.apache.log4j.Logger;

/**
 * Generic Transformer ErrorListener.
 * 
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 */
public class TransformerErrorListener implements ErrorListener {
	static Logger log = LogUtil.getLogger(XmlUtils.class);

	private boolean throwException;
	private TransformerException fatalTransformerException;
	private IOException fatalIOException;

	TransformerErrorListener() {
		this(true);
	}

	TransformerErrorListener(boolean throwException) {
		this.throwException = throwException;
	}

	@Override
	public void error(TransformerException transformerException) throws TransformerException {
		if (throwException) {
			throw transformerException;
		}
		log.warn("Nonfatal transformation error: " + transformerException.getMessage());
	}

	@Override
	public void fatalError(TransformerException transformerException) throws TransformerException {
		if (throwException) {
			throw transformerException;
		}
		this.setFatalTransformerException(transformerException);
	}

	@Override
	public void warning(TransformerException transformerException) throws TransformerException {
		log.warn("Nonfatal transformation warning: "+ transformerException.getMessage());
	}

	public void setFatalTransformerException(
			TransformerException fatalTransformerException) {
		this.fatalTransformerException = fatalTransformerException;
	}

	public TransformerException getFatalTransformerException() {
		return fatalTransformerException;
	}

	public void setFatalIOException(IOException fatalIOException) {
		this.fatalIOException = fatalIOException;
	}

	public IOException getFatalIOException() {
		return fatalIOException;
	}
}
