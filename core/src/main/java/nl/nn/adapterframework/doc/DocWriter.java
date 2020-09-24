/* 
Copyright 2019, 2020 WeAreFrank! 

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

package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.doc.objects.ClassJson;
import nl.nn.adapterframework.doc.objects.FolderJson;
import nl.nn.adapterframework.doc.objects.MethodJson;
import nl.nn.adapterframework.doc.objects.BeanProperty;
import nl.nn.adapterframework.doc.objects.IbisBean;
import nl.nn.adapterframework.doc.objects.MethodXsd;
import nl.nn.adapterframework.doc.objects.DocInfo;
import nl.nn.adapterframework.util.XmlBuilder;

public class DocWriter {
	private static final String XML_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";

	public static String getSchema(DocInfo docInfo) throws PipeRunException {
		XmlBuilder schema;
		XmlBuilder complexType;
		XmlBuilder choice;

		schema = new XmlBuilder("schema", "xs", XML_SCHEMA_URI);
		schema.addAttribute("xmlns:xs", XML_SCHEMA_URI);
		schema.addAttribute("elementFormDefault", "qualified");

		addElement(schema, "Configuration", "ConfigurationType");
		addElement(schema, "Module", "ModuleType");
		addElement(schema, "Adapter", "AdapterType");
		complexType = addComplexType(schema, "ModuleType");
		choice = addChoice(complexType, "0", "unbounded");
		addElement(choice, "Adapter", "AdapterType", "0");
		addElement(choice, "Job", "JobType", "0");
		for (IbisBean ibisBean : docInfo.getIbisBeans()) {
			addIbisBeanToSchema(ibisBean, schema, docInfo);
		}
		return schema.toXML(true);
	}

	private static void addElement(
			XmlBuilder choice,
			String elementName,
			String elementType,
			String minOccurs) {
		XmlBuilder element;
		element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", elementName);
		element.addAttribute("type", elementType);
		element.addAttribute("minOccurs", minOccurs);
		choice.addSubElement(element);
	}

	private static void addElement(XmlBuilder schema, String elementName, String elementType) {
		XmlBuilder element;
		element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", elementName);
		element.addAttribute("type", elementType);
		schema.addSubElement(element);
	}

	private static XmlBuilder addComplexType(XmlBuilder schema, String complexTypeName) {
		XmlBuilder complexType;
		complexType = new XmlBuilder("complexType", "xs", XML_SCHEMA_URI);
		complexType.addAttribute("name", complexTypeName);
		schema.addSubElement(complexType);
		return complexType;
	}

	private static XmlBuilder addChoice(XmlBuilder complexType, String minOccurs, String maxOccurs) {
		XmlBuilder choice;
		choice = new XmlBuilder("choice", "xs", XML_SCHEMA_URI);
		choice.addAttribute("minOccurs", minOccurs);
		choice.addAttribute("maxOccurs", maxOccurs);
		complexType.addSubElement(choice);
		return choice;
	}

	private static void addIbisBeanToSchema(IbisBean ibisBean, XmlBuilder schema, DocInfo docInfo) {
		if ((ibisBean.getClazz() != null) && (ibisBean.getSortedMethodsXsd().length >= 1)) {
			XmlBuilder complexType = new XmlBuilder("complexType", "xs", XML_SCHEMA_URI);
			complexType.addAttribute("name", ibisBean.getName() + "Type");
			List<XmlBuilder> choices = new ArrayList<XmlBuilder>();
			for (MethodXsd methoXsd : ibisBean.getSortedMethodsXsd()) {
				if (methoXsd.getChildIbisBeans() != null) {
					// Pipes, Senders, ...
					if (!ignore(ibisBean, methoXsd.getChildIbisBeanName(), docInfo)) {
						XmlBuilder choice = new XmlBuilder("choice", "xs", XML_SCHEMA_URI);
						choice.addAttribute("minOccurs", "0");
						addMaxOccurs(choice, methoXsd.getMaxOccurs());
							for (IbisBean childIbisBean : methoXsd.getChildIbisBeans()) {
							choice.addSubElement(getChildIbisBeanSchemaElement(childIbisBean.getName(), 1));
						}
						choices.add(choice);
					}
				} else {
					// Param, Forward, ...
					if (methoXsd.getChildIbisBeanName() != null) {
						if (methoXsd.isExistingIbisBean()) {
							choices.add(getChildIbisBeanSchemaElement(
									methoXsd.getChildIbisBeanName(), methoXsd.getMaxOccurs()));
						}
					}
				}
			}
			if (choices.size() > 0) {
				XmlBuilder sequence = new XmlBuilder("sequence");
				for (XmlBuilder choice : choices) {
					sequence.addSubElement(choice);
				}
				complexType.addSubElement(sequence);
			}
			addPropertiesToSchemaOrHtml(ibisBean, complexType, null);
			schema.addSubElement(complexType);
		}
	}

	private static boolean ignore(IbisBean ibisBean, String childIbisBeanName, DocInfo docInfo) {
		boolean ignore = false;
		for (String namePart : docInfo.getIgnores().keySet()) {
			if (ibisBean.getName().indexOf(namePart) != -1 && childIbisBeanName.equals(
					docInfo.getIgnores().get(namePart))) {
				ignore = true;
			}
		}
		return ignore;
	}

	private static XmlBuilder getChildIbisBeanSchemaElement(String childIbisBeanName, int maxOccurs) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", childIbisBeanName);
		element.addAttribute("type", childIbisBeanName + "Type");
		element.addAttribute("minOccurs", "0");
		addMaxOccurs(element, maxOccurs);
		return element;
	}

	private static void addMaxOccurs(XmlBuilder element, int maxOccurs) {
		if (maxOccurs == -1) {
			element.addAttribute("maxOccurs", "unbounded");
		} else {
			// Default value for element maxOccurs is 1
			if (maxOccurs != 1) {
				element.addAttribute("maxOccurs", maxOccurs);
			}
		}
	}

	private static void addPropertiesToSchemaOrHtml(IbisBean ibisBean, XmlBuilder beanComplexType,
			StringBuffer beanHtml) {
		if (ibisBean.getProperties() != null) {
			Iterator<String> iterator = new TreeSet<String>(ibisBean.getProperties().keySet()).iterator();
			while (iterator.hasNext()) {
				String property = (String)iterator.next();
				BeanProperty beanProperty = ibisBean.getProperties().get(property);
				if (!beanProperty.isExcluded()) {
					XmlBuilder attribute = null;
					if (beanComplexType != null) {
						attribute = new XmlBuilder("attribute");
						attribute.addAttribute("name", property);
						attribute.addAttribute("type", "xs:string");
						if (property.equals("name")) {
							attribute.addAttribute("use", "required");
						}
						beanComplexType.addSubElement(attribute);
					}
					Method method = beanProperty.getMethod();
					if (beanHtml != null) {
						beanHtml.append("<tr>");
						beanHtml.append("<td>" + method.getDeclaringClass().getSimpleName() + "</td>");
						beanHtml.append("<td>" + property + "</td>");
					}
					if (beanProperty.isHasDocumentation()) {
						if (beanComplexType != null) {
							String ibisDocValue = beanProperty.getDescription();
							if (StringUtils.isNotEmpty(beanProperty.getDefaultValue())) {
								ibisDocValue = ibisDocValue + " (default: " + beanProperty.getDefaultValue() + ")";
							}
							XmlBuilder annotation = new XmlBuilder("annotation");
							XmlBuilder documentation = new XmlBuilder("documentation");
							attribute.addSubElement(annotation);
							annotation.addSubElement(documentation);
							documentation.setValue(ibisDocValue);
						}
						if (beanHtml != null) {
							beanHtml.append("<td>" + beanProperty.getDescription() + "</td>");
							beanHtml.append("<td>" + beanProperty.getDefaultValue() + "</td>");
						}
					} else {
						if (beanHtml != null) {
							beanHtml.append("<td></td><td></td>");
						}
					}
					if (beanHtml != null) {
						beanHtml.append("</tr>");
					}
				}
			}
		}
	}

    public static String getJson(DocInfo docInfo) {
        JSONArray newFolders = new JSONArray();
        JSONArray newClasses;
        JSONArray newMethods;

        try {
            for (FolderJson folder : docInfo.getFolders()) {
                JSONObject folderObject = new JSONObject();
                folderObject.put("name", folder.getName());

                newClasses = new JSONArray();
                for (ClassJson classJson : folder.getClasses()) {
                    JSONObject classObject = new JSONObject();
                    classObject.put("name", classJson.getClazz().getSimpleName());
                    classObject.put("packageName", classJson.getClazz().getName());
                    classObject.put("javadocLink", classJson.getJavadocLink());
                    classObject.put("superClasses", classJson.getSuperClassesSimpleNames());

                    newMethods = new JSONArray();
                    for (MethodJson method : classJson.getMethods()) {
                        JSONObject methodObject = new JSONObject();
                        methodObject.put("name", method.getName());
                        methodObject.put("originalClassName", method.getOriginalClassName());
                        methodObject.put("description", method.getDescription());
                        methodObject.put("defaultValue", method.getDefaultValue());
                        methodObject.put("order", method.getOrder());
                        methodObject.put("deprecated", method.isDeprecated());
                        newMethods.put(methodObject);
                    }
                    classObject.put("methods", newMethods);
                    newClasses.put(classObject);
                }
                folderObject.put("classes", newClasses);
                newFolders.put(folderObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return newFolders.toString();
    }

	public static String getUglifyLookup(DocInfo docInfo) {
		StringBuffer result = new StringBuffer();
		result.append("<Elements>\n");
		Map<String, TreeSet<IbisBean>> groups = docInfo.getGroups();
		for (String group : docInfo.getGroups().keySet()) {
			for (IbisBean ibisBean : groups.get(group)) {
				String type = "";
				String className = ibisBean.getClazz().getName();
				String name = ibisBean.getName();
				if (group.equals("Other")) {
					type = name.substring(0,  1).toLowerCase() + name.substring(1);
					if (!name.equals("Receiver")) {
						className = "";
					}
				} else {
					type = group.substring(0,  1).toLowerCase() + group.substring(1, group.length() - 1);
				}
				result.append("  <Element>\n");
				result.append("    <Name>" + name + "</Name>\n");
				result.append("    <Type>" + type + "</Type>\n");
				if (StringUtils.isNotEmpty(className)) {
					result.append("    <ClassName>" + className + "</ClassName>\n");
				} else {
					result.append("    <ClassName/>\n");
				}
				result.append("  </Element>\n");
			}
		}
		result.append("</Elements>\n");
		return result.toString();
	}

	public static String getHtmlFrankDocAll(DocInfo docInfo) {
		StringBuffer allHtml = new StringBuffer();
		getMenuHtml(null, allHtml, null, docInfo);
		return allHtml.toString();
	}

	private static void getMenuHtml(
			StringBuffer topmenuHtml,
			StringBuffer allHtml,
			Map<String, String> groupsHtml,
			DocInfo docInfo) {
		if (topmenuHtml == null) topmenuHtml = new StringBuffer();
		if (allHtml == null)  allHtml = new StringBuffer();
		if (groupsHtml == null) groupsHtml = new HashMap<String, String>();
		Map<String, TreeSet<IbisBean>> groups = docInfo.getGroups();

		for (String group : groups.keySet()) {
			topmenuHtml.append("<a href='" + group + ".html' target='submenuFrame'>" + group + "</a><br/>\n");
			StringBuffer submenuHtml = new StringBuffer();
			for (IbisBean ibisBean : groups.get(group)) {
				submenuHtml.append("<a href='" + ibisBean.getName()
						+ ".html' target='contentFrame'>" + ibisBean.getName() + "</a>");
				submenuHtml.append("&nbsp;[");
				submenuHtml.append("<a href='https://javadoc.ibissource.org/latest/"
						+ ibisBean.getClazz().getName().replaceAll("\\.", "/") + ".html' target='contentFrame'>"
						+ "javadoc</a>");
				submenuHtml.append("]<br/>\n");
			}
			groupsHtml.put(group, submenuHtml.toString());
			allHtml.append(submenuHtml.toString());
		}
		topmenuHtml.append("<a href='all.html' target='submenuFrame'>All</a><br/>\n");
		topmenuHtml.append("<br/>\n");
		topmenuHtml.append("<a href='excludes.html' target='contentFrame'>Excludes</a><br/>\n");
	}

	public static String getHtmlFrankDocGroupOrBean(String page, DocInfo docInfo) {
		String result = null;
		if (docInfo.getGroups().get(page) != null) {
			Map<String, String> groupsHtml = new HashMap<String, String>();
			getMenuHtml(null, null, groupsHtml, docInfo);
			result = groupsHtml.get(page);
		} else {
			String beanHtml = getBeanHtml(page, docInfo);
			if (beanHtml != null) {
				result = beanHtml;
			}
		}
		return result;
	}

	private static String getBeanHtml(String beanName, DocInfo docInfo) {
		for (IbisBean ibisBean : docInfo.getIbisBeans()) {
			if (beanName.equals(ibisBean.getName())) {
				StringBuffer result = new StringBuffer();
				result.append(beanName);
				result.append("<table border='1'>");
				result.append("<tr><th>class</th><th>attribute</th><th>description</th><th>default</th></tr>");
				addPropertiesToSchemaOrHtml(ibisBean, null, result);
				result.append("</table>");
				return result.toString();
			}
		}
		return null;
	}

	public static String getHtmlFrankDocTopLevel() {
		return "<html>\n"
				+ "<head>\n"
				+ "<title>Adapter elements</title>\n"
				+ "</head>\n"
				+ "  <frameset cols=\"200,*\">\n"
				+ "    <frameset rows=\"300,*\">\n"
				+ "      <frame name=\"topmenuFrame\" src=\"topmenu.html\" >\n"
				+ "      <frame name=\"submenuFrame\" src=\"\">\n"
				+ "    </frameset>\n"
				+ "    <frame name=\"contentFrame\" src=\"\">\n"
				+ "  </frameset>\n"
				+ "  <noframes>\n"
				+ "    <body bgcolor=\"#FFFFFF\">\n"
				+ "      Your browser doesn't support frames!\n"
				+ "    </body>\n"
				+ "  </noframes>\n"
				+ "</html>";
	}

	public static String getHtmlFrankDocTopMenu(DocInfo docInfo) {
		StringBuffer topmenuHtml = new StringBuffer();
		getMenuHtml(topmenuHtml, null, null, docInfo);
		return topmenuHtml.toString();
	}

	public static String getHtmlFrankDocExcludes(DocInfo docInfo) {
		StringBuffer excludesHtml = new StringBuffer();
		for (String exclude : docInfo.getExcludeFilters()) {
			excludesHtml.append("<p> " + exclude + "</p>\n");
		}
		return excludesHtml.toString();
	}
}
