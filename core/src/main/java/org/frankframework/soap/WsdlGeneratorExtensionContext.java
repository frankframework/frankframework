/*
   Copyright 2023-2025 WeAreFrank!

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
package org.frankframework.soap;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.frankframework.core.IJmsListener;
import org.frankframework.core.PipeLine;

public interface WsdlGeneratorExtensionContext {
	List<String> getWarnings();
	String getFilename();
	String getTNS();

	boolean hasSOAPActionName();
	String getSOAPActionName();

	void setExtensionNamespacePrefixes(XMLStreamWriter w) throws XMLStreamException;
	void addExtensionNamespaces(XMLStreamWriter w) throws XMLStreamException;
	void addJmsBindingInfo(XMLStreamWriter w, WsdlGenerator wsdlGenerator, PipeLine pipeLine) throws XMLStreamException;
	void addJmsServiceInfo(XMLStreamWriter w, IJmsListener listener) throws XMLStreamException;
}
