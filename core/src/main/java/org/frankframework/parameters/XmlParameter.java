/*
   Copyright 2024 WeAreFrank!

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

package org.frankframework.parameters;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.w3c.dom.Document;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.stream.Message;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.XmlException;
import org.frankframework.util.XmlUtils;

@Log4j2
public class XmlParameter extends AbstractParameter {
	private XmlType xmlType;

	public XmlParameter() {
		setXmlType(XmlType.DOMDOC);
	}

	public enum XmlType {
		NODE, DOMDOC
	}

	@Override
	protected Object getValueAsType(@Nonnull Message request, boolean namespaceAware) throws ParameterException, IOException {
		boolean isTypeNode = XmlType.NODE.equals(xmlType);

		if (isTypeNode) {
			log.warn("Parameter [{}] uses parameterType NODE, falling back to DOMDOC instead", this::getName);
		}

		try {
			if (request.asObject() instanceof Document document) {
				return isTypeNode ? document.getDocumentElement() : document;
			}

			Message requestToUse = isRemoveNamespaces() ? XmlUtils.removeNamespaces(request) : request;
			Document document = XmlUtils.buildDomDocument(requestToUse.asInputSource(), namespaceAware);

			return isTypeNode ? document.getDocumentElement() : document;
		} catch (DomBuilderException | IOException | XmlException e) {
			throw new ParameterException(getName(), "Parameter [" + getName() + "] could not parse result [" + request + "] to XML document", e);
		}
	}

	@Override
	@Deprecated
	@ConfigurationWarning("use element XmlParameter with attribute xmlType instead")
	public void setType(ParameterType type) {
		this.xmlType = EnumUtils.parse(XmlType.class, type.name());
		super.setType(type);
	}

	public void setXmlType(XmlType xmlType) {
		this.xmlType = xmlType;
		super.setType(EnumUtils.parse(ParameterType.class, xmlType.name()));
	}
}
