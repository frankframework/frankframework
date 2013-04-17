/*
   Copyright 2013 Nationale-Nederlanden

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

import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import java.io.Reader;
import java.io.StringReader;

/**
 * Class to handle all kinds of conversions.
 * @version $Id$
 *
 * @author Gerrit van Brakel IOS
 */
public class Variant {
	public static final String version = "$RCSfile: Variant.java,v $ $Revision: 1.8 $ $Date: 2011-11-30 13:51:48 $";

	private String data = null;
	private Source dataAsXmlSource = null;
	
	public Variant(Object obj) {
		this(obj.toString());
	}
	public Variant(String obj) {
		super();
		data = obj;
	}
	public Reader asReader() {
		return new StringReader(data);
	}
	public String asString() {
		return data;
	}
	
	/**
	 * Renders an InputSource for SAX parsing
	 */
	public InputSource asXmlInputSource() {

		StringReader sr;

		sr = new StringReader(data);

		return new InputSource(sr);
	}

	/**
	 * Renders a Source for XSLT-transformation
	 */
	public Source asXmlSource() throws DomBuilderException {
		return asXmlSource(true);
	}

	public Source asXmlSource(boolean forMultipleUse) throws DomBuilderException {
		if (!forMultipleUse && dataAsXmlSource==null) {
			return XmlUtils.stringToSourceForSingleUse(data);
		}
		if (dataAsXmlSource==null) {
			dataAsXmlSource = XmlUtils.stringToSource(data);
		}
		return dataAsXmlSource;
	}
}
