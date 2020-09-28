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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.doc.objects.IbisBean;
import nl.nn.adapterframework.doc.objects.ChildIbisBeanMapping;
import nl.nn.adapterframework.doc.objects.SpringBean;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

class InfoBuilderSource {
	private static Logger log = LogUtil.getLogger(InfoBuilderSource.class);

	static Map<String, String> ignores = new HashMap<String, String>();
	static {
		// Adding a sender to a listener has been used in the past but is not (commonly) used anymore
		ignores.put("Listener", "Sender");
	}
	static List<String> excludeNameAttribute = new ArrayList<String>();
	static {
		excludeNameAttribute.add("putValidator");
		excludeNameAttribute.add("putWrapper");
	}
	static List<String> overwriteMaxOccursToOne= new ArrayList<String>();
	static {
		// When registerPipeLine in Adapter has been renamed to setPipeLine this workaround can be removed.
		overwriteMaxOccursToOne.add("registerPipeLine");
	}
	static List<String> overwriteMaxOccursToUnbounded = new ArrayList<String>();
	static {
		// The setSender in ParallelSenders adds senders to a list. When setSender  has been renamed to addSender this
		// workaround can be removed.
		overwriteMaxOccursToUnbounded.add("parallelSendersSender");
	}
	// Influence the order of elements in the XSD, this will override the alphabetic order.
	// Instead of using the Maps below it might be a good idea to use annotations on the specified methods.
	static Map<String, Integer> sortWeightAdapter = new HashMap<String, Integer>();
	static {
		sortWeightAdapter.put("registerReceiver", 100);
	}
	static Map<String, Integer> sortWeightReceiver = new HashMap<String, Integer>();
	static {
		sortWeightReceiver.put("setListener", 100);
		sortWeightReceiver.put("setErrorSender", 90);
		sortWeightReceiver.put("setErrorStorage", 80);
		sortWeightReceiver.put("setMessageLog", 70);
		sortWeightReceiver.put("setSender", 60);
	}
	static Map<String, Integer> sortWeightPipeline = new HashMap<String, Integer>();
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
	static Map<String, Integer> sortWeight = new HashMap<String, Integer>();
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
	static Map<String, String> copyPropterties = new HashMap<String, String>();
	static {
		// FileSender extends FileHandler which FilePipe cannot because it already extends FixedForwardPipe.
		// Might be a good idea to specify this with an annotation.
		copyPropterties.put("FilePipe", "FileSender");
	}

