/*
   Copyright 2018 Integration Partners

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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Generate documentation and XSD for code completion of beautiful Ibis configurations in Eclipse
 *
 * @author Jaco de Groot
 */
public class IbisDocPipe extends FixedForwardPipe {
	private static Set<String> excludeFilters = new TreeSet<String>();
	static {
		// Exclude classes that will give conflicts with existing, non-compatible bean definition of same name and class
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.esb\\.WsdlGeneratorPipe");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.ifsa\\.IfsaRequesterSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.ifsa\\.IfsaProviderListener");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco2\\.SapSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco2\\.SapListener");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco3\\.SapSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco3\\.SapListener");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.CommandSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.EchoSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.FixedResultSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.LogSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.MailSender");
		excludeFilters.add(".*\\.IbisstoreSummaryQuerySender");
		// Exclude classes that cannot be used directly in configurations
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.MessageSendingPipe");
	}
	private static Map<String, String> ignores = new HashMap<String, String>();
	static {
		// Adding a sender to a listener has been used in the past but is not (commonly) used anymore
		ignores.put("Listener", "Sender");
	}
	private static List<String> excludeNameAttribute = new ArrayList<String>();
	static {
		excludeNameAttribute.add("putValidator");
		excludeNameAttribute.add("putWrapper");
	}
	private static List<String> overwriteMaxOccursToOne= new ArrayList<String>();
	static {
		// When registerPipeLine in Adapter has been renamed to setPipeLine this workaround can be removed.
		overwriteMaxOccursToOne.add("registerPipeLine");
	}
	private static List<String> overwriteMaxOccursToUnbounded = new ArrayList<String>();
	static {
		// The setSender in ParallelSenders adds senders to a list. When setSender has been renamed to addSender this
		// workaround can be removed.
		overwriteMaxOccursToUnbounded.add("parallelSendersSender");
	}
	// Influence the order of elements in the XSD, this will override the alphabetic order.
	// Instead of using the Maps below it might be a good idea to use annotations on the specified methods. 
	private static Map<String, Integer> sortWeightAdapter = new HashMap<String, Integer>();
	static {
		sortWeightAdapter.put("registerReceiver", 100);
	}
	private static Map<String, Integer> sortWeightReceiver = new HashMap<String, Integer>();
	static {
		sortWeightReceiver.put("setListener", 100);
		sortWeightReceiver.put("setErrorSender", 90);
		sortWeightReceiver.put("setErrorStorage", 80);
		sortWeightReceiver.put("setMessageLog", 70);
		sortWeightReceiver.put("setSender", 60);
	}
	private static Map<String, Integer> sortWeightPipeline = new HashMap<String, Integer>();
	static {
		sortWeightPipeline.put("registerCache", 100);
		sortWeightPipeline.put("setLocker", 90);
		sortWeightPipeline.put("setInputValidator", 80);
		sortWeightPipeline.put("setInputWrapper", 70);
		sortWeightPipeline.put("addPipe", 60);
		sortWeightPipeline.put("registerPipeLineExit", 50);
		sortWeightPipeline.put("setOutputWrapper", 40);
		sortWeightPipeline.put("setOutputValidator", 30);
	}
	private static Map<String, Integer> sortWeight = new HashMap<String, Integer>();
	static {
		sortWeight.put("registerCache", 100);
		sortWeight.put("setLocker", 90);
		sortWeight.put("setInputWrapper", 80);
		sortWeight.put("setInputValidator", 70);
		sortWeight.put("setSender", 60);
		sortWeight.put("setListener", 50);
		sortWeight.put("setMessageLog", 40);
		sortWeight.put("setOutputValidator", 30);
		sortWeight.put("setOutputWrapper", 20);
	}
	private static Map<String, String> copyPropterties = new HashMap<String, String>();
	static {
		// FileSender extends FileHandler which FilePipe cannot because it already extends FixedForwardPipe.
		// Might be a good idea to specify this with an annotation.
		copyPropterties.put("FilePipe", "FileSender");
	}
	// Cache groups for better performance, don't use it directly, use getGroups()
	private static Map<String, TreeSet<IbisBean>> cachedGroups;
	private static Map<String, String> errors = new HashMap<String, String>();

	private static synchronized Map<String, TreeSet<IbisBean>> getGroups() {
		if (cachedGroups == null) {
			Map<String, TreeSet<IbisBean>> groups = new LinkedHashMap<String, TreeSet<IbisBean>>();
			addIbisBeans("Listeners", getClass("nl.nn.adapterframework.core.IListener"), groups);
			addIbisBeans("Senders", getClass("nl.nn.adapterframework.core.ISender"), groups);
			addIbisBeans("Pipes", getClass("nl.nn.adapterframework.core.IPipe"), groups);
			addIbisBeans("ErrorStorages", getClass("nl.nn.adapterframework.core.ITransactionalStorage"), "TransactionalStorage", groups);
			addIbisBeans("MessageLogs", getClass("nl.nn.adapterframework.core.ITransactionalStorage"), "TransactionalStorage", groups);
			addIbisBeans("ErrorSenders", getClass("nl.nn.adapterframework.core.ISender"), "Sender", groups);
			addIbisBeans("InputValidators", groups.get("Pipes"), "ValidatorPipe", groups);
			addIbisBeans("OutputValidators", groups.get("Pipes"), "ValidatorPipe", groups);
			addIbisBeans("InputWrappers", groups.get("Pipes"), "WrapperPipe", groups);
			addIbisBeans("OutputWrappers", groups.get("Pipes"), "WrapperPipe", groups);
			TreeSet<IbisBean> otherIbisBeans = new TreeSet<IbisBean>();
			otherIbisBeans.add(new IbisBean("Configuration", getClass("nl.nn.adapterframework.configuration.Configuration")));
			otherIbisBeans.add(new IbisBean("Adapter", getClass("nl.nn.adapterframework.core.Adapter")));
			otherIbisBeans.add(new IbisBean("Receiver", getClass("nl.nn.adapterframework.receivers.GenericReceiver")));
			otherIbisBeans.add(new IbisBean("Pipeline", getClass("nl.nn.adapterframework.core.PipeLine")));
			otherIbisBeans.add(new IbisBean("Forward", getClass("nl.nn.adapterframework.core.PipeForward")));
			otherIbisBeans.add(new IbisBean("Exit", getClass("nl.nn.adapterframework.core.PipeLineExit")));
			otherIbisBeans.add(new IbisBean("Param", getClass("nl.nn.adapterframework.parameters.Parameter")));
			otherIbisBeans.add(new IbisBean("Job", getClass("nl.nn.adapterframework.scheduler.JobDef")));
			otherIbisBeans.add(new IbisBean("Locker", getClass("nl.nn.adapterframework.util.Locker")));
			otherIbisBeans.add(new IbisBean("Cache", getClass("nl.nn.adapterframework.cache.EhCache")));
			otherIbisBeans.add(new IbisBean("DirectoryCleaner", getClass("nl.nn.adapterframework.util.DirectoryCleaner")));
			groups.put("Other", otherIbisBeans);
			cachedGroups = groups;
		}
		return cachedGroups;
	}

	private static void addIbisBeans(String group, Class<?> clazz, Map<String, TreeSet<IbisBean>> groups) {
		addIbisBeans(group, clazz, null, groups);
	}

	private static void addIbisBeans(String group, Class<?> clazz, String nameLastPartToReplaceWithGroupName,
			Map<String, TreeSet<IbisBean>> groups) {
		TreeSet<IbisBean> ibisBeans = new TreeSet<IbisBean>();
		if (clazz != null && clazz.isInterface()) {
			Set<SpringBean> springBeans = getSpringBeans(clazz);
			for (SpringBean springBean : springBeans) {
				addIbisBean(group, toUpperCamelCase(springBean.getName()), nameLastPartToReplaceWithGroupName,
						springBean.getClazz(), ibisBeans);
			}
		}
		groups.put(group, ibisBeans);
	}

	private static void addIbisBeans(String group, TreeSet<IbisBean> ibisBeansUnfiltered,
			String nameLastPartToReplaceWithGroupName, Map<String, TreeSet<IbisBean>> groups) {
		TreeSet<IbisBean> ibisBeans = new TreeSet<IbisBean>();
		for (IbisBean ibisBean : ibisBeansUnfiltered) {
			if (ibisBean.getName().endsWith(nameLastPartToReplaceWithGroupName)) {
				addIbisBean(group, ibisBean.getName(), nameLastPartToReplaceWithGroupName, ibisBean.getClazz(), ibisBeans);
			}
		}
		groups.put(group, ibisBeans);
	}

	private static void addIbisBean(String group, String beanName, String nameLastPartToReplaceWithGroupName,
			Class<?> clazz, TreeSet<IbisBean> ibisBeans) {
		if (nameLastPartToReplaceWithGroupName != null) {
			if (beanName.endsWith(nameLastPartToReplaceWithGroupName)) {
				ibisBeans.add(new IbisBean(replaceNameLastPartWithGroupName(group, beanName, nameLastPartToReplaceWithGroupName), clazz));
			}
		} else {
			// Normalize listeners to end with Listener, pipes to end with Pipe and senders to end with Sender 
			String suffix = group.substring(0, 1).toUpperCase() + group.substring(1, group.length() - 1);
			if (!beanName.endsWith(suffix)) {
				beanName = beanName + suffix;
			}
			// Rename the default pipe (for senders) to a more intuitive name
			if (beanName.equals("GenericMessageSendingPipe")) {
				beanName = "SenderPipe";
			}
			ibisBeans.add(new IbisBean(beanName, clazz));
		}
	}

	private static Set<SpringBean> getSpringBeans(Class<?> interfaze) {
		Set<SpringBean> result = new HashSet<SpringBean>();
		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(interfaze));
		for (String excludeFilter : excludeFilters) {
			addExcludeFilter(scanner, excludeFilter);
		}
		boolean success = false;
		int maxTries = 100;
		int tryCount = 0;
		while (!success && tryCount < maxTries) {
			tryCount++;
			try {
				scanner.scan("nl.nn.adapterframework");
				success = true;
			} catch(BeanDefinitionStoreException e) {
				String excludeFilter = e.getMessage();
				excludeFilter = excludeFilter.substring(excludeFilter.indexOf(".jar!/") + 6);
				excludeFilter = excludeFilter.substring(0, excludeFilter.indexOf(".class"));
				excludeFilter = excludeFilter.replaceAll("/", "\\\\.");
				excludeFilter = excludeFilter.substring(0, excludeFilter.lastIndexOf('.') + 1) + ".*";
				excludeFilters.add(excludeFilter);
				addExcludeFilter(scanner, excludeFilter);
				errors.put(excludeFilter, e.getMessage());
			}
		}
		String[] beans = beanDefinitionRegistry.getBeanDefinitionNames();
		for (int i = 0; i < beans.length; i++) {
			String name = beans[i];
			String className = beanDefinitionRegistry.getBeanDefinition(name).getBeanClassName();
			Class<?> clazz = getClass(className);
			if (clazz != null && clazz.getModifiers() == Modifier.PUBLIC) {
				result.add(new SpringBean(beans[i], clazz));
			}
		}
		return result;
	}

	private static void addExcludeFilter(ClassPathBeanDefinitionScanner scanner, String excludeFilter) {
		scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(excludeFilter)));
	}

	private static Class<?> getClass(String className) {
		try {
			return Class.forName(className);
		} catch (Throwable t) {
			errors.put(className, t.getClass() + ": " + t.getMessage());
			return null;
		}
	}

	private static String replaceNameLastPartWithGroupName(String group, String beanName,
			String nameLastPartToReplaceWithGroupName) {
		if (nameLastPartToReplaceWithGroupName != null && beanName.endsWith(nameLastPartToReplaceWithGroupName)) {
			return beanName.substring(0, beanName.lastIndexOf(nameLastPartToReplaceWithGroupName))
				+ group.substring(0, 1).toUpperCase() + group.substring(1, group.length() - 1);
		} else {
			return beanName;
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String uri = null;
		ParameterList parameterList = getParameterList();
		if (parameterList != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext((String) input, session);
			try {
				 uri = prc.getValues(getParameterList()).getParameterValue("uri").asStringValue(null);
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
			result = getSchema();
			contentType = "application/xml";
		} else if ("/ibisdoc/uglify_lookup.xml".equals(uri)) {
			result = getUglifyLookup();
			contentType = "application/xml";
		} else if ("/ibisdoc/ibisdoc.json".equals(uri)) {
			result = getJson();
			contentType = "application/json";
		} else if ("/ibisdoc".equals(uri)) {
			result = "<html>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.html\">ibisdoc.html (deprecated)</a><br/>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.xsd\">ibisdoc.xsd</a><br/>\n"
					+ "  <a href=\"ibisdoc/uglify_lookup.xml\">uglify_lookup.xml</a><br/>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.json\">ibisdoc.json</a><br/>\n"
					+ "  <a href=\"../../iaf/ibisdoc\">The new ibisdoc application</a><br/>\n"
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
				getMenuHtml(topmenuHtml, null, null);
				result = topmenuHtml.toString();
			} else if ("/ibisdoc/all.html".equals(uri)) {
				StringBuffer allHtml = new StringBuffer();
				getMenuHtml(null, allHtml, null);
				result = allHtml.toString();
			} else if ("/ibisdoc/excludes.html".equals(uri)) {
				StringBuffer excludesHtml = new StringBuffer();
				for (String exclude : excludeFilters) {
					excludesHtml.append("<p> " + exclude + "</p>\n");
				}
				result = excludesHtml.toString();
			} else if ("/ibisdoc/errors.html".equals(uri)) {
				StringBuffer errorsHtml = new StringBuffer();
				for (String key : errors.keySet()) {
					errorsHtml.append("<p> " + key + ": " + errors.get(key) + "</p>\n");
				}
				result = errorsHtml.toString();
			} else {
				if (uri.length() > "/ibisdoc/".length() && uri.indexOf(".") != -1) {
					Map<String, TreeSet<IbisBean>> groups = getGroups();
					String page = uri.substring("/ibisdoc/".length(), uri.lastIndexOf("."));
					if (groups.get(page) != null) {
						Map<String, String> groupsHtml = new HashMap<String, String>();
						getMenuHtml(null, null, groupsHtml);
						result = groupsHtml.get(page);
					} else {
						String beanHtml = getBeanHtml(page);
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

	private String getJson() {
		Map<String, TreeSet<IbisBean>> groups = getGroups();
		IbisDocExtractor extractor = new IbisDocExtractor();

		// For all folders
		for (String folder : groups.keySet()) {

			// For all classes
			for (IbisBean ibisBean : groups.get(folder)) {

				// Get the class
				Map<String, Method> beanProperties = getBeanProperties(ibisBean.getClazz());
				if (beanProperties != null) {

					// For each method in the class
					Iterator<String> iterator = new TreeSet<String>(beanProperties.keySet()).iterator();
					while (iterator.hasNext()) {

						// Get the method
						String property = iterator.next();
						Method method = beanProperties.get(property);

						// Get the ibisdoc of the method
						IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);

						// Create an array for the superclasses of this method's class
						ArrayList<String> superClasses = new ArrayList<String>();

						// Get the class
                        Class clazz = ibisBean.getClazz();
						int index = 0;

						// For each superclass
                        while (clazz.getSuperclass() != null) {

                        	// Assign a string with a priority number attached to it and add it to the array of superclasses
                        	String str = Integer.toString(index);
                        	superClasses.add(clazz.getSimpleName() + str);
                            clazz = clazz.getSuperclass();
                            index++;
                        }

                        String javadocLink = ibisBean.getClazz().getName().replaceAll("\\.", "/");

						// If there is an IbisDoc of the method
						if (ibisDoc != null) {

							// Get the values (description and default value)
							String[] ibisDocValues = ibisDoc.value();

							int order;
							int desc;
							int def;

							// If it doesnt have anything
							if (ibisDocValues[0].matches("\\d+")) {
								order = Integer.parseInt(ibisDocValues[0]);
								desc = 1;
								def = 2;
							} else {
								order = 999;
								desc = 0;
								def = 1;
							}

							if (ibisDocValues.length > def) {
								extractor.addMethods(folder, ibisBean.getName(), property, ibisDocValues[desc], ibisDocValues[def], method.getDeclaringClass().getSimpleName(), superClasses, javadocLink, order);
							} else {
								extractor.addMethods(folder, ibisBean.getName(), property, ibisDocValues[desc], "", method.getDeclaringClass().getSimpleName(), superClasses, javadocLink, order);
							}
						}
					}
				}
			}
		}
		extractor.addAllFolder();
		extractor.writeToJsonUrl();

		return extractor.getJsonString();
	}

	private String getSchema() throws PipeRunException {
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

		Map<String, TreeSet<IbisBean>> groups = getGroups();
		Set<IbisBean> ibisBeans = getIbisBeans(groups);
		List<IbisMethod> ibisMethods = getIbisMethods(this);

		for (IbisBean ibisBean : ibisBeans) {
			addIbisBeanToSchema(ibisBean, schema, ibisBeans, ibisMethods, groups);
		}
		return schema.toXML(true);
	}

	private static Set<IbisBean> getIbisBeans(Map<String, TreeSet<IbisBean>> groups) {
		Set<IbisBean> ibisBeans = new TreeSet<IbisBean>();
		for (String group : groups.keySet()) {
			ibisBeans.addAll(groups.get(group));
		}
		return ibisBeans;
	}

	private static Set<IbisBean> getIbisBeans() {
		return getIbisBeans(getGroups());
	}

	private static List<IbisMethod> getIbisMethods(IPipe pipe) throws PipeRunException {
		DigesterXmlHandler digesterXmlHandler = new DigesterXmlHandler();
		try {
			XmlUtils.parseXml(digesterXmlHandler, Misc.resourceToString(ClassUtils.getResourceURL(pipe, "digester-rules.xml")));
		} catch (IOException e) {
			throw new PipeRunException(pipe, "Could nog parse digester-rules.xml", e);
		} catch (SAXException e) {
			throw new PipeRunException(pipe, "Could nog parse digester-rules.xml", e);
		}
		return digesterXmlHandler.getIbisMethods();
	}

	private static void addIbisBeanToSchema(IbisBean ibisBean, XmlBuilder schema,
			Set<IbisBean> ibisBeans, List<IbisMethod> ibisMethods, Map<String, TreeSet<IbisBean>> groups) {
		XmlBuilder complexType = new XmlBuilder("complexType", "xs", "http://www.w3.org/2001/XMLSchema");
		complexType.addAttribute("name", ibisBean.getName() + "Type");
		if (ibisBean.getClazz() != null) {
			List<XmlBuilder> choices = new ArrayList<XmlBuilder>();
			final Map<String, Integer> sortWeight;
			if (ibisBean.getName().equals("Adapter")) {
				sortWeight = IbisDocPipe.sortWeightAdapter;
			} else if (ibisBean.getName().equals("Receiver")) {
				sortWeight = IbisDocPipe.sortWeightReceiver;
			} else if (ibisBean.getName().equals("Pipeline")) {
				sortWeight = IbisDocPipe.sortWeightPipeline;
			} else {
				sortWeight = IbisDocPipe.sortWeight;
			}
			Method[] classMethods = ibisBean.getClazz().getMethods();
			Arrays.sort(classMethods, new Comparator<Method>() {
				@Override
				public int compare(Method m1, Method m2) {
					Integer w1 = sortWeight.get(m1.getName());
					Integer w2 = sortWeight.get(m2.getName());
					if (w1 != null || w2 != null) {
						if (w1 == null) w1 = Integer.MIN_VALUE;
						if (w2 == null) w2 = Integer.MIN_VALUE;
						return w2.compareTo(w1);
					}
					return (m1.getName().compareTo(m2.getName()));
				}
			});
			for (Method method : classMethods) {
				IbisMethod ibisMethod = getIbisBeanParameter(method.getName(), ibisMethods);
				if (ibisMethod != null) {
					String childIbisBeanName = toUpperCamelCase(ibisMethod.getParameterName());
					TreeSet<IbisBean> childIbisBeans = groups.get(childIbisBeanName + "s");
					if (childIbisBeans != null) {
						// Pipes, Senders, ...
						if (!ignore(ibisBean, childIbisBeanName)) {
							XmlBuilder choice = new XmlBuilder("choice", "xs", "http://www.w3.org/2001/XMLSchema");
							choice.addAttribute("minOccurs", "0");
							int maxOccursX = ibisMethod.getMaxOccurs();
							if (overwriteMaxOccursToUnbounded.contains(ibisBean.getName())) {
								maxOccursX = -1;
							}
							addMaxOccurs(choice, maxOccursX);
							
							for (IbisBean childIbisBean : childIbisBeans) {
								choice.addSubElement(getChildIbisBeanSchemaElement(childIbisBean.getName(), 1));
							}
							choices.add(choice);
						}
					} else {
						// Param, Forward, ...
						if (childIbisBeanName != null) {
							boolean isExistingIbisBean = false;
							for (IbisBean existingIbisBean : ibisBeans) {
								if (existingIbisBean.getName().equals(childIbisBeanName)) {
									isExistingIbisBean = true;
								}
							}
							if (isExistingIbisBean) {
								int maxOccurs = ibisMethod.getMaxOccurs();
								if (overwriteMaxOccursToOne.contains(ibisMethod.getMethodName())) {
									maxOccurs = 1;
								}
								choices.add(getChildIbisBeanSchemaElement(childIbisBeanName, maxOccurs));
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
		addPropertiesToSchemaOrHtml(ibisBean, complexType, null);
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

	private static void addPropertiesToSchemaOrHtml(IbisBean ibisBean, XmlBuilder beanComplexType,
			StringBuffer beanHtml) {
		Map<String, Method> beanProperties = getBeanProperties(ibisBean.getClazz());
		if (copyPropterties.containsKey(ibisBean.getName())) {
			for (IbisBean ibisBean2 : getIbisBeans()) {
				if (copyPropterties.get(ibisBean.getName()).equals(ibisBean2.getName())) {
					beanProperties.putAll(getBeanProperties(ibisBean2.getClazz()));
				}
			}
		}
		if (beanProperties != null) {
			Iterator<String> iterator = new TreeSet<String>(beanProperties.keySet()).iterator();
			while (iterator.hasNext()) {
				String property = (String)iterator.next();
				boolean exclude = false;
				if (property.equals("name")) {
					for (String filter : excludeNameAttribute) {
						if (ibisBean.getName().endsWith(filter)) {
							exclude = true;
						}
					}
				}
				if (!exclude) {
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
					Method method = beanProperties.get(property);
					if (beanHtml != null) {
						beanHtml.append("<tr>");
						beanHtml.append("<td>" + method.getDeclaringClass().getSimpleName() + "</td>");
						beanHtml.append("<td>" + property + "</td>");
					}
					IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
					if (ibisDoc != null) {
						String[] ibisDocValues = ibisDoc.value();
						if (beanComplexType != null) {
							String ibisDocValue = ibisDocValues[0];
							if (ibisDocValues.length > 1) {
								ibisDocValue = ibisDocValue + " (default: " + ibisDocValues[1] + ")";
							}
							XmlBuilder annotation = new XmlBuilder("annotation");
							XmlBuilder documentation = new XmlBuilder("documentation");
							attribute.addSubElement(annotation);
							annotation.addSubElement(documentation);
							documentation.setValue(ibisDocValue);
						}
						if (beanHtml != null) {
							String ibisDocValue = ibisDocValues[0];
							beanHtml.append("<td>" + ibisDocValue + "</td>");
							if (ibisDocValues.length > 1) {
								ibisDocValue = ibisDocValues[1];
							} else {
								ibisDocValue = "";
							}
							beanHtml.append("<td>" + ibisDocValue + "</td>");
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

	private static IbisMethod getIbisBeanParameter(String ibisMethodName, List<IbisMethod> ibisMethods) {
		for (IbisMethod ibisMethod : ibisMethods) {
			if (ibisMethod.getMethodName().equals(ibisMethodName)) {
				return ibisMethod;
			}
		}
		return null;
	}

	private static String toUpperCamelCase(String beanName) {
		return beanName.substring(0,  1).toUpperCase() + beanName.substring(1);
	}

	private static boolean ignore(IbisBean ibisBean, String childIbisBeanName) {
		boolean ignore = false;
		for (String namePart : ignores.keySet()) {
			if (ibisBean.getName().indexOf(namePart) != -1 && childIbisBeanName.equals(ignores.get(namePart))) {
				ignore = true;
			}
		}
		return ignore;
	}

	private static Map<String, Method> getBeanProperties(Class<?> clazz) {
		Map<String, Method> result = new HashMap<String, Method>();
		getBeanProperties(clazz, "set", result);
		Set<String> remove = new HashSet<String>();
		Map<String, Method> getMethods = new HashMap<String, Method>();
		getBeanProperties(clazz, "get", getMethods);
		getBeanProperties(clazz, "is", getMethods);
		for (String name : result.keySet()) {
			if (!getMethods.containsKey(name) && !result.get(name).isAnnotationPresent(IbisDoc.class)) {
				remove.add(name);
			}
		}
		for (String name : remove) {
			result.remove(name);
		}
		return result;
	}

	private static void getBeanProperties(Class<?> clazz, String verb, Map<String, Method> beanProperties) {
		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Class<?> returnType = methods[i].getReturnType();
			if (returnType == String.class || returnType.isPrimitive()) {
				if (methods[i].getName().startsWith(verb)) {
					if (methods[i].getName().length() > verb.length()) {
						beanProperties.put(methods[i].getName().substring(verb.length(), verb.length() + 1).toLowerCase()
						+ methods[i].getName().substring(verb.length() + 1), methods[i]);
					}
				}
			}
		}
	}

	private static String getUglifyLookup() {
		StringBuffer result = new StringBuffer();
		result.append("<Elements>\n");
		Map<String, TreeSet<IbisBean>> groups = getGroups();
		for (String group : groups.keySet()) {
			for (IbisBean ibisBean : groups.get(group)) {
				String type = "";
				String className = ibisBean.getClazz().getName();
				if (group.equals("Other")) {
					type = ibisBean.getName().substring(0,  1).toLowerCase() + ibisBean.getName().substring(1);
					if (!ibisBean.getName().equals("Receiver")) {
						className = "";
					}
				} else {
					type = group.substring(0,  1).toLowerCase() + group.substring(1, group.length() - 1);
				}
				result.append("  <Element>\n");
				result.append("    <Name>" + ibisBean.getName() + "</Name>\n");
				result.append("    <Type>" + type + "</Type>\n");
				result.append("    <ClassName>" + className + "</ClassName>\n");
				result.append("  </Element>\n");
			}
		}
		result.append("</Elements>\n");
		return result.toString();
	}

	private static void getMenuHtml(StringBuffer topmenuHtml, StringBuffer allHtml, Map<String, String> groupsHtml) {
		if (topmenuHtml == null) topmenuHtml = new StringBuffer();
		if (allHtml == null)  allHtml = new StringBuffer();
		if (groupsHtml == null) groupsHtml = new HashMap<String, String>();
		Map<String, TreeSet<IbisBean>> groups = getGroups();

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
		if (errors.size() > 0) {
			topmenuHtml.append("<a href='errors.html' target='contentFrame'>Errors</a><br/>\n");
		}
	}

	private static String getBeanHtml(String beanName) {
		Map<String, TreeSet<IbisBean>> groups = getGroups();
		for (IbisBean ibisBean : getIbisBeans()) {
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

}

class SpringBean implements Comparable<Object> {
	private String name;
	private Class<?> clazz;
	
	SpringBean(String name, Class<?> clazz) {
		this.name = name;
		this.clazz = clazz;
	}

	public String getName() {
		return name;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public int compareTo(Object o) {
		return name.compareTo(((SpringBean)o).name);
	}

	@Override
	public String toString() {
		return name +  "[" + clazz.getName() + "]";
	}
}

class IbisBean implements Comparable<IbisBean> {
	private String name;
	private Class<?> clazz;

	IbisBean(String name, Class<?> clazz) {
		this.name = name; 
		this.clazz = clazz;
	}

	public String getName() {
		return name;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public int compareTo(IbisBean ibisBean) {
		return name.compareTo((ibisBean).name);
	}

	@Override
	public String toString() {
		return name +  "[" + clazz.getName() + "]";
	}
}

class IbisMethod {
	private String methodName; // E.g. registerAdapter
	private String parameterName; // E.g. adapter
	int maxOccurs = -1;

	IbisMethod(String methodName, String parameterName) {
		this.methodName = methodName;
		this.parameterName = parameterName;
		if (methodName.startsWith("set")) {
			maxOccurs = 1;
		} else if (!(methodName.startsWith("add") || methodName.startsWith("register"))) {
			throw new RuntimeException("Unknow verb in method name: " + methodName);
		}
	}

	public String getMethodName() {
		return methodName;
	}

	public String getParameterName() {
		return parameterName;
	}

	public int getMaxOccurs() {
		return maxOccurs;
	}

	@Override
	public String toString() {
		return methodName +  "(" + parameterName + ")";
	}
}

class DigesterXmlHandler extends DefaultHandler {
	private String currentIbisBeanName;
	private List<IbisMethod> ibisMethods = new ArrayList<IbisMethod>();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("pattern".equals(qName)) {
			String pattern = attributes.getValue("value");
			StringTokenizer tokenizer = new StringTokenizer(pattern, "/");
			while (tokenizer.hasMoreElements()) {
				String token = tokenizer.nextToken();
				if (!"*".equals(token)) {
					currentIbisBeanName = token;
				}
			}
		} else if ("set-next-rule".equals(qName)) {
			String methodName = attributes.getValue("methodname");
			ibisMethods.add(new IbisMethod(methodName, currentIbisBeanName));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("pattern".equals(qName)) {
			currentIbisBeanName = null;
		}
	}
	
	public List<IbisMethod> getIbisMethods() {
		return ibisMethods;
	}

}
