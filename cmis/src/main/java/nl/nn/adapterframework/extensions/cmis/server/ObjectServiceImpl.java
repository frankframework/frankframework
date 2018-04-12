package nl.nn.adapterframework.extensions.cmis.server;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.cmis.CmisSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PolicyIdListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ObjectServiceImpl implements ObjectService {

	private ObjectService objectService;
	private Logger log = LogUtil.getLogger(this);

	public ObjectServiceImpl(ObjectService objectService) {
		this.objectService = objectService;
	}

	@Override
	public String createDocument(String repositoryId, Properties properties,
			String folderId, ContentStream contentStream,
			VersioningState versioningState, List<String> policies,
			Acl addAces, Acl removeAces, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.createDocument(repositoryId, properties, folderId, contentStream, versioningState, policies, addAces, removeAces, extension);
	}

	@Override
	public String createDocumentFromSource(String repositoryId,
			String sourceId, Properties properties, String folderId,
			VersioningState versioningState, List<String> policies,
			Acl addAces, Acl removeAces, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.createDocumentFromSource(repositoryId, sourceId, properties, folderId, versioningState, policies, addAces, removeAces, extension);
	}

	@Override
	public String createFolder(String repositoryId, Properties properties,
			String folderId, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.createFolder(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
	}

	@Override
	public String createRelationship(String repositoryId,
			Properties properties, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.createRelationship(repositoryId, properties, policies, addAces, removeAces, extension);
	}

	@Override
	public String createPolicy(String repositoryId, Properties properties,
			String folderId, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.createPolicy(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
	}

	@Override
	public String createItem(String repositoryId, Properties properties,
			String folderId, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.createItem(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
	}

	@Override
	public AllowableActions getAllowableActions(String repositoryId,
			String objectId, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.getAllowableActions(repositoryId, objectId, extension);
	}

	private Properties processProperties(Element cmisElement) throws SenderException {
		PropertiesImpl properties = new PropertiesImpl();

		Element propertiesElement = XmlUtils.getFirstChildTag(cmisElement, "properties");
		Iterator<Node> propertyIterator = XmlUtils.getChildTags(propertiesElement, "property").iterator();
		while (propertyIterator.hasNext()) {
			Element propertyElement = (Element) propertyIterator.next();
			String propertyValue = XmlUtils.getStringValue(propertyElement);
			String nameAttr = propertyElement.getAttribute("name");
			String typeAttr = propertyElement.getAttribute("type");
			boolean isNull = Boolean.parseBoolean(propertyElement.getAttribute("isNull"));
			if(isNull)
				propertyValue = null;

			if (StringUtils.isEmpty(typeAttr) || typeAttr.equalsIgnoreCase("string")) {
				PropertyStringDefinitionImpl propertyDefinition = new PropertyStringDefinitionImpl();
				propertyDefinition.setId(nameAttr);
				propertyDefinition.setDisplayName(nameAttr);
				propertyDefinition.setLocalName(nameAttr);
				propertyDefinition.setQueryName(nameAttr);
				propertyDefinition.setCardinality(Cardinality.SINGLE);

				if(nameAttr.startsWith("cmis:")) {
					propertyDefinition.setPropertyType(PropertyType.ID);
					properties.addProperty(new PropertyIdImpl(propertyDefinition, propertyValue));
				}
				else {
					propertyDefinition.setPropertyType(PropertyType.STRING);
					properties.addProperty(new PropertyStringImpl(propertyDefinition, propertyValue));
				}
			} else if (typeAttr.equalsIgnoreCase("integer")) {

				PropertyIntegerDefinitionImpl propertyDefinition = new PropertyIntegerDefinitionImpl();
				propertyDefinition.setId(nameAttr);
				propertyDefinition.setDisplayName(nameAttr);
				propertyDefinition.setLocalName(nameAttr);
				propertyDefinition.setQueryName(nameAttr);
				propertyDefinition.setCardinality(Cardinality.SINGLE);

				properties.addProperty(new PropertyIntegerImpl(propertyDefinition, new BigInteger(propertyValue)));
			} else if (typeAttr.equalsIgnoreCase("boolean")) {

				PropertyBooleanDefinitionImpl propertyDefinition = new PropertyBooleanDefinitionImpl();
				propertyDefinition.setId(nameAttr);
				propertyDefinition.setDisplayName(nameAttr);
				propertyDefinition.setLocalName(nameAttr);
				propertyDefinition.setQueryName(nameAttr);
				propertyDefinition.setCardinality(Cardinality.SINGLE);

				properties.addProperty(new PropertyBooleanImpl(propertyDefinition, Boolean.parseBoolean(propertyValue)));
			} else if (typeAttr.equalsIgnoreCase("datetime")) {

				PropertyDateTimeDefinitionImpl propertyDefinition = new PropertyDateTimeDefinitionImpl();
				propertyDefinition.setId(nameAttr);
				propertyDefinition.setDisplayName(nameAttr);
				propertyDefinition.setLocalName(nameAttr);
				propertyDefinition.setQueryName(nameAttr);
				propertyDefinition.setCardinality(Cardinality.SINGLE);

				String formatStringAttr = propertyElement.getAttribute("formatString");
				if (StringUtils.isEmpty(formatStringAttr)) {
					formatStringAttr = CmisSender.FORMATSTRING_BY_DEFAULT;
				}
				DateFormat df = new SimpleDateFormat(formatStringAttr);
				Date date;
				try {
					date = df.parse(propertyValue);
				} catch (ParseException e) {
					throw new SenderException("exception parsing date [" + propertyValue + "] using formatString [" + formatStringAttr + "]", e);
				}
				GregorianCalendar gregorian = new GregorianCalendar();
				gregorian.setTime(date);

				properties.addProperty(new PropertyDateTimeImpl(propertyDefinition, gregorian));
			} else {
				log.warn("unparsable type [" + typeAttr + "] for property ["+propertyValue+"]");
			}
			log.debug("set property name [" + nameAttr + "] value [" + propertyValue + "]");
		}

		return properties;
	}

	private XmlBuilder buildXml(String name, Object value) {
		XmlBuilder filterXml = new XmlBuilder(name);

		if(value != null)
			filterXml.setValue(value.toString());

		return filterXml;
	}

	@Override
	public ObjectData getObject(String repositoryId, String objectId,
			String filter, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includePolicyIds, Boolean includeAcl,
			ExtensionsData extensions) {

		boolean bypass = AppConstants.getInstance().getBoolean("cmis.proxy.bypass.getObject", false);
		if(!bypass) {
			ObjectData objectData = objectService.getObject(repositoryId, objectId, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl, extensions);

			return objectData;
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml("repositoryId", repositoryId));
			cmisXml.addSubElement(buildXml("objectId", objectId));
			cmisXml.addSubElement(buildXml("filter", filter));
			cmisXml.addSubElement(buildXml("includeAllowableActions", includeAllowableActions));
			cmisXml.addSubElement(buildXml("includePolicies", includePolicyIds));
			cmisXml.addSubElement(buildXml("includeAcl", includeAcl));

			ObjectDataImpl impl = new ObjectDataImpl();
			try {

				IPipeLineSession messageContext = new PipeLineSessionBase();
				String result = CmisServletDispatcher.getInstance().getCmisListener().processRequest(null, cmisXml.toXML(), messageContext);

				Element cmisElement;
				if (XmlUtils.isWellFormed(result, "cmis")) {
					cmisElement = XmlUtils.buildElement(result);
				} else {
					cmisElement = XmlUtils.buildElement("<cmis/>");
				}

				// Handle allowable actions
				Element allowableActionsElem = XmlUtils.getFirstChildTag(cmisElement, "allowableActions");
				if(allowableActionsElem != null) {
					AllowableActionsImpl allowableActions = new AllowableActionsImpl();
					Set<Action> actions = EnumSet.noneOf(Action.class);

					Iterator<Node> actionIterator = XmlUtils.getChildTags(allowableActionsElem, "action").iterator();
					while (actionIterator.hasNext()) {
						String property = XmlUtils.getStringValue((Element) actionIterator.next());
						actions.add(Action.fromValue(property));
					}

					allowableActions.setAllowableActions(actions);
					impl.setAllowableActions(allowableActions);
				}

				// Handle isExactAcl
				impl.setIsExactAcl(XmlUtils.getChildTagAsBoolean(cmisElement, "isExactAcl"));

				// Handle policyIds
				Element policyIdsElem = XmlUtils.getFirstChildTag(cmisElement, "policyIds");
				if(policyIdsElem != null) {
					PolicyIdListImpl policyIdList = new PolicyIdListImpl();
					List<String> policies = new ArrayList<String>();

					Iterator<Node> policyIterator = XmlUtils.getChildTags(allowableActionsElem, "policyId").iterator();
					while (policyIterator.hasNext()) {
						String policyId = XmlUtils.getStringValue((Element) policyIterator.next());
						policies.add(policyId);
					}

					policyIdList.setPolicyIds(policies);
					impl.setPolicyIds(policyIdList);
				}

				// Handle properties
				impl.setProperties(processProperties(cmisElement));
			}
			catch(Exception e) {
				log.error("error creating CMIS objectData: " + e.getMessage(), e.getCause());
			}

			impl.setRenditions(null);
			impl.setExtensions(null);
			impl.setChangeEventInfo(null);
			impl.setRelationships(null);

			return impl;
		}
	}

	@Override
	public Properties getProperties(String repositoryId, String objectId,
			String filter, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.getProperties(repositoryId, objectId, filter, extension);
	}

	@Override
	public List<RenditionData> getRenditions(String repositoryId,
			String objectId, String renditionFilter, BigInteger maxItems,
			BigInteger skipCount, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.getRenditions(repositoryId, objectId, renditionFilter, maxItems, skipCount, extension);
	}

	@Override
	public ObjectData getObjectByPath(String repositoryId, String path,
			String filter, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includePolicyIds, Boolean includeAcl,
			ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.getObjectByPath(repositoryId, path, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl, extension);
	}

	@Override
	public ContentStream getContentStream(String repositoryId, String objectId,
			String streamId, BigInteger offset, BigInteger length,
			ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.getContentStream(repositoryId, objectId, streamId, offset, length, extension);
	}

	@Override
	public void updateProperties(String repositoryId, Holder<String> objectId,
			Holder<String> changeToken, Properties properties,
			ExtensionsData extension) {
		// TODO Auto-generated method stub
		objectService.updateProperties(repositoryId, objectId, changeToken, properties, extension);
	}

	@Override
	public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(
			String repositoryId,
			List<BulkUpdateObjectIdAndChangeToken> objectIdsAndChangeTokens,
			Properties properties, List<String> addSecondaryTypeIds,
			List<String> removeSecondaryTypeIds, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.bulkUpdateProperties(repositoryId, objectIdsAndChangeTokens, properties, addSecondaryTypeIds, removeSecondaryTypeIds, extension);
	}

	@Override
	public void moveObject(String repositoryId, Holder<String> objectId,
			String targetFolderId, String sourceFolderId,
			ExtensionsData extension) {
		// TODO Auto-generated method stub
		objectService.moveObject(repositoryId, objectId, targetFolderId, sourceFolderId, extension);
	}

	@Override
	public void deleteObject(String repositoryId, String objectId,
			Boolean allVersions, ExtensionsData extension) {
		// TODO Auto-generated method stub
		objectService.deleteObject(repositoryId, objectId, allVersions, extension);
	}

	@Override
	public FailedToDeleteData deleteTree(String repositoryId, String folderId,
			Boolean allVersions, UnfileObject unfileObjects,
			Boolean continueOnFailure, ExtensionsData extension) {
		// TODO Auto-generated method stub
		return objectService.deleteTree(repositoryId, folderId, allVersions, unfileObjects, continueOnFailure, extension);
	}

	@Override
	public void setContentStream(String repositoryId, Holder<String> objectId,
			Boolean overwriteFlag, Holder<String> changeToken,
			ContentStream contentStream, ExtensionsData extension) {
		// TODO Auto-generated method stub
		objectService.setContentStream(repositoryId, objectId, overwriteFlag, changeToken, contentStream, extension);
	}

	@Override
	public void deleteContentStream(String repositoryId,
			Holder<String> objectId, Holder<String> changeToken,
			ExtensionsData extension) {
		// TODO Auto-generated method stub
		objectService.deleteContentStream(repositoryId, objectId, changeToken, extension);
	}

	@Override
	public void appendContentStream(String repositoryId,
			Holder<String> objectId, Holder<String> changeToken,
			ContentStream contentStream, boolean isLastChunk,
			ExtensionsData extension) {
		// TODO Auto-generated method stub
		objectService.appendContentStream(repositoryId, objectId, changeToken, contentStream, isLastChunk, extension);
	}
}
