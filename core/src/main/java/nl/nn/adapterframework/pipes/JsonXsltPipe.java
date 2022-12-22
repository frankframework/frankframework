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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.senders.JsonXsltSender;
import nl.nn.adapterframework.senders.XsltSender;

/**
 * Perform an XSLT transformation with a specified stylesheet on a JSON input, yielding JSON, XML or text.
 * JSON input is transformed into XML map, array, string, integer and boolean elements, in the namespace http://www.w3.org/2013/XSL/json.
 * The XSLT stylesheet or XPathExpression operates on these element.
 * 
 * @see  <a href="https://www.xml.com/articles/2017/02/14/why-you-should-be-using-xslt-30/">https://www.xml.com/articles/2017/02/14/why-you-should-be-using-xslt-30/</a>
 *
 * @author Gerrit van Brakel
 */

public class JsonXsltPipe extends XsltPipe {

	private final String JSONXSLTSENDER = "nl.nn.adapterframework.senders.JsonXsltSender";

	@Override
	protected XsltSender createXsltSender() {
		return new JsonXsltSender();
	}

	/** @ff.ref nl.nn.adapterframework.senders.JsonXsltSender */
	public void setJsonResult(boolean jsonResult) {
		((JsonXsltSender)getSender()).setJsonResult(jsonResult);
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.senders.JsonXsltSender */
	public void setNamespaceDefs(String namespaceDefs) {
		super.setNamespaceDefs(namespaceDefs);
	}

}
