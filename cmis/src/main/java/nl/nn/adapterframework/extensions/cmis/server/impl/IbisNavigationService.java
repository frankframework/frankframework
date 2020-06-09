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
import java.util.List;

import nl.nn.adapterframework.extensions.cmis.CmisUtils;
import nl.nn.adapterframework.extensions.cmis.server.CmisEvent;
import nl.nn.adapterframework.extensions.cmis.server.CmisEventDispatcher;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.w3c.dom.Element;

public class IbisNavigationService implements NavigationService {

	private NavigationService navigationService;
	private CmisEventDispatcher eventDispatcher = CmisEventDispatcher.getInstance();
	private CallContext callContext;

	public IbisNavigationService(NavigationService navigationService, CallContext callContext) {
		this.navigationService = navigationService;
		this.callContext = callContext;
	}

	private XmlBuilder buildXml(String name, Object value) {
		XmlBuilder filterXml = new XmlBuilder(name);

		if(value != null)
			filterXml.setValue(value.toString());

		return filterXml;
	}

	@Override
	public ObjectInFolderList getChildren(String repositoryId, String folderId,
			String filter, String orderBy, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includePathSegment, BigInteger maxItems,
			BigInteger skipCount, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_CHILDREN)) {
			return navigationService.getChildren(repositoryId, folderId, filter, orderBy, includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, maxItems, skipCount, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml("repositoryId", repositoryId));
			cmisXml.addSubElement(buildXml("folderId", folderId));
			cmisXml.addSubElement(buildXml("filter", filter));
			cmisXml.addSubElement(buildXml("orderBy", orderBy));
			cmisXml.addSubElement(buildXml("includeAllowableActions", includeAllowableActions));
			cmisXml.addSubElement(buildXml("includeRelationships", includeRelationships.name()));
			cmisXml.addSubElement(buildXml("renditionFilter", renditionFilter));
			cmisXml.addSubElement(buildXml("includePathSegment", includePathSegment));
			cmisXml.addSubElement(buildXml("maxItems", maxItems));
			cmisXml.addSubElement(buildXml("skipCount", skipCount));

			Element cmisResult = eventDispatcher.trigger(CmisEvent.GET_CHILDREN, cmisXml.toXML(), callContext);
			Element typesXml = XmlUtils.getFirstChildTag(cmisResult, "objectInFolderList");

			return CmisUtils.xml2ObjectsInFolderList(typesXml);
		}
	}

	@Override
	public List<ObjectInFolderContainer> getDescendants(String repositoryId,
			String folderId, BigInteger depth, String filter,
			Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includePathSegment, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return navigationService.getDescendants(repositoryId, folderId, depth, filter, includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, extension);
	}

	@Override
	public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
			String folderId, BigInteger depth, String filter,
			Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includePathSegment, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return navigationService.getFolderTree(repositoryId, folderId, depth, filter, includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, extension);
	}

	@Override
	public List<ObjectParentData> getObjectParents(String repositoryId,
			String objectId, String filter, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includeRelativePathSegment, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return navigationService.getObjectParents(repositoryId, objectId, filter, includeAllowableActions, includeRelationships, renditionFilter, includeRelativePathSegment, extension);
	}

	@Override
	public ObjectData getFolderParent(String repositoryId, String folderId,
			String filter, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectList getCheckedOutDocs(String repositoryId, String folderId,
			String filter, String orderBy, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return navigationService.getCheckedOutDocs(repositoryId, folderId, filter, orderBy, includeAllowableActions, includeRelationships, renditionFilter, maxItems, skipCount, extension);
	}
}
