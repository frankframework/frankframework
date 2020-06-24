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
package nl.nn.adapterframework.extensions.cmis.server.impl;

import java.math.BigInteger;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.w3c.dom.Element;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.extensions.cmis.CmisUtils;
import nl.nn.adapterframework.extensions.cmis.server.CmisEvent;
import nl.nn.adapterframework.extensions.cmis.server.CmisEventDispatcher;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

public class IbisDiscoveryService implements DiscoveryService {

	private DiscoveryService discoveryService;
	private CmisEventDispatcher eventDispatcher = CmisEventDispatcher.getInstance();
	private CallContext callContext;

	public IbisDiscoveryService(DiscoveryService discoveryService, CallContext callContext) {
		this.discoveryService = discoveryService;
		this.callContext = callContext;
	}

	private XmlBuilder buildXml(String name, Object value) {
		XmlBuilder filterXml = new XmlBuilder(name);

		if(value != null)
			filterXml.setValue(value.toString());

		return filterXml;
	}

	@Override
	public ObjectList query(String repositoryId, String statement,
			Boolean searchAllVersions, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.QUERY)) {
			return discoveryService.query(repositoryId, statement, searchAllVersions, includeAllowableActions, includeRelationships, renditionFilter, maxItems, skipCount, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml("repositoryId", repositoryId));
			cmisXml.addSubElement(buildXml("statement", statement));
			cmisXml.addSubElement(buildXml("searchAllVersions", searchAllVersions));
			cmisXml.addSubElement(buildXml("includeAllowableActions", includeAllowableActions));
			cmisXml.addSubElement(buildXml("includeRelationships", includeRelationships.name()));
			cmisXml.addSubElement(buildXml("renditionFilter", renditionFilter));
			cmisXml.addSubElement(buildXml("maxItems", maxItems));
			cmisXml.addSubElement(buildXml("skipCount", skipCount));

			IPipeLineSession context = new PipeLineSessionBase();
			context.put(CmisUtils.CMIS_CALLCONTEXT_KEY, callContext);
			Element cmisResult = eventDispatcher.trigger(CmisEvent.QUERY, cmisXml.toXML(), context);
			Element typesXml = XmlUtils.getFirstChildTag(cmisResult, "objectList");

			return CmisUtils.xml2ObjectList(typesXml, context);
		}
	}

	@Override
	public ObjectList getContentChanges(String repositoryId,
			Holder<String> changeLogToken, Boolean includeProperties,
			String filter, Boolean includePolicyIds, Boolean includeAcl,
			BigInteger maxItems, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return discoveryService.getContentChanges(repositoryId, changeLogToken, includeProperties, filter, includePolicyIds, includeAcl, maxItems, extension);
	}
	
}
