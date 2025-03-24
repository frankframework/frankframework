/*
   Copyright 2019-2020 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.extensions.cmis;

import static org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper.HTTP_INVOKER_OBJECT;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.CmisSpi;
import org.apache.chemistry.opencmis.commons.data.AclCapabilities;
import org.apache.chemistry.opencmis.commons.data.CreatablePropertyTypes;
import org.apache.chemistry.opencmis.commons.data.NewTypeSettableAttributes;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.PolicyIdList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyBoolean;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyDateTime;
import org.apache.chemistry.opencmis.commons.data.PropertyDecimal;
import org.apache.chemistry.opencmis.commons.data.PropertyHtml;
import org.apache.chemistry.opencmis.commons.data.PropertyId;
import org.apache.chemistry.opencmis.commons.data.PropertyInteger;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.data.PropertyUri;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeMutability;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityOrderBy;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.SupportedPermissions;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AclCapabilitiesDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.CreatablePropertyTypesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.NewTypeSettableAttributesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionDefinitionDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionMappingDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PolicyIdListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeMutabilityImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.cmis.server.CmisSecurityHandler;
import org.frankframework.http.HttpSecurityHandler;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

public class CmisUtils {

	public static final String FORMATSTRING_BY_DEFAULT = AppConstants.getInstance().getString("cmis.datetime.formatstring", "yyyy-MM-dd'T'HH:mm:ss");
	public static final String ORIGINAL_OBJECT_KEY = "originalObject";
	public static final String CMIS_VERSION_KEY = "cmisVersion";
	public static final String CMIS_BINDING_KEY = "cmisBinding";
	public static final String CMIS_CALLCONTEXT_KEY = "cmisCallContext";

	private static final Logger log = LogUtil.getLogger(CmisUtils.class);
	private static final String CMIS_SECURITYHANDLER = AppConstants.getInstance().getString("cmis.securityHandler.type", "wsse");

	public static void populateCmisAttributes(PipeLineSession session) {
		CallContext callContext = (CallContext) session.get(CMIS_CALLCONTEXT_KEY);
		if(callContext != null) {
			session.put(CMIS_VERSION_KEY, callContext.getCmisVersion());
			session.put(CMIS_BINDING_KEY, callContext.getBinding());

			if("basic".equalsIgnoreCase(CMIS_SECURITYHANDLER)) {
				HttpServletRequest request = (HttpServletRequest) callContext.get(CallContext.HTTP_SERVLET_REQUEST);
				session.setSecurityHandler(new HttpSecurityHandler(request));
			} else if("wsse".equalsIgnoreCase(CMIS_SECURITYHANDLER)) {
				session.setSecurityHandler(new CmisSecurityHandler(callContext));
			}
		}
	}

	public static void closeBindingSession(CmisSpi owner, BindingSession bindingSession) {
		log.debug("Closing {}", owner.getClass().getSimpleName());
		Object invoker = bindingSession.get(HTTP_INVOKER_OBJECT);
		if (invoker instanceof CmisHttpInvoker cmisHttpInvoker) {
			log.debug("Closing CMIS Invoker {}", cmisHttpInvoker);
			cmisHttpInvoker.close();
		} else {
			log.debug("BindingSession for {} does not have instance of CmisHttpInvoker: {}", owner.getClass().getSimpleName(), invoker);
		}

	}

	public static XmlBuilder buildXml(String name, Object value) {
		XmlBuilder filterXml = new XmlBuilder(name);

		if(value != null)
			filterXml.setValue(value.toString());

		return filterXml;
	}

	public static XmlBuilder getPropertyXml(PropertyData<?> property) {
		XmlBuilder propertyXml = new XmlBuilder("property");

		propertyXml.addAttribute("name", property.getId());
		propertyXml.addAttribute("displayName", property.getDisplayName());
		propertyXml.addAttribute("localName", property.getLocalName());
		propertyXml.addAttribute("queryName", property.getQueryName());

		PropertyType propertyType = PropertyType.STRING;
		if(property instanceof Property property1) {
			propertyType = property1.getType();
		}
		else {
			if(property instanceof PropertyId) {
				propertyType = PropertyType.ID;
			} else if(property instanceof PropertyBoolean) {
				propertyType = PropertyType.BOOLEAN;
			} else if(property instanceof PropertyUri) {
				propertyType = PropertyType.URI;
			} else if(property instanceof PropertyInteger) {
				propertyType = PropertyType.INTEGER;
			} else if(property instanceof PropertyHtml) {
				propertyType = PropertyType.HTML;
			} else if(property instanceof PropertyDecimal) {
				propertyType = PropertyType.DECIMAL;
			} else if(property instanceof PropertyString) {
				propertyType = PropertyType.STRING;
			} else if(property instanceof PropertyDateTime) {
				propertyType = PropertyType.DATETIME;
			}
		}
		//If it's not a property, what would it be? assume it's a string...

		propertyXml.addAttribute("type", propertyType.value());

		Object value = property.getFirstValue();
		if (value == null) {
			propertyXml.addAttribute("isNull", "true");
		}
		else {
			switch (propertyType) {
			case INTEGER:
				BigInteger bi = (BigInteger) value;
				propertyXml.setValue(String.valueOf(bi));
				break;
			case BOOLEAN:
				Boolean b = (Boolean) value;
				propertyXml.setValue(String.valueOf(b));
				break;
			case DATETIME:
				GregorianCalendar gregorianCalendar = (GregorianCalendar) value;
				String formattedDate = DateTimeFormatter.ofPattern(FORMATSTRING_BY_DEFAULT).format(Instant.ofEpochMilli(gregorianCalendar.getTimeInMillis()));

				propertyXml.setValue(formattedDate);
				break;

			default: // String/ID/HTML/URI
				propertyXml.setValue((String) value);
				break;
			}
		}

		return propertyXml;
	}

	private static <T> AbstractPropertyDefinition<T> addStandardDefinitions(AbstractPropertyDefinition<T> propertyDefinition, Element propertyElement, PropertyType propertyType) {

		String nameAttr = propertyElement.getAttribute("name");
		String displayNameAttr = propertyElement.getAttribute("displayName");
		String localNameAttr = propertyElement.getAttribute("localName");
		String queryNameAttr = propertyElement.getAttribute("queryName");

		propertyDefinition.setId(nameAttr);
		propertyDefinition.setDisplayName(displayNameAttr);
		propertyDefinition.setLocalName(localNameAttr);
		propertyDefinition.setQueryName(queryNameAttr);
		propertyDefinition.setCardinality(Cardinality.SINGLE);
		propertyDefinition.setPropertyType(propertyType);

		return propertyDefinition;
	}

	public static Properties processProperties(Element cmisElement) {
		PropertiesImpl properties = new PropertiesImpl();

		Element propertiesElement = XmlUtils.getFirstChildTag(cmisElement, "properties");
		Iterator<Node> propertyIterator = XmlUtils.getChildTags(propertiesElement, "property").iterator();
		while (propertyIterator.hasNext()) {
			Element propertyElement = (Element) propertyIterator.next();
			String propertyValue = XmlUtils.getStringValue(propertyElement);
			String nameAttr = propertyElement.getAttribute("name");
			String typeAttr = propertyElement.getAttribute("type");
			PropertyType propertyType = PropertyType.STRING;
			if(StringUtils.isNotEmpty(typeAttr)) {
				propertyType = PropertyType.fromValue(typeAttr);
			}
			if(StringUtils.isEmpty(typeAttr) && nameAttr.startsWith("cmis:")) {
				propertyType = PropertyType.ID;
			}

			boolean isNull = Boolean.parseBoolean(propertyElement.getAttribute("isNull"));
			if(isNull)
				propertyValue = null;

			switch (propertyType) {
			case ID:
				properties.addProperty(new PropertyIdImpl(addStandardDefinitions(new PropertyIdDefinitionImpl(), propertyElement, propertyType), propertyValue));
				break;
			case STRING:
				properties.addProperty(new PropertyStringImpl(addStandardDefinitions(new PropertyStringDefinitionImpl(), propertyElement, propertyType), propertyValue));
				break;
			case INTEGER:
				BigInteger bigInt = null;
				if(StringUtils.isNotEmpty(propertyValue)) {
					bigInt = new BigInteger(propertyValue);
				}
				properties.addProperty(new PropertyIntegerImpl(addStandardDefinitions(new PropertyIntegerDefinitionImpl(), propertyElement, propertyType), bigInt));
				break;
			case DATETIME:
				GregorianCalendar gregorianCalendar = null;
				if(StringUtils.isNotEmpty(propertyValue)) {
					String formatStringAttr = propertyElement.getAttribute("formatString");
					String timezoneAttr = propertyElement.getAttribute("timezone");
					if (StringUtils.isEmpty(formatStringAttr)) {
						formatStringAttr = CmisUtils.FORMATSTRING_BY_DEFAULT;
					}

					DateTimeFormatter formatter = DateFormatUtils.getDateTimeFormatterWithOptionalComponents(formatStringAttr);
					try {
						TemporalAccessor parse = formatter.parse(propertyValue);

						gregorianCalendar = new GregorianCalendar();
						gregorianCalendar.setTimeInMillis(Instant.from(parse).getEpochSecond());

						if (StringUtils.isNotEmpty(timezoneAttr)) {
							gregorianCalendar.setTimeZone(TimeZone.getTimeZone(timezoneAttr));
						}
					} catch (DateTimeParseException e) {
						log.warn("exception parsing date [{}] using formatString [{}]", propertyValue, formatStringAttr, e);
					}
				}
				properties.addProperty(new PropertyDateTimeImpl(addStandardDefinitions(new PropertyDateTimeDefinitionImpl(), propertyElement, propertyType), gregorianCalendar));
				break;
			case BOOLEAN:
				Boolean bool = null;
				if(StringUtils.isNotEmpty(propertyValue)) {
					bool = Boolean.parseBoolean(propertyValue);
				}
				properties.addProperty(new PropertyBooleanImpl(addStandardDefinitions(new PropertyBooleanDefinitionImpl(), propertyElement, propertyType), bool));
				break;
			case DECIMAL:
				BigDecimal decimal = null;
				if(StringUtils.isNotEmpty(propertyValue)) {
					decimal = new BigDecimal(propertyValue);
				}
				properties.addProperty(new PropertyDecimalImpl(addStandardDefinitions(new PropertyDecimalDefinitionImpl(), propertyElement, propertyType), decimal));
				break;
			case URI:
				properties.addProperty(new PropertyUriImpl(addStandardDefinitions(new PropertyUriDefinitionImpl(), propertyElement, propertyType), propertyValue));
				break;
			case HTML:
				properties.addProperty(new PropertyHtmlImpl(addStandardDefinitions(new PropertyHtmlDefinitionImpl(), propertyElement, propertyType), propertyValue));
				break;
			default:
				log.warn("unparsable type [{}] for property [{}]", typeAttr, propertyValue);
				continue; //Skip all and continue with the next property!
			}

			log.debug("set property name [{}] value [{}]", nameAttr, propertyValue);
		}

		return properties;
	}

	public static XmlBuilder propertyDefintions2Xml(Map<String, PropertyDefinition<?>> propertyDefinitions) {
		XmlBuilder propertyDefinitionsXml = new XmlBuilder("propertyDefinitions");
		for (Entry<String, PropertyDefinition<?>> entry : propertyDefinitions.entrySet()) {
			XmlBuilder propertyDefinitionXml = new XmlBuilder("propertyDefinition");
			PropertyDefinition<?> definition = entry.getValue();
			propertyDefinitionXml.addAttribute("id", definition.getId());
			propertyDefinitionXml.addAttribute("displayName", definition.getDisplayName());
			propertyDefinitionXml.addAttribute("description", definition.getDescription());
			propertyDefinitionXml.addAttribute("localName", definition.getLocalName());
			propertyDefinitionXml.addAttribute("localNamespace", definition.getLocalNamespace());
			propertyDefinitionXml.addAttribute("queryName", definition.getQueryName());
			propertyDefinitionXml.addAttribute("cardinality", definition.getCardinality().toString());
			propertyDefinitionXml.addAttribute("propertyType", definition.getPropertyType().value());
			propertyDefinitionXml.addAttribute("updatability", definition.getUpdatability().toString());
			if(definition.isInherited() != null)
				propertyDefinitionXml.addAttribute("inherited", definition.isInherited());
			if(definition.isOpenChoice() != null)
				propertyDefinitionXml.addAttribute("openChoice", definition.isOpenChoice());
			if(definition.isOrderable() != null)
				propertyDefinitionXml.addAttribute("orderable", definition.isOrderable());
			if(definition.isQueryable() != null)
				propertyDefinitionXml.addAttribute("queryable", definition.isQueryable());
			if(definition.isRequired() != null)
				propertyDefinitionXml.addAttribute("required", definition.isRequired());
			if(definition.getDefaultValue() != null && definition.getDefaultValue().size() > 0) {
				String defValue = definition.getDefaultValue().get(0).toString();
				propertyDefinitionXml.addAttribute("defaultValue", defValue);
			}
			if(definition.getChoices() != null && definition.getChoices().size() > 0) {
				propertyDefinitionXml.addAttribute("choices", "not implemented");
			}
			propertyDefinitionsXml.addSubElement(propertyDefinitionXml);
		}
		return propertyDefinitionsXml;
	}

	public static XmlBuilder typeDefinition2Xml(ObjectType objectType) {
		XmlBuilder typeXml = new XmlBuilder("typeDefinition");
		typeXml.addAttribute("id", objectType.getId());
		typeXml.addAttribute("description", objectType.getDescription());
		typeXml.addAttribute("displayName", objectType.getDisplayName());
		typeXml.addAttribute("localName", objectType.getLocalName());
		typeXml.addAttribute("localNamespace", objectType.getLocalNamespace());
		typeXml.addAttribute("baseTypeId", objectType.getBaseTypeId().value());
		typeXml.addAttribute("parentTypeId", objectType.getParentTypeId());
		typeXml.addAttribute("queryName", objectType.getQueryName());

		if(objectType.isControllableAcl() != null)
			typeXml.addAttribute("controllableACL", objectType.isControllableAcl());
		if(objectType.isControllablePolicy() != null)
			typeXml.addAttribute("controllablePolicy", objectType.isControllablePolicy());
		if(objectType.isCreatable() != null)
			typeXml.addAttribute("creatable", objectType.isCreatable());
		if(objectType.isFileable() != null)
			typeXml.addAttribute("fileable", objectType.isFileable());
		if(objectType.isControllableAcl() != null)
			typeXml.addAttribute("fulltextIndexed", objectType.isFulltextIndexed());
		if(objectType.isIncludedInSupertypeQuery() != null)
			typeXml.addAttribute("includedInSupertypeQuery", objectType.isIncludedInSupertypeQuery());
		if(objectType.isQueryable() != null)
			typeXml.addAttribute("queryable", objectType.isQueryable());

		typeXml.addSubElement(CmisUtils.typeMutability2xml(objectType.getTypeMutability()));

		Map<String, PropertyDefinition<?>> propertyDefinitions = objectType.getPropertyDefinitions();
		if(propertyDefinitions != null) {
			typeXml.addSubElement(CmisUtils.propertyDefintions2Xml(propertyDefinitions));
		}

		return typeXml;
	}

	private static XmlBuilder typeMutability2xml(TypeMutability typeMutability) {
		XmlBuilder xmlBuilder = new XmlBuilder("typeMutability");
		if(typeMutability != null) {
			if(typeMutability.canCreate() != null)
				xmlBuilder.addAttribute("create", typeMutability.canCreate());
			if(typeMutability.canDelete() != null)
				xmlBuilder.addAttribute("delete", typeMutability.canDelete());
			if(typeMutability.canUpdate() != null)
				xmlBuilder.addAttribute("update", typeMutability.canUpdate());
		}
		return xmlBuilder;
	}

	private static TypeMutability xml2typeMutability(Element typeXml) {
		TypeMutabilityImpl typeMutability = new TypeMutabilityImpl();
		if(typeXml.hasAttribute("create"))
			typeMutability.setCanCreate(CmisUtils.parseBooleanAttr(typeXml, "create"));
		if(typeXml.hasAttribute("update"))
			typeMutability.setCanUpdate(CmisUtils.parseBooleanAttr(typeXml, "update"));
		if(typeXml.hasAttribute("delete"))
			typeMutability.setCanDelete(CmisUtils.parseBooleanAttr(typeXml, "delete"));
		return typeMutability;
	}

	public static TypeDefinition xml2TypeDefinition(Element typeXml, CmisVersion cmisVersion) {
		String id = typeXml.getAttribute("id");
		String description = typeXml.getAttribute("description");
		String displayName = typeXml.getAttribute("displayName");
		String localName = typeXml.getAttribute("localName");
		String localNamespace = typeXml.getAttribute("localNamespace");
		String baseTypeId = typeXml.getAttribute("baseTypeId");
		String parentId = typeXml.getAttribute("parentTypeId");
		String queryName = typeXml.getAttribute("queryName");

		TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		MutableTypeDefinition definition = null;

		if(BaseTypeId.CMIS_DOCUMENT == BaseTypeId.fromValue(baseTypeId)) {
			definition = factory.createBaseDocumentTypeDefinition(cmisVersion);
		}
		else if(BaseTypeId.CMIS_FOLDER == BaseTypeId.fromValue(baseTypeId)) {
			definition = factory.createBaseFolderTypeDefinition(cmisVersion);
		}
		else if(BaseTypeId.CMIS_ITEM == BaseTypeId.fromValue(baseTypeId)) {
			definition = factory.createBaseItemTypeDefinition(cmisVersion);
		}
		else if(BaseTypeId.CMIS_POLICY == BaseTypeId.fromValue(baseTypeId)) {
			definition = factory.createBasePolicyTypeDefinition(cmisVersion);
		}
		else if(BaseTypeId.CMIS_RELATIONSHIP == BaseTypeId.fromValue(baseTypeId)) {
			definition = factory.createBaseRelationshipTypeDefinition(cmisVersion);
		}
		else if(BaseTypeId.CMIS_SECONDARY == BaseTypeId.fromValue(baseTypeId)) {
			definition = factory.createBaseSecondaryTypeDefinition(cmisVersion);
		}
		definition.setDescription(description);
		definition.setDisplayName(displayName);
		definition.setId(id);
		definition.setLocalName(localName);
		definition.setLocalNamespace(localNamespace);
		definition.setParentTypeId(parentId);
		definition.setQueryName(queryName);

		Element propertyDefinitions = XmlUtils.getFirstChildTag(typeXml, "propertyDefinitions");
		Collection<Node> propertyDefinitionList = XmlUtils.getChildTags(propertyDefinitions, "propertyDefinition");
		if(propertyDefinitionList != null) {
			for (Node node : propertyDefinitionList) {
				definition.addPropertyDefinition(CmisUtils.xml2PropertyDefinition((Element) node));
			}
		}

		definition.setIsControllableAcl(CmisUtils.parseBooleanAttr(typeXml, "controllableACL"));
		definition.setIsControllablePolicy(CmisUtils.parseBooleanAttr(typeXml, "controllablePolicy"));
		definition.setIsCreatable(CmisUtils.parseBooleanAttr(typeXml, "creatable"));
		definition.setIsFileable(CmisUtils.parseBooleanAttr(typeXml, "fileable"));
		definition.setIsFulltextIndexed(CmisUtils.parseBooleanAttr(typeXml, "fulltextIndexed"));
		definition.setIsIncludedInSupertypeQuery(CmisUtils.parseBooleanAttr(typeXml, "includedInSupertypeQuery"));
		definition.setIsQueryable(CmisUtils.parseBooleanAttr(typeXml, "queryable"));

		Element typeMutabilityXml = XmlUtils.getFirstChildTag(typeXml, "typeMutability");
		if(typeMutabilityXml != null) {
			definition.setTypeMutability(CmisUtils.xml2typeMutability(typeMutabilityXml));
		}

		return definition;
	}

	private static PropertyDefinition<?> xml2PropertyDefinition(Element propertyDefinitionXml) {
		MutablePropertyDefinition<?> definition = null;

		PropertyType type = PropertyType.fromValue(propertyDefinitionXml.getAttribute("propertyType"));
		switch (type) {
		case ID:
			definition = new PropertyIdDefinitionImpl();
			break;
		case BOOLEAN:
			definition = new PropertyBooleanDefinitionImpl();
			break;
		case DATETIME:
			definition = new PropertyDateTimeDefinitionImpl();
			break;
		case DECIMAL:
			definition = new PropertyDecimalDefinitionImpl();
			break;
		case INTEGER:
			definition = new PropertyIntegerDefinitionImpl();
			break;
		case HTML:
			definition = new PropertyHtmlDefinitionImpl();
			break;
		case URI:
			definition = new PropertyUriDefinitionImpl();
			break;
		case STRING:
		default:
			definition = new PropertyStringDefinitionImpl();
			break;
		}

		definition.setPropertyType(type);
		definition.setId(propertyDefinitionXml.getAttribute("id"));
		definition.setDisplayName(CmisUtils.parseStringAttr(propertyDefinitionXml, "displayName"));
		definition.setDescription(CmisUtils.parseStringAttr(propertyDefinitionXml, "description"));
		definition.setLocalName(CmisUtils.parseStringAttr(propertyDefinitionXml, "localName"));
		definition.setLocalNamespace(CmisUtils.parseStringAttr(propertyDefinitionXml, "localNamespace"));
		definition.setQueryName(CmisUtils.parseStringAttr(propertyDefinitionXml, "queryName"));

		if(propertyDefinitionXml.hasAttribute("cardinality")) {
			definition.setCardinality(Cardinality.valueOf(propertyDefinitionXml.getAttribute("cardinality")));
		}
		if(propertyDefinitionXml.hasAttribute("updatability")) {
			definition.setUpdatability(Updatability.valueOf(propertyDefinitionXml.getAttribute("updatability")));
		}

		definition.setIsInherited(CmisUtils.parseBooleanAttr(propertyDefinitionXml, "inherited"));
		definition.setIsOpenChoice(CmisUtils.parseBooleanAttr(propertyDefinitionXml, "openChoice"));
		definition.setIsOrderable(CmisUtils.parseBooleanAttr(propertyDefinitionXml, "orderable"));
		definition.setIsQueryable(CmisUtils.parseBooleanAttr(propertyDefinitionXml, "queryable"));
		definition.setIsRequired(CmisUtils.parseBooleanAttr(propertyDefinitionXml, "required"));

		if(propertyDefinitionXml.hasAttribute("defaultValue")) {
			//TODO: turn this into a list
			List defaultValues = new ArrayList();
			String defaultValue = propertyDefinitionXml.getAttribute("defaultValue");
			defaultValues.add(defaultValue);
			definition.setDefaultValue(defaultValues);
		}

		return definition;
	}
	/**
	 * Helper class
	 */
	private static String parseStringAttr(Element xml, String attribute) {
		if(xml.hasAttribute(attribute)) {
			return xml.getAttribute(attribute);
		}
		return null;
	}

	/**
	 * Helper class because Boolean can also be NULL in some cases with CMIS
	 */
	private static Boolean parseBooleanAttr(Element xml, String attribute) {
		if(xml.hasAttribute(attribute)) {
			return Boolean.parseBoolean(xml.getAttribute(attribute));
		}
		return null;
	}

	/**
	 * Helper class because BigInteger can also be NULL in some cases with CMIS
	 */
	private static BigInteger parseBigIntegerAttr(Element xml, String attribute) {
		if(xml.hasAttribute(attribute)) {
			String value = xml.getAttribute(attribute);
			Long longValue = Long.parseLong(value);
			return BigInteger.valueOf(longValue);
		}
		return null;
	}


	public static XmlBuilder repositoryInfo2xml(RepositoryInfo repository) {
		XmlBuilder repositoryXml = new XmlBuilder("repository");
		repositoryXml.addAttribute("cmisVersion", repository.getCmisVersion().value());
		repositoryXml.addAttribute("cmisVersionSupported", repository.getCmisVersionSupported());
		repositoryXml.addAttribute("description", repository.getDescription());
		repositoryXml.addAttribute("id", repository.getId());
		repositoryXml.addAttribute("latestChangeLogToken", repository.getLatestChangeLogToken());
		repositoryXml.addAttribute("name", repository.getName());
		repositoryXml.addAttribute("principalIdAnonymous", repository.getPrincipalIdAnonymous());
		repositoryXml.addAttribute("principalIdAnyone", repository.getPrincipalIdAnyone());
		repositoryXml.addAttribute("productName", repository.getProductName());
		repositoryXml.addAttribute("productVersion", repository.getProductVersion());
		repositoryXml.addAttribute("rootFolderId", repository.getRootFolderId());
		repositoryXml.addAttribute("thinClientUri", repository.getThinClientUri());
		repositoryXml.addAttribute("vendorName", repository.getVendorName());
		repositoryXml.addAttribute("changesIncomplete", repository.getChangesIncomplete());
		repositoryXml.addSubElement(CmisUtils.aclCapabilities2xml(repository.getAclCapabilities()));
		repositoryXml.addSubElement(CmisUtils.repositoryCapabilities2xml(repository.getCapabilities()));
		repositoryXml.addSubElement(CmisUtils.changesOnType2xml(repository.getChangesOnType()));

		return repositoryXml;
	}

	private static XmlBuilder aclCapabilities2xml(AclCapabilities aclCapabilities) {
		XmlBuilder aclCapabilitiesXml = new XmlBuilder("aclCapabilities");
		if(aclCapabilities != null) {
			aclCapabilitiesXml.addAttribute("aclPropagation", aclCapabilities.getAclPropagation().name());
			aclCapabilitiesXml.addAttribute("supportedPermissions", aclCapabilities.getSupportedPermissions().name());
			aclCapabilitiesXml.addSubElement(permissionMapping2xml(aclCapabilities.getPermissionMapping()));
			aclCapabilitiesXml.addSubElement(permissionDefinitionList2xml(aclCapabilities.getPermissions()));
		}
		return aclCapabilitiesXml;
	}

	private static XmlBuilder permissionMapping2xml(Map<String, PermissionMapping> permissionMapping) {
		XmlBuilder permissionMappingXml = new XmlBuilder("permissionMapping");
		for (Entry<String, PermissionMapping> entry : permissionMapping.entrySet()) {
			XmlBuilder permissionXml = new XmlBuilder("permission");
			permissionXml.addAttribute("name", entry.getKey());
			PermissionMapping mapping = entry.getValue();
			StringBuilder types = new StringBuilder();

			for (String permission : mapping.getPermissions()) {
				if(types.length() > 0)
					types.append(",");

				types.append(permission);
			}
			permissionXml.setValue(types.toString());
			permissionMappingXml.addSubElement(permissionXml);
		}
		return permissionMappingXml;
	}

	private static XmlBuilder permissionDefinitionList2xml(List<PermissionDefinition> list) {
		XmlBuilder permissionsXml = new XmlBuilder("permissions");

		for (PermissionDefinition permission : list) {
			XmlBuilder permissionXml = new XmlBuilder("permissions");
			permissionXml.addAttribute("id", permission.getId());
			permissionXml.addAttribute("description", permission.getDescription());
			permissionsXml.addSubElement(permissionXml);
		}
		return permissionsXml;
	}

	private static XmlBuilder repositoryCapabilities2xml(RepositoryCapabilities capabilities) {
		XmlBuilder repositoryXml = new XmlBuilder("repositoryCapabilities");
		if(capabilities != null) {
			if(capabilities.isAllVersionsSearchableSupported() != null)
				repositoryXml.addAttribute("allVersionsSearchable", capabilities.isAllVersionsSearchableSupported());
			if(capabilities.isGetDescendantsSupported() != null)
				repositoryXml.addAttribute("supportsGetDescendants", capabilities.isGetDescendantsSupported());
			if(capabilities.isGetFolderTreeSupported() != null)
				repositoryXml.addAttribute("supportsGetFolderTree", capabilities.isGetFolderTreeSupported());
			if(capabilities.isMultifilingSupported() != null)
				repositoryXml.addAttribute("supportsMultifiling", capabilities.isMultifilingSupported());
			if(capabilities.isPwcSearchableSupported() != null)
				repositoryXml.addAttribute("isPwcSearchable", capabilities.isPwcSearchableSupported());
			if(capabilities.isPwcUpdatableSupported() != null)
				repositoryXml.addAttribute("isPwcUpdatable", capabilities.isPwcUpdatableSupported());
			if(capabilities.isUnfilingSupported() != null)
				repositoryXml.addAttribute("supportsUnfiling", capabilities.isUnfilingSupported());
			if(capabilities.isVersionSpecificFilingSupported() != null)
				repositoryXml.addAttribute("supportsVersionSpecificFiling", capabilities.isVersionSpecificFilingSupported());

			if(capabilities.getAclCapability() != null)
				repositoryXml.addAttribute("aclCapability", capabilities.getAclCapability().name());
			if(capabilities.getChangesCapability() != null)
				repositoryXml.addAttribute("changesCapability", capabilities.getChangesCapability().name());
			if(capabilities.getContentStreamUpdatesCapability() != null)
				repositoryXml.addAttribute("contentStreamUpdatesCapability", capabilities.getContentStreamUpdatesCapability().name());
			if(capabilities.getJoinCapability() != null)
				repositoryXml.addAttribute("joinCapability", capabilities.getJoinCapability().name());
			if(capabilities.getOrderByCapability() != null)
				repositoryXml.addAttribute("orderByCapability", capabilities.getOrderByCapability().name());
			if(capabilities.getQueryCapability() != null)
				repositoryXml.addAttribute("queryCapability", capabilities.getQueryCapability().name());
			if(capabilities.getRenditionsCapability() != null)
				repositoryXml.addAttribute("renditionsCapability", capabilities.getRenditionsCapability().name());

			repositoryXml.addSubElement(CmisUtils.creatablePropertyTypes2xml(capabilities.getCreatablePropertyTypes()));
			repositoryXml.addSubElement(CmisUtils.newTypeSettableAttributes2xml(capabilities.getNewTypeSettableAttributes()));
		}
		return repositoryXml;
	}

	private static XmlBuilder creatablePropertyTypes2xml(CreatablePropertyTypes creatablePropertyTypes) {
		XmlBuilder creatablePropertyTypesXml = new XmlBuilder("creatablePropertyTypes");
		if(creatablePropertyTypes != null) {
			for (PropertyType propertyType : creatablePropertyTypes.canCreate()) {
				creatablePropertyTypesXml.addSubElement(CmisUtils.buildXml("type", propertyType.name()));
			}
		}

		return creatablePropertyTypesXml;
	}

	private static XmlBuilder newTypeSettableAttributes2xml(NewTypeSettableAttributes newTypeSettableAttributes) {
		XmlBuilder newTypeSettableAttributesXml = new XmlBuilder("newTypeSettableAttributes");

		if(newTypeSettableAttributes != null) {
			if(newTypeSettableAttributes.canSetControllableAcl() != null)
				newTypeSettableAttributesXml.addAttribute("canSetControllableAcl", newTypeSettableAttributes.canSetControllableAcl());
			if(newTypeSettableAttributes.canSetControllablePolicy() != null)
				newTypeSettableAttributesXml.addAttribute("canSetControllablePolicy", newTypeSettableAttributes.canSetControllablePolicy());
			if(newTypeSettableAttributes.canSetCreatable() != null)
				newTypeSettableAttributesXml.addAttribute("canSetCreatable", newTypeSettableAttributes.canSetCreatable());
			if(newTypeSettableAttributes.canSetDescription() != null)
				newTypeSettableAttributesXml.addAttribute("canSetDescription", newTypeSettableAttributes.canSetDescription());
			if(newTypeSettableAttributes.canSetDisplayName() != null)
				newTypeSettableAttributesXml.addAttribute("canSetDisplayName", newTypeSettableAttributes.canSetDisplayName());
			if(newTypeSettableAttributes.canSetFileable() != null)
				newTypeSettableAttributesXml.addAttribute("canSetFileable", newTypeSettableAttributes.canSetFileable());
			if(newTypeSettableAttributes.canSetFulltextIndexed() != null)
				newTypeSettableAttributesXml.addAttribute("canSetFulltextIndexed", newTypeSettableAttributes.canSetFulltextIndexed());
			if(newTypeSettableAttributes.canSetId() != null)
				newTypeSettableAttributesXml.addAttribute("canSetId", newTypeSettableAttributes.canSetId());
			if(newTypeSettableAttributes.canSetIncludedInSupertypeQuery() != null)
				newTypeSettableAttributesXml.addAttribute("canSetIncludedInSupertypeQuery", newTypeSettableAttributes.canSetIncludedInSupertypeQuery());
			if(newTypeSettableAttributes.canSetLocalName() != null)
				newTypeSettableAttributesXml.addAttribute("canSetLocalName", newTypeSettableAttributes.canSetLocalName());
			if(newTypeSettableAttributes.canSetLocalNamespace() != null)
				newTypeSettableAttributesXml.addAttribute("canSetLocalNamespace", newTypeSettableAttributes.canSetLocalNamespace());
			if(newTypeSettableAttributes.canSetQueryable() != null)
				newTypeSettableAttributesXml.addAttribute("canSetQueryable", newTypeSettableAttributes.canSetQueryable());
			if(newTypeSettableAttributes.canSetQueryName() != null)
				newTypeSettableAttributesXml.addAttribute("canSetQueryName", newTypeSettableAttributes.canSetQueryName());
		}

		return newTypeSettableAttributesXml;
	}

	private static XmlBuilder changesOnType2xml(List<BaseTypeId> list) {
		XmlBuilder changesOnTypeXml = new XmlBuilder("changesOnTypes");
		for (BaseTypeId baseTypeId : list) {
			changesOnTypeXml.addSubElement(CmisUtils.buildXml("type", baseTypeId.name()));
		}

		return changesOnTypeXml;
	}

	public static RepositoryInfo xml2repositoryInfo(Element cmisResult) {
		RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();

		repositoryInfo.setCmisVersion(CmisVersion.fromValue(cmisResult.getAttribute("cmisVersion")));
		repositoryInfo.setCmisVersionSupported(cmisResult.getAttribute("cmisVersionSupported"));
		repositoryInfo.setDescription(cmisResult.getAttribute("description"));
		repositoryInfo.setId(cmisResult.getAttribute("id"));
		repositoryInfo.setLatestChangeLogToken(cmisResult.getAttribute("latestChangeLogToken"));
		repositoryInfo.setName(cmisResult.getAttribute("name"));
		repositoryInfo.setPrincipalAnonymous(cmisResult.getAttribute("principalIdAnonymous"));
		repositoryInfo.setPrincipalAnyone(cmisResult.getAttribute("principalIdAnyone"));
		repositoryInfo.setProductName(cmisResult.getAttribute("productName"));
		repositoryInfo.setProductVersion(cmisResult.getAttribute("productVersion"));
		repositoryInfo.setRootFolder(cmisResult.getAttribute("rootFolderId"));
		repositoryInfo.setThinClientUri(cmisResult.getAttribute("thinClientUri"));
		repositoryInfo.setVendorName(cmisResult.getAttribute("vendorName"));
		repositoryInfo.setChangesIncomplete(CmisUtils.parseBooleanAttr(cmisResult, "changesIncomplete"));
		repositoryInfo.setAclCapabilities(CmisUtils.xml2aclCapabilities(cmisResult));
		repositoryInfo.setCapabilities(CmisUtils.xml2capabilities(cmisResult));
		repositoryInfo.setChangesOnType(CmisUtils.xml2changesOnType(cmisResult));

		return repositoryInfo;
	}

	private static AclCapabilities xml2aclCapabilities(Element cmisResult) {
		AclCapabilitiesDataImpl aclCapabilities = new AclCapabilitiesDataImpl();

		Element aclCapabilitiesXml = XmlUtils.getFirstChildTag(cmisResult, "aclCapabilities");

		aclCapabilities.setAclPropagation(AclPropagation.valueOf(aclCapabilitiesXml.getAttribute("aclPropagation")));
		aclCapabilities.setSupportedPermissions(SupportedPermissions.valueOf(aclCapabilitiesXml.getAttribute("supportedPermissions")));

		aclCapabilities.setPermissionMappingData(CmisUtils.xml2permissionMapping(aclCapabilitiesXml));
		aclCapabilities.setPermissionDefinitionData(CmisUtils.xml2permissionDefinitionList(aclCapabilitiesXml));

		return aclCapabilities;
	}

	private static Map<String, PermissionMapping> xml2permissionMapping(Element cmisResult) {
		Map<String, PermissionMapping> permissionMap = new HashMap<>();

		Element permissionMapXml = XmlUtils.getFirstChildTag(cmisResult, "permissionMapping");
		if (permissionMapXml == null) {
			return permissionMap;
		}
		for (Node node : XmlUtils.getChildTags(permissionMapXml, "permission")) {
			Element element = (Element) node;
			String key = element.getAttribute("name");
			String types = XmlUtils.getStringValue(element);

			PermissionMappingDataImpl permissionMapData = new PermissionMappingDataImpl();
			List<String> permissions = StringUtil.split(types);
			permissionMapData.setPermissions(permissions);
			permissionMapData.setKey(key);
			permissionMap.put(key, permissionMapData);
		}
		return permissionMap;
	}

	private static List<PermissionDefinition> xml2permissionDefinitionList(Element cmisResult) {
		List<PermissionDefinition> permissionsList = new ArrayList<>();

		Element permissionsXml = XmlUtils.getFirstChildTag(cmisResult, "permissions");
		if (permissionsXml == null) {
			return permissionsList;
		}
		for (Node node : XmlUtils.getChildTags(permissionsXml, "permission")) {
			Element element = (Element) node;

			PermissionDefinitionDataImpl permissionDefinition = new PermissionDefinitionDataImpl();
			permissionDefinition.setId(element.getAttribute("id"));
			permissionDefinition.setDescription(element.getAttribute("description"));
			permissionsList.add(permissionDefinition);
		}
		return permissionsList;
	}

	private static RepositoryCapabilities xml2capabilities(Element cmisResult) {
		Element repositoryCapabilitiesXml = XmlUtils.getFirstChildTag(cmisResult, "repositoryCapabilities");
		RepositoryCapabilitiesImpl repositoryCapabilities = new RepositoryCapabilitiesImpl();

		repositoryCapabilities.setAllVersionsSearchable(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "allVersionsSearchable"));
		repositoryCapabilities.setSupportsGetDescendants(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "supportsGetDescendants"));
		repositoryCapabilities.setSupportsGetFolderTree(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "supportsGetFolderTree"));
		repositoryCapabilities.setSupportsMultifiling(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "supportsMultifiling"));
		repositoryCapabilities.setIsPwcSearchable(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "isPwcSearchable"));
		repositoryCapabilities.setIsPwcUpdatable(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "isPwcUpdatable"));
		repositoryCapabilities.setSupportsUnfiling(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "supportsUnfiling"));
		repositoryCapabilities.setSupportsVersionSpecificFiling(CmisUtils.parseBooleanAttr(repositoryCapabilitiesXml, "supportsVersionSpecificFiling"));
		repositoryCapabilities.setNewTypeSettableAttributes(CmisUtils.xml2newTypeSettableAttributes(repositoryCapabilitiesXml));
		repositoryCapabilities.setCreatablePropertyTypes(CmisUtils.xml2creatablePropertyTypes(repositoryCapabilitiesXml));

		//These enums don't have to be set, require a null check else Enum.valueOf will bom.
		if(StringUtils.isNotEmpty(repositoryCapabilitiesXml.getAttribute("aclCapability")))
			repositoryCapabilities.setCapabilityAcl(CapabilityAcl.valueOf(repositoryCapabilitiesXml.getAttribute("aclCapability")));
		if(StringUtils.isNotEmpty(repositoryCapabilitiesXml.getAttribute("changesCapability")))
			repositoryCapabilities.setCapabilityChanges(CapabilityChanges.valueOf(repositoryCapabilitiesXml.getAttribute("changesCapability")));
		if(StringUtils.isNotEmpty(repositoryCapabilitiesXml.getAttribute("contentStreamUpdatesCapability")))
			repositoryCapabilities.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.valueOf(repositoryCapabilitiesXml.getAttribute("contentStreamUpdatesCapability")));
		if(StringUtils.isNotEmpty(repositoryCapabilitiesXml.getAttribute("joinCapability")))
			repositoryCapabilities.setCapabilityJoin(CapabilityJoin.valueOf(repositoryCapabilitiesXml.getAttribute("joinCapability")));
		if(StringUtils.isNotEmpty(repositoryCapabilitiesXml.getAttribute("orderByCapability")))
			repositoryCapabilities.setCapabilityOrderBy(CapabilityOrderBy.valueOf(repositoryCapabilitiesXml.getAttribute("orderByCapability")));
		if(StringUtils.isNotEmpty(repositoryCapabilitiesXml.getAttribute("queryCapability")))
			repositoryCapabilities.setCapabilityQuery(CapabilityQuery.valueOf(repositoryCapabilitiesXml.getAttribute("queryCapability")));
		if(StringUtils.isNotEmpty(repositoryCapabilitiesXml.getAttribute("renditionsCapability")))
			repositoryCapabilities.setCapabilityRendition(CapabilityRenditions.valueOf(repositoryCapabilitiesXml.getAttribute("renditionsCapability")));

		return repositoryCapabilities;
	}

	private static NewTypeSettableAttributes xml2newTypeSettableAttributes(Element cmisResult) {
		Element newTypeSettableAttributesXml = XmlUtils.getFirstChildTag(cmisResult, "newTypeSettableAttributes");
		NewTypeSettableAttributesImpl newTypeSettableAttributes = new NewTypeSettableAttributesImpl();

		newTypeSettableAttributes.setCanSetControllableAcl(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetControllableAcl"));
		newTypeSettableAttributes.setCanSetControllablePolicy(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetControllablePolicy"));
		newTypeSettableAttributes.setCanSetCreatable(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetCreatable"));
		newTypeSettableAttributes.setCanSetDescription(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetDescription"));
		newTypeSettableAttributes.setCanSetDisplayName(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetDisplayName"));
		newTypeSettableAttributes.setCanSetFileable(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetFileable"));
		newTypeSettableAttributes.setCanSetFulltextIndexed(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetFulltextIndexed"));
		newTypeSettableAttributes.setCanSetId(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetId"));
		newTypeSettableAttributes.setCanSetIncludedInSupertypeQuery(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetIncludedInSupertypeQuery"));
		newTypeSettableAttributes.setCanSetLocalName(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetLocalName"));
		newTypeSettableAttributes.setCanSetLocalNamespace(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetLocalNamespace"));
		newTypeSettableAttributes.setCanSetQueryable(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetQueryable"));
		newTypeSettableAttributes.setCanSetQueryName(CmisUtils.parseBooleanAttr(newTypeSettableAttributesXml, "canSetQueryName"));

		return newTypeSettableAttributes;
	}

	private static CreatablePropertyTypes xml2creatablePropertyTypes(Element cmisResult) {
		CreatablePropertyTypesImpl creatablePropertyTypes = new CreatablePropertyTypesImpl();
		Element creatablePropertyTypesXml = XmlUtils.getFirstChildTag(cmisResult, "creatablePropertyTypes");
		if(creatablePropertyTypesXml != null) {
			Set<PropertyType> propertyTypeSet = new TreeSet<>();
			for (Node type : XmlUtils.getChildTags(cmisResult, "type")) {
				String value = XmlUtils.getStringValue((Element) type);
				if(StringUtils.isNotEmpty(value))
					propertyTypeSet.add(PropertyType.valueOf(value));
			}
			creatablePropertyTypes.setCanCreate(propertyTypeSet);
		}
		return creatablePropertyTypes;
	}

	private static List<BaseTypeId> xml2changesOnType(Element cmisResult) {
		List<BaseTypeId> baseTypeIds = new ArrayList<>();

		Element changesOnType = XmlUtils.getFirstChildTag(cmisResult, "changesOnTypes");
		if(changesOnType != null) {
			for (Node type : XmlUtils.getChildTags(cmisResult, "type")) {
				String value = XmlUtils.getStringValue((Element) type);
				if(StringUtils.isNotEmpty(value))
					baseTypeIds.add(BaseTypeId.valueOf(value));
			}
		}
		return baseTypeIds;
	}

	public static XmlBuilder typeDescendants2Xml(List<Tree<ObjectType>> objectTypes) {
		return typeDescendants2Xml(objectTypes, new XmlBuilder("typeDescendants"));
	}

	public static XmlBuilder typeDescendants2Xml(List<Tree<ObjectType>> objectTypes, XmlBuilder parent) {
		for (Tree<ObjectType> object : objectTypes) {
			XmlBuilder typeDescendantXml = new XmlBuilder("typeDescendant");
			typeDescendantXml.addSubElement(CmisUtils.typeDefinition2Xml(object.getItem()));

			typeDescendantXml.addSubElement(typeDescendants2Xml(object.getChildren(), new XmlBuilder("children")));

			parent.addSubElement(typeDescendantXml);
		}
		return parent;
	}

	public static List<TypeDefinitionContainer> xml2TypeDescendants(Element typeDefinitionsXml, CmisVersion cmisVersion) {
		List<TypeDefinitionContainer> typeDefinitionList = new ArrayList<>();
		Collection<Node> typeDescendantList = XmlUtils.getChildTags(typeDefinitionsXml, "typeDescendant");
		for (Node node : typeDescendantList) {
			Element typeDefinition = XmlUtils.getFirstChildTag((Element) node, "typeDefinition");
			TypeDefinition typeDef = CmisUtils.xml2TypeDefinition(typeDefinition, cmisVersion);
			TypeDefinitionContainerImpl typeDefinitionContainer = new TypeDefinitionContainerImpl(typeDef);

			Element children = XmlUtils.getFirstChildTag((Element) node, "children");
			typeDefinitionContainer.setChildren(xml2TypeDescendants(children, cmisVersion));

			typeDefinitionList.add(typeDefinitionContainer);
		}
		return typeDefinitionList;
	}

	public static void cmisObject2Xml(XmlBuilder cmisXml, CmisObject object) {
		if(object.getProperties() != null) {
			XmlBuilder propertiesXml = new XmlBuilder("properties");
			for (Iterator<Property<?>> it = object.getProperties().iterator(); it.hasNext();) {
				Property<?> property = it.next();
				propertiesXml.addSubElement(CmisUtils.getPropertyXml(property));
			}
			cmisXml.addSubElement(propertiesXml);
		}

		if(object.getAllowableActions() != null) {
			XmlBuilder allowableActionsXml = new XmlBuilder("allowableActions");
			Set<Action> actions = object.getAllowableActions().getAllowableActions();
			for (Action action : actions) {
				XmlBuilder actionXml = new XmlBuilder("action");
				actionXml.setValue(action.value());
				allowableActionsXml.addSubElement(actionXml);
			}
			cmisXml.addSubElement(allowableActionsXml);
		}

		if(object.getAcl() != null) {
			XmlBuilder isExactAclXml = new XmlBuilder("isExactAcl");
			isExactAclXml.setValue(object.getAcl().isExact().toString());
			cmisXml.addSubElement(isExactAclXml);
		}

		List<ObjectId> policies = object.getPolicyIds();
		if(policies != null) {
			XmlBuilder policiesXml = new XmlBuilder("policyIds");
			for (ObjectId objectId : policies) {
				XmlBuilder policyXml = new XmlBuilder("policyId");
				policyXml.setValue(objectId.getId());
				policiesXml.addSubElement(policyXml);
			}
			cmisXml.addSubElement(policiesXml);
		}

		XmlBuilder relationshipsXml = new XmlBuilder("relationships");
		List<Relationship> relationships = object.getRelationships();
		if(relationships != null) {
			for (Relationship relation : relationships) {
				XmlBuilder relationXml = new XmlBuilder("relation");
				relationXml.setValue(relation.getId());
				relationshipsXml.addSubElement(relationXml);
			}
		}
		cmisXml.addSubElement(relationshipsXml);
	}

	public static XmlBuilder objectData2Xml(ObjectData object) {
		return CmisUtils.objectData2Xml(object, new XmlBuilder("objectData"));
	}

	/**
	 * @param object to translate to xml
	 * @param cmisXml root XML element (defaults to creating a new 'objectData' element)
	 * @return the root XML element
	 */
	public static XmlBuilder objectData2Xml(ObjectData object, XmlBuilder cmisXml) {

		if(object.getProperties() != null) {
			XmlBuilder propertiesXml = new XmlBuilder("properties");
			for (Iterator<PropertyData<?>> it = object.getProperties().getPropertyList().iterator(); it.hasNext();) {
				propertiesXml.addSubElement(CmisUtils.getPropertyXml(it.next()));
			}
			cmisXml.addSubElement(propertiesXml);
		}

		if(object.getAllowableActions() != null) {
			XmlBuilder allowableActionsXml = new XmlBuilder("allowableActions");
			Set<Action> actions = object.getAllowableActions().getAllowableActions();
			for (Action action : actions) {
				XmlBuilder actionXml = new XmlBuilder("action");
				actionXml.setValue(action.value());
				allowableActionsXml.addSubElement(actionXml);
			}
			cmisXml.addSubElement(allowableActionsXml);
		}

		if(object.getAcl() != null) {
			XmlBuilder isExactAclXml = new XmlBuilder("isExactAcl");
			isExactAclXml.setValue(object.getAcl().isExact().toString());
			cmisXml.addSubElement(isExactAclXml);
		}

		cmisXml.addAttribute("id", object.getId());
		if(object.getBaseTypeId() != null)
			cmisXml.addAttribute("baseTypeId", object.getBaseTypeId().name());

		PolicyIdList policies = object.getPolicyIds();
		if(policies != null) {
			XmlBuilder policiesXml = new XmlBuilder("policyIds");
			for (String objectId : policies.getPolicyIds()) {
				XmlBuilder policyXml = new XmlBuilder("policyId");
				policyXml.setValue(objectId);
				policiesXml.addSubElement(policyXml);
			}
			cmisXml.addSubElement(policiesXml);
		}

		XmlBuilder relationshipsXml = new XmlBuilder("relationships");
		List<ObjectData> relationships = object.getRelationships();
		if(relationships != null) {
			for (ObjectData relation : relationships) {
				relationshipsXml.addSubElement(objectData2Xml(relation, new XmlBuilder("relation")));
			}
		}
		cmisXml.addSubElement(relationshipsXml);

		return cmisXml;
	}

	public static ObjectData xml2ObjectData(Element cmisElement, PipeLineSession context) {
		ObjectDataImpl impl = new ObjectDataImpl();

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
		String isExactAcl = XmlUtils.getChildTagAsString(cmisElement, "isExactAcl");
		if(isExactAcl != null) {
			impl.setIsExactAcl(Boolean.parseBoolean(isExactAcl));
		}

		// If the original object exists copy the permissions over. These cannot (and shouldn't) be changed)
		if(context != null) {
			CmisObject object = (CmisObject) context.get(CmisUtils.ORIGINAL_OBJECT_KEY);
			if(object != null) {
				impl.setAcl(object.getAcl());
			}
		}

		// Handle policyIds
		Element policyIdsElem = XmlUtils.getFirstChildTag(cmisElement, "policyIds");
		if(policyIdsElem != null) {
			PolicyIdListImpl policyIdList = new PolicyIdListImpl();
			List<String> policies = new ArrayList<>();

			Iterator<Node> policyIterator = XmlUtils.getChildTags(allowableActionsElem, "policyId").iterator();
			while (policyIterator.hasNext()) {
				String policyId = XmlUtils.getStringValue((Element) policyIterator.next());
				policies.add(policyId);
			}

			policyIdList.setPolicyIds(policies);
			impl.setPolicyIds(policyIdList);
		}

		// Handle properties
		impl.setProperties(CmisUtils.processProperties(cmisElement));

		Element relationshipsElem = XmlUtils.getFirstChildTag(cmisElement, "relationships");
		if(relationshipsElem != null) {
			List<ObjectData> relationships = new ArrayList<>();
			for (Node type : XmlUtils.getChildTags(relationshipsElem, "relation")) {
				ObjectData data = xml2ObjectData((Element) type, null);
				relationships.add(data);
			}
			impl.setRelationships(relationships);
		}

		impl.setRenditions(null);
		impl.setExtensions(null);
		impl.setChangeEventInfo(null);

		return impl;
	}

	public static ObjectList xml2ObjectList(Element result, PipeLineSession context) {
		ObjectListImpl objectList = new ObjectListImpl();
		objectList.setNumItems(CmisUtils.parseBigIntegerAttr(result, "numberOfItems"));
		objectList.setHasMoreItems(CmisUtils.parseBooleanAttr(result, "hasMoreItems"));

		List<ObjectData> objects = new ArrayList<>();

		Element objectsElem = XmlUtils.getFirstChildTag(result, "objects");
		for (Node type : XmlUtils.getChildTags(objectsElem, "objectData")) {
			ObjectData objectData = xml2ObjectData((Element) type, context);
			objects.add(objectData);
		}
		objectList.setObjects(objects);

		return objectList;
	}

	public static XmlBuilder objectList2xml(ObjectList result) {
		XmlBuilder objectListXml = new XmlBuilder("objectList");
		if(result.getNumItems() != null)
			objectListXml.addAttribute("numberOfItems", result.getNumItems().toString());
		if(result.hasMoreItems() != null)
			objectListXml.addAttribute("hasMoreItems", result.hasMoreItems());

		XmlBuilder objectDataXml = new XmlBuilder("objects");
		for (ObjectData objectData : result.getObjects()) {
			objectDataXml.addSubElement(CmisUtils.objectData2Xml(objectData));
		}
		objectListXml.addSubElement(objectDataXml);

		return objectListXml;
	}

	public static ObjectInFolderList xml2ObjectsInFolderList(Element result) {
		ObjectInFolderListImpl objectInFolderList = new ObjectInFolderListImpl();
		objectInFolderList.setNumItems(CmisUtils.parseBigIntegerAttr(result, "numberOfItems"));
		objectInFolderList.setHasMoreItems(CmisUtils.parseBooleanAttr(result, "hasMoreItems"));

		List<ObjectInFolderData> objects = new ArrayList<>();
		Element objectsElem = XmlUtils.getFirstChildTag(result, "objects");
		for (Node type : XmlUtils.getChildTags(objectsElem, "object")) {
			ObjectInFolderDataImpl oifd = new ObjectInFolderDataImpl();
			String pathSegment = CmisUtils.parseStringAttr(result, "pathSegment");
			oifd.setPathSegment(pathSegment);

			ObjectData objectData = xml2ObjectData((Element) type, null);
			oifd.setObject(objectData);
			objects.add(oifd);
		}
		objectInFolderList.setObjects(objects);

		return objectInFolderList;
	}

	public static XmlBuilder objectInFolderList2xml(ObjectInFolderList oifs) {
		XmlBuilder objectInFolderListXml = new XmlBuilder("objectInFolderList");
		if(oifs.getNumItems() != null)
			objectInFolderListXml.addAttribute("numberOfItems", oifs.getNumItems().toString());
		if(oifs.hasMoreItems() != null)
			objectInFolderListXml.addAttribute("hasMoreItems", oifs.hasMoreItems());

		XmlBuilder objectDataListXml = new XmlBuilder("objects");
		for (ObjectInFolderData objectData : oifs.getObjects()) {
			XmlBuilder objectDataXml = new XmlBuilder("object");
			String path = objectData.getPathSegment();
			objectDataXml.addAttribute("pathSegment", path);
			CmisUtils.objectData2Xml(objectData.getObject(), objectDataXml);
			objectDataListXml.addSubElement(objectDataXml);
		}
		objectInFolderListXml.addSubElement(objectDataListXml);

		return objectInFolderListXml;
	}
}