	static Set<String> excludeFilters = new TreeSet<String>();
	static {
		// Exclude classes that will give conflicts with existing, non-compatible bean definition of same name and class
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.esb\\.WsdlGeneratorPipe");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.ifsa\\.IfsaRequesterSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.ifsa\\.IfsaProviderListener");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco2\\.SapSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco2\\.SapListener");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco2\\.SapLUWManager");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco3\\.SapSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco3\\.SapListener");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.jco3\\.SapLUWManager");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.CommandSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.EchoSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.FixedResultSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.LogSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.MailSender");
		excludeFilters.add(".*\\.IbisstoreSummaryQuerySender");
		// Exclude classes that cannot be used directly in configurations
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.MessageSendingPipe");
		
		// Exclude classes that should only be used in internal configurations
		excludeFilters.add("nl\\.nn\\.adapterframework\\.doc\\.IbisDocPipe");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.webcontrol\\..*");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.pipes\\.CreateRestViewPipe");
	}

	// Cache groups for better performance, don't use it directly, use getGroups()
	private static Map<String, TreeSet<IbisBean>> cachedGroups;

	/**
	 * @return The groups to be displayed in the Frank!Doc. Each group contains
	 * the IbisBean objects to be included. These IbisBean objects are only
	 * partially filled. Only the {@code name} and {@code clazz} fields are set.
	 */
	static synchronized Map<String, TreeSet<IbisBean>> getGroups() {
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

	private static void addIbisBeans(String group, Class<?> clazz, Map<String, TreeSet<IbisBean>> groups) {
		addIbisBeans(group, clazz, null, groups);
	}

	private static void addIbisBeans(String group, Class<?> clazz, String nameLastPartToReplaceWithGroupName,
			Map<String, TreeSet<IbisBean>> groups) {
		TreeSet<IbisBean> ibisBeans = new TreeSet<IbisBean>();
		if (clazz != null && clazz.isInterface()) {
			Set<SpringBean> springBeans = getSpringBeans(clazz);
			for (SpringBean springBean : springBeans) {
				if (clazz.isAssignableFrom(springBean.getClazz())) {
					addIbisBean(group, toUpperCamelCase(springBean.getName()), nameLastPartToReplaceWithGroupName,
							springBean.getClazz(), ibisBeans);
				}
			}
		}
		groups.put(group, ibisBeans);
	}

	static String toUpperCamelCase(String beanName) {
		return beanName.substring(0,  1).toUpperCase() + beanName.substring(1);
	}

	private static Set<SpringBean> getSpringBeans(Class<?> interfaze) {
		Set<SpringBean> result = new HashSet<SpringBean>();
		BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(interfaze));
		BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator() {
			@Override
			protected String buildDefaultBeanName(BeanDefinition definition) {
				String beanClassName = definition.getBeanClassName();
				Assert.state(beanClassName != null, "No bean class name set");
				return beanClassName;
			}
		};
		scanner.setBeanNameGenerator(beanNameGenerator);
		for (String excludeFilter : excludeFilters) {
			addExcludeFilter(scanner, excludeFilter);
		}
		boolean success = false;
		int maxTries = 100;
		int tryCount = 0;
		while (!success && tryCount < maxTries) {
			tryCount++;
			try {
				scanner.scan("nl.nn.adapterframework", "nl.nn.ibistesttool");
				success = true;
			} catch(BeanDefinitionStoreException e) {
				// Exclude errors like class java.lang.NoClassDefFoundError: com/tibco/tibjms/admin/TibjmsAdminException
				// for SendTibcoMessage. See menu item Errors in GUI.
				String excludeFilter = e.getMessage();
				excludeFilter = excludeFilter.substring(excludeFilter.indexOf(".jar!/") + 6);
				excludeFilter = excludeFilter.substring(0, excludeFilter.indexOf(".class"));
				excludeFilter = excludeFilter.replaceAll("/", "\\\\.");
				excludeFilter = excludeFilter.substring(0, excludeFilter.lastIndexOf('.') + 1) + ".*";
				excludeFilters.add(excludeFilter);
				addExcludeFilter(scanner, excludeFilter);
				log.warn(excludeFilter + e.getMessage() + ": " + e.getStackTrace());
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

	private static void addIbisBean(String group, String fqBeanName, String nameLastPartToReplaceWithGroupName,
			Class<?> clazz, TreeSet<IbisBean> ibisBeans) {
		int i = fqBeanName.lastIndexOf(".");
		String beanName = fqBeanName;
		if(i != -1) {
			beanName = fqBeanName.substring(i+1);
		}
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

	private static String replaceNameLastPartWithGroupName(String group, String beanName,
			String nameLastPartToReplaceWithGroupName) {
		if (nameLastPartToReplaceWithGroupName != null && beanName.endsWith(nameLastPartToReplaceWithGroupName)) {
			return beanName.substring(0, beanName.lastIndexOf(nameLastPartToReplaceWithGroupName))
				+ group.substring(0, 1).toUpperCase() + group.substring(1, group.length() - 1);
		} else {
			return beanName;
		}
	}

	private static Class<?> getClass(String className) {
		try {
			return Class.forName(className);
		} catch (NoClassDefFoundError e) {
			// This exception happens when you have the proprietary sub-projects of the Frank!Framework.
			// These sub-projects have classes that depend on third-party classes. If such a third-party
			// class is not found, then this exception handler is entered. We ignore the error because
			// we do have the class in the proprietary FF! subproject.
			log.warn("Ignoring NoClassDefFoundError, assuming it is about a third party class", e);
			return null;
		} catch(ClassNotFoundException e) {
			// This handler is entered when we really do not have the class.
			throw new RuntimeException("Class not found", e);
		}
	}

	/**
	 * @param groups The Map holding the groups with their IbisBean objects.
	 * 
	 * @return The set of all IbisBean objects in all groups. Please note
	 * that {@link InfoBuilderSource#getGroups} fills the IbisBean objects partially. Only
	 * the {@code name} and {@code clazz} fields are set by that method.
	 */
	static Set<IbisBean> getIbisBeans(Map<String, TreeSet<IbisBean>> groups) {
		Set<IbisBean> ibisBeans = new TreeSet<IbisBean>();
		for (String group : groups.keySet()) {
			ibisBeans.addAll(groups.get(group));
		}
		return ibisBeans;
	}

	static List<ChildIbisBeanMapping> getChildIbisBeanMappings() throws IOException, SAXException {
		DigesterRulesParser digesterRulesParser = new DigesterRulesParser();
		try {
			XmlUtils.parseXml(Misc.resourceToString(ClassUtils.getResourceURL(IbisDocPipe.class, "digester-rules.xml")), digesterRulesParser);
		} catch (Exception e) {
			log.error("Could nog parse digester-rules.xml: " + e.getStackTrace());
			throw e;
		}
		return digesterRulesParser.getChildIbisBeanMappings();
	}

	static Map<String, Method> getBeanPropertiesJson(Class<?> clazz) {
		Map<String, Method> result = new HashMap<String, Method>();
		getBeanProperties(clazz, "set", result);
		Set<String> remove = new HashSet<String>();
		Map<String, Method> getMethods = new HashMap<String, Method>();
		getBeanProperties(clazz, "get", getMethods);
		getBeanProperties(clazz, "is", getMethods);
		for (String name : result.keySet()) {
			if (!getMethods.containsKey(name) && !result.get(name).isAnnotationPresent(IbisDoc.class) && !result.get(name).isAnnotationPresent(IbisDocRef.class)) {
				remove.add(name);
			}
		}
		for (String name : remove) {
			result.remove(name);
		}
		return result;
	}

	private static void getBeanProperties(Class<?> clazz, String verb, Map<String, Method> beanProperties) {
		try {
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
		} catch (NoClassDefFoundError e) {
			//TODO fix this, why are all (sub)interfaces also instantiated?
			//Ignore classes that cannot be found...
		}
	}

}
