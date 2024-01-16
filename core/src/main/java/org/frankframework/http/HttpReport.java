/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.http;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.frankframework.util.XmlUtils;


public class HttpReport extends HttpEntityEnclosingRequestBase {

	public static final String METHOD_NAME = "REPORT";

	/**
	 * @param uri to connect to
	 * @param element entity
	 * @throws TransformerException
	 * @throws IllegalArgumentException if the uri is invalid.
	 */
	public HttpReport(final URI uri, Element element) throws TransformerException {
		super();
		setURI(uri);
		setHeader("Depth", "0");
		Document doc = element.getOwnerDocument();
		Source xmlSource = new DOMSource(doc);
		Transformer t = XmlUtils.getTransformerFactory(2).newTransformer();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Result outputTarget = new StreamResult(outputStream);
		t.transform(xmlSource, outputTarget);

		setEntity(new ByteArrayEntity(outputStream.toByteArray()));
	}

	/**
	 * @param uri to connect to
	 * @param element entity
	 * @throws TransformerException
	 * @throws IllegalArgumentException if the uri is invalid.
	 */
	public HttpReport(final String uri, Element element) throws TransformerException {
		this(URI.create(uri), element);
	}

	@Override
	public String getMethod() {
		return METHOD_NAME;
	}
}
