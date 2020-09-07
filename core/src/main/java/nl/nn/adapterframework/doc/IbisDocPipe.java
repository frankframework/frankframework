/*
   Copyright 2018-2020 WeAreFrank!

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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.objects.AClass;
import nl.nn.adapterframework.doc.objects.AFolder;
import nl.nn.adapterframework.doc.objects.AMethod;
import nl.nn.adapterframework.doc.objects.BeanProperty;
import nl.nn.adapterframework.doc.objects.IbisBean;
import nl.nn.adapterframework.doc.objects.IbisBeanExtra;
import nl.nn.adapterframework.doc.objects.MethodExtra;
import nl.nn.adapterframework.doc.objects.SchemaInfo;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Generate documentation and XSD for code completion of beautiful Ibis configurations in Eclipse
 *
 * @author Jaco de Groot
 */
public class IbisDocPipe extends FixedForwardPipe {
	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		SchemaInfo schemaInfo;
		try {
			schemaInfo = InfoBuilder.build();
		} catch(Exception e) {
			throw new PipeRunException(this, "Could not gather the necessary information", e);
		}
		String uri = null;
		ParameterList parameterList = getParameterList();
		if (parameterList != null) {
			try {
				ParameterValueList pvl = parameterList.getValues(message, session);
				uri = pvl.getParameterValue("uri").asStringValue(null);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}
		if (uri == null) {
			throw new PipeRunException(this, getLogPrefix(session) + "uri parameter not found or null");
		}
		String result = "Not found";
		String contentType = "text/html";
		if ("/ibisdoc/ibisdoc.xsd".equals(uri)) {
			result = getSchema(schemaInfo);
			contentType = "application/xml";
		} else if ("/ibisdoc/uglify_lookup.xml".equals(uri)) {
			result = getUglifyLookup(schemaInfo);
			contentType = "application/xml";
		} else if ("/ibisdoc/ibisdoc.json".equals(uri)) {
			result = getJson(schemaInfo);
			contentType = "application/json";
		} else if ("/ibisdoc".equals(uri)) {
			result = "<html>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.html\">ibisdoc.html (deprecated)</a><br/>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.xsd\">ibisdoc.xsd</a><br/>\n"
					+ "  <a href=\"ibisdoc/uglify_lookup.xml\">uglify_lookup.xml</a><br/>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.json\">ibisdoc.json</a><br/>\n"
					+ "  <a href=\"../iaf/ibisdoc\">The new ibisdoc application</a><br/>\n"
					+ "</html>";
		} else if ("/ibisdoc/ibisdoc.html".equals(uri)) {
			result = "<html>\n"
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
		} else if (uri.endsWith(".html")) {
			if ("/ibisdoc/topmenu.html".equals(uri)) {
				StringBuffer topmenuHtml = new StringBuffer();
				getMenuHtml(topmenuHtml, null, null, schemaInfo);
				result = topmenuHtml.toString();
			} else if ("/ibisdoc/all.html".equals(uri)) {
				StringBuffer allHtml = new StringBuffer();
				getMenuHtml(null, allHtml, null, schemaInfo);
				result = allHtml.toString();
			} else if ("/ibisdoc/excludes.html".equals(uri)) {
				StringBuffer excludesHtml = new StringBuffer();
				for (String exclude : schemaInfo.getExcludeFilters()) {
					excludesHtml.append("<p> " + exclude + "</p>\n");
				}
				result = excludesHtml.toString();
			} else {
				if (uri.length() > "/ibisdoc/".length() && uri.indexOf(".") != -1) {
					Map<String, TreeSet<IbisBean>> groups = schemaInfo.getGroups();
					String page = uri.substring("/ibisdoc/".length(), uri.lastIndexOf("."));
					if (groups.get(page) != null) {
						Map<String, String> groupsHtml = new HashMap<String, String>();
						getMenuHtml(null, null, groupsHtml, schemaInfo);
						result = groupsHtml.get(page);
					} else {
						String beanHtml = getBeanHtml(page, schemaInfo);
						if (beanHtml != null) {
							result = beanHtml;
						}
					}
				}
			}
		}
		session.put("contentType", contentType);
		return new PipeRunResult(getForward(), result);
	}

