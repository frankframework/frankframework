/*
   Copyright 2019-2020 Nationale-Nederlanden

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
package org.frankframework.extensions.cmis.server.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.frankframework.extensions.cmis.CmisUtils;
import org.frankframework.extensions.cmis.server.CmisEvent;
import org.frankframework.extensions.cmis.server.CmisEventDispatcher;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Wrapper that delegates when a matching CmisEvent is present.
 *
 * @author Niels
 */
public class IbisRepositoryService implements RepositoryService {

	private final RepositoryService repositoryService;
	private final CmisEventDispatcher eventDispatcher = CmisEventDispatcher.getInstance();
	private final CallContext callContext;

	public IbisRepositoryService(RepositoryService repositoryService, CallContext callContext) {
		this.repositoryService = repositoryService;
		this.callContext = callContext;
	}

	private XmlBuilder buildXml(String name, Object value) {
		XmlBuilder filterXml = new XmlBuilder(name);

		if(value != null)
			filterXml.setValue(value.toString());

		return filterXml;
	}

	@Override
	public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_REPOSITORIES)) {
			return repositoryService.getRepositoryInfos(extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");

			Element cmisResult = eventDispatcher.trigger(CmisEvent.GET_REPOSITORIES, cmisXml.asXmlString(), callContext);
			Element repositories = XmlUtils.getFirstChildTag(cmisResult, "repositories");

			List<RepositoryInfo> repositoryInfoList = new ArrayList<>();
			for(Node node : XmlUtils.getChildTags(repositories, "repository")) {
				repositoryInfoList.add(CmisUtils.xml2repositoryInfo((Element) node));
			}
			return repositoryInfoList;
		}
	}

	@Override
	public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_REPOSITORY_INFO)) {
			return repositoryService.getRepositoryInfo(repositoryId, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml("repositoryId", repositoryId));

			Element cmisResult = eventDispatcher.trigger(CmisEvent.GET_REPOSITORY_INFO, cmisXml.asXmlString(), callContext);

			Element repositories = XmlUtils.getFirstChildTag(cmisResult, "repositories");
			Element repository = XmlUtils.getFirstChildTag(repositories, "repository");

			return CmisUtils.xml2repositoryInfo(repository);
		}
	}

	@Override
	public TypeDefinitionList getTypeChildren(String repositoryId, String typeId, Boolean includePropertyDefinitions,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return repositoryService.getTypeChildren(repositoryId, typeId, includePropertyDefinitions, maxItems, skipCount, extension);
	}

	@Override
	public List<TypeDefinitionContainer> getTypeDescendants(String repositoryId, String typeId, BigInteger depth,
			Boolean includePropertyDefinitions, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_TYPE_DESCENDANTS)) {
			return repositoryService.getTypeDescendants(repositoryId, typeId, depth, includePropertyDefinitions, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml("repositoryId", repositoryId));
			cmisXml.addSubElement(buildXml("typeId", typeId));
			cmisXml.addSubElement(buildXml("depth", depth));
			cmisXml.addSubElement(buildXml("includePropertyDefinitions", includePropertyDefinitions));

			Element cmisResult = eventDispatcher.trigger(CmisEvent.GET_TYPE_DESCENDANTS, cmisXml.asXmlString(), callContext);
			Element typesXml = XmlUtils.getFirstChildTag(cmisResult, "typeDescendants");

			return CmisUtils.xml2TypeDescendants(typesXml, callContext.getCmisVersion());
		}
	}

	@Override
	public TypeDefinition getTypeDefinition(String repositoryId, String typeId, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_TYPE_DEFINITION)) {
			return repositoryService.getTypeDefinition(repositoryId, typeId, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml("repositoryId", repositoryId));
			cmisXml.addSubElement(buildXml("typeId", typeId));

			Element cmisResult = eventDispatcher.trigger(CmisEvent.GET_TYPE_DEFINITION, cmisXml.asXmlString(), callContext);

			Element typesXml = XmlUtils.getFirstChildTag(cmisResult, "typeDefinitions");

			return CmisUtils.xml2TypeDefinition(XmlUtils.getFirstChildTag(typesXml, "typeDefinition"), callContext.getCmisVersion());
		}
	}

	@Override
	public TypeDefinition createType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return repositoryService.createType(repositoryId, type, extension);
	}

	@Override
	public TypeDefinition updateType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return repositoryService.updateType(repositoryId, type, extension);
	}

	@Override
	public void deleteType(String repositoryId, String typeId, ExtensionsData extension) {
		repositoryService.deleteType(repositoryId, typeId, extension);
	}
}