	private String getSchema(SchemaInfo schemaInfo) throws PipeRunException {
		XmlBuilder schema;
		XmlBuilder element;
		XmlBuilder complexType;
		XmlBuilder choice;

		schema = new XmlBuilder("schema", "xs", "http://www.w3.org/2001/XMLSchema");
		schema.addAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
		schema.addAttribute("elementFormDefault", "qualified");

		element = new XmlBuilder("element", "xs", "http://www.w3.org/2001/XMLSchema");
		element.addAttribute("name", "Configuration");
		element.addAttribute("type", "ConfigurationType");
		schema.addSubElement(element);

		element = new XmlBuilder("element", "xs", "http://www.w3.org/2001/XMLSchema");
		element.addAttribute("name", "Module");
		element.addAttribute("type", "ModuleType");
		schema.addSubElement(element);

		element = new XmlBuilder("element", "xs", "http://www.w3.org/2001/XMLSchema");
		element.addAttribute("name", "Adapter");
		element.addAttribute("type", "AdapterType");
		schema.addSubElement(element);

		complexType = new XmlBuilder("complexType", "xs", "http://www.w3.org/2001/XMLSchema");
		complexType.addAttribute("name", "ModuleType");
		schema.addSubElement(complexType);

		choice = new XmlBuilder("choice", "xs", "http://www.w3.org/2001/XMLSchema");
		choice.addAttribute("minOccurs", "0");
		choice.addAttribute("maxOccurs", "unbounded");
		complexType.addSubElement(choice);

		element = new XmlBuilder("element", "xs", "http://www.w3.org/2001/XMLSchema");
		element.addAttribute("name", "Adapter");
		element.addAttribute("type", "AdapterType");
		element.addAttribute("minOccurs", "0");
		choice.addSubElement(element);

		element = new XmlBuilder("element", "xs", "http://www.w3.org/2001/XMLSchema");
		element.addAttribute("name", "Job");
		element.addAttribute("type", "JobType");
		element.addAttribute("minOccurs", "0");
		choice.addSubElement(element);

		for (IbisBeanExtra ibisBeanExtra : schemaInfo.getIbisBeansExtra()) {
			addIbisBeanToSchema(ibisBeanExtra, schema, schemaInfo);
		}
		return schema.toXML(true);
	}

	private static void addIbisBeanToSchema(IbisBeanExtra ibisBeanExtra, XmlBuilder schema, SchemaInfo schemaInfo) {
		XmlBuilder complexType = new XmlBuilder("complexType", "xs", "http://www.w3.org/2001/XMLSchema");
		complexType.addAttribute("name", ibisBeanExtra.getIbisBean().getName() + "Type");
		if (ibisBeanExtra.getIbisBean().getClazz() != null) {
			List<XmlBuilder> choices = new ArrayList<XmlBuilder>();
			for (MethodExtra methodExtra : ibisBeanExtra.getSortedClassMethods()) {
				if (methodExtra.getIbisMethod() != null) {
					if (methodExtra.getChildIbisBeans() != null) {
						// Pipes, Senders, ...
						if (!ignore(ibisBeanExtra.getIbisBean(), methodExtra.getChildIbisBeanName(), schemaInfo)) {
							XmlBuilder choice = new XmlBuilder("choice", "xs", "http://www.w3.org/2001/XMLSchema");
							choice.addAttribute("minOccurs", "0");
							addMaxOccurs(choice, methodExtra.getMaxOccurs());

							for (IbisBean childIbisBean : methodExtra.getChildIbisBeans()) {
								choice.addSubElement(getChildIbisBeanSchemaElement(childIbisBean.getName(), 1));
							}
							choices.add(choice);
						}
					} else {
						// Param, Forward, ...
						if (methodExtra.getChildIbisBeanName() != null) {
							if (methodExtra.isExistingIbisBean()) {
								choices.add(getChildIbisBeanSchemaElement(
										methodExtra.getChildIbisBeanName(), methodExtra.getMaxOccurs()));
							}
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
		}
		addPropertiesToSchemaOrHtml(ibisBeanExtra, complexType, null);
		schema.addSubElement(complexType);
	}

	private static XmlBuilder getChildIbisBeanSchemaElement(String childIbisBeanName, int maxOccurs) {
		XmlBuilder element = new XmlBuilder("element", "xs", "http://www.w3.org/2001/XMLSchema");
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

	private static void addPropertiesToSchemaOrHtml(IbisBeanExtra ibisBeanExtra, XmlBuilder beanComplexType,
			StringBuffer beanHtml) {
		if (ibisBeanExtra.getProperties() != null) {
			Iterator<String> iterator = new TreeSet<String>(ibisBeanExtra.getProperties().keySet()).iterator();
			while (iterator.hasNext()) {
				String property = (String)iterator.next();
				BeanProperty beanProperty = ibisBeanExtra.getProperties().get(property);
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

    private String getJson(SchemaInfo schemaInfo) {
        JSONArray newFolders = new JSONArray();
        JSONArray newClasses;
        JSONArray newMethods;

        try {
            for (AFolder folder : schemaInfo.getFolders()) {
                JSONObject folderObject = new JSONObject();
                folderObject.put("name", folder.getName());

                newClasses = new JSONArray();
                for (AClass aClass : folder.getClasses()) {
                    JSONObject classObject = new JSONObject();
                    classObject.put("name", aClass.getClazz().getSimpleName());
                    classObject.put("packageName", aClass.getClazz().getName());
                    classObject.put("javadocLink", aClass.getJavadocLink());
                    classObject.put("superClasses", aClass.getSuperClasses());

                    newMethods = new JSONArray();
                    for (AMethod method : aClass.getMethods()) {
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

	private static boolean ignore(IbisBean ibisBean, String childIbisBeanName, SchemaInfo schemaInfo) {
		boolean ignore = false;
		for (String namePart : schemaInfo.getIgnores().keySet()) {
			if (ibisBean.getName().indexOf(namePart) != -1 && childIbisBeanName.equals(
					schemaInfo.getIgnores().get(namePart))) {
				ignore = true;
			}
		}
		return ignore;
	}

	private static String getUglifyLookup(SchemaInfo schemaInfo) {
		StringBuffer result = new StringBuffer();
		result.append("<Elements>\n");
		Map<String, TreeSet<IbisBean>> groups = schemaInfo.getGroups();
		for (String group : schemaInfo.getGroups().keySet()) {
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

	private static void getMenuHtml(
			StringBuffer topmenuHtml,
			StringBuffer allHtml,
			Map<String, String> groupsHtml,
			SchemaInfo schemaInfo) {
		if (topmenuHtml == null) topmenuHtml = new StringBuffer();
		if (allHtml == null)  allHtml = new StringBuffer();
		if (groupsHtml == null) groupsHtml = new HashMap<String, String>();
		Map<String, TreeSet<IbisBean>> groups = schemaInfo.getGroups();

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

	private static String getBeanHtml(String beanName, SchemaInfo schemaInfo) {
		for (IbisBeanExtra ibisBeanExtra : schemaInfo.getIbisBeansExtra()) {
			if (beanName.equals(ibisBeanExtra.getIbisBean().getName())) {
				StringBuffer result = new StringBuffer();
				result.append(beanName);
				result.append("<table border='1'>");
				result.append("<tr><th>class</th><th>attribute</th><th>description</th><th>default</th></tr>");
				addPropertiesToSchemaOrHtml(ibisBeanExtra, null, result);
				result.append("</table>");
				return result.toString();
			}
		}
		return null;
	}

}
