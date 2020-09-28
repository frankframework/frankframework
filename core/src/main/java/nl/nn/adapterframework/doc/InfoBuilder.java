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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;

import nl.nn.adapterframework.doc.objects.ClassJson;
import nl.nn.adapterframework.doc.objects.FolderJson;
import nl.nn.adapterframework.doc.objects.MethodJson;
import nl.nn.adapterframework.doc.objects.BeanProperty;
import nl.nn.adapterframework.doc.objects.DocInfo;
import nl.nn.adapterframework.doc.objects.IbisBean;
import nl.nn.adapterframework.doc.objects.MethodXsd;
import nl.nn.adapterframework.doc.objects.ChildIbisBeanMapping;
import nl.nn.adapterframework.util.LogUtil;

public class InfoBuilder {
	private static Logger log = LogUtil.getLogger(InfoBuilder.class);
	private static final int MAX_ORDER = 999;

	private DocInfo docInfo;
	private final Map<String, ClassJson> classJsonLookup = new TreeMap<>();

	/**
	 * @return The {@link DocInfo} object that holds all the information that is
	 *         needed to build HTML Frank!Doc, the JSON text that is needed by the
	 *         Angular Frank!Doc webpage, the Frank configuration schema (currently
	 *         still called ibisdoc.xsd) and the syntax 1.0 lookup (currently still
	 *         called uglify_lookup.xml).
	 *
	 *         The correctness of the output can be tested by a group of Larva
	 *         scenarios in subproject ibis-adapterframework-test. The scenarios are
	 *         in directory {@code /src/test/resources/TestTool/IbisDoc}.
	 * 
	 * @throws Exception
	 */
	public DocInfo build() throws Exception {
		docInfo = new DocInfo();
		docInfo.setExcludeFilters(InfoBuilderSource.excludeFilters);
		docInfo.setIgnores(InfoBuilderSource.ignores);
		docInfo.setGroups(InfoBuilderSource.getGroups());
		docInfo.setIbisBeans(InfoBuilderSource.getIbisBeans(docInfo.getGroups()));
		docInfo.setChildIbisBeanMappings(InfoBuilderSource.getChildIbisBeanMappings());
		for (IbisBean ibisBean : docInfo.getIbisBeans()) {
			if (ibisBean.getClazz() != null) {
				enrichIbisBeanWithSortedMethods(ibisBean);
				for (MethodXsd methodXsd : ibisBean.getSortedMethodsXsd()) {
					enrichMethodOfIbisBean(methodXsd, ibisBean);
				}
			}
			enrichIbisBeanWithProperties(ibisBean);
			ibisBean.getProperties().keySet()
					.forEach(property -> enrichPropertyOfIbisBean(ibisBean.getProperties().get(property), ibisBean));
			docInfo.getIbisBeans().add(ibisBean);
		}
		calculateFolderClasses();
		addFolders();
		return docInfo;
	}

	/**
	 * 
	 * @param ibisBean The {@code clazz} field should not be null.
	 */
	public void enrichIbisBeanWithSortedMethods(IbisBean ibisBean) {
		final Map<String, Integer> sortWeight;
		if (ibisBean.getName().equals("Adapter")) {
			sortWeight = InfoBuilderSource.sortWeightAdapter;
		} else if (ibisBean.getName().equals("Receiver")) {
			sortWeight = InfoBuilderSource.sortWeightReceiver;
		} else if (ibisBean.getName().equals("Pipeline")) {
			sortWeight = InfoBuilderSource.sortWeightPipeline;
		} else {
			sortWeight = InfoBuilderSource.sortWeight;
		}
		Method[] classMethods = new Method[] {};
		try {
			classMethods = ibisBean.getClazz().getMethods();
		} catch (NoClassDefFoundError e) {
			// TODO Why is it trying to resolve (sub) interfaces?
			log.warn("Cannot retrieve methods of [" + ibisBean.getName() + "] due to a NoClassDefFoundError");
		}
		Arrays.sort(classMethods, new Comparator<Method>() {
			@Override
			public int compare(Method m1, Method m2) {
				Integer w1 = sortWeight.get(m1.getName());
				Integer w2 = sortWeight.get(m2.getName());
				if (w1 != null || w2 != null) {
					if (w1 == null)
						w1 = Integer.MIN_VALUE;
					if (w2 == null)
						w2 = Integer.MIN_VALUE;
					return w2.compareTo(w1);
				}
				return (m1.getName().compareTo(m2.getName()));
			}
		});
		MethodXsd[] methodsXsd = new MethodXsd[classMethods.length];
		for (int i = 0; i < classMethods.length; i++) {
			MethodXsd methodXsd = new MethodXsd();
			methodXsd.setMethod(classMethods[i]);
			methodsXsd[i] = methodXsd;
		}
		ibisBean.setSortedMethodsXsd(methodsXsd);
	}

	private void enrichMethodOfIbisBean(MethodXsd methodXsd, IbisBean ibisBean) {
		ChildIbisBeanMapping childIbisBeanMapping = getChildIbisBeanMapping(
				methodXsd.getMethod().getName(), docInfo.getChildIbisBeanMappings());
		if (childIbisBeanMapping != null) {
			methodXsd.setChildIbisBeanName(
					InfoBuilderSource.toUpperCamelCase(childIbisBeanMapping.getChildIbisBeanName()));
			methodXsd.setChildIbisBeans(docInfo.getGroups().get(methodXsd.getChildIbisBeanName() + "s"));
			if (methodXsd.getChildIbisBeans() != null) {
				// Pipes, Senders, ...
				int maxOccursX = childIbisBeanMapping.getMaxOccurs();
				if (InfoBuilderSource.overwriteMaxOccursToUnbounded.contains(ibisBean.getName())) {
					maxOccursX = -1;
				}
				methodXsd.setMaxOccurs(maxOccursX);
			} else {
				// Param, Forward, ...
				if (methodXsd.getChildIbisBeanName() != null) {
					boolean isExistingIbisBean = false;
					for (IbisBean existingIbisBean : docInfo.getIbisBeans()) {
						if (existingIbisBean.getName().equals(methodXsd.getChildIbisBeanName())) {
							isExistingIbisBean = true;
						}
					}
					methodXsd.setExistingIbisBean(isExistingIbisBean);
					if (isExistingIbisBean) {
						int maxOccurs = childIbisBeanMapping.getMaxOccurs();
						if (InfoBuilderSource.overwriteMaxOccursToOne.contains(childIbisBeanMapping.getMethodName())) {
							maxOccurs = 1;
						}
						methodXsd.setMaxOccurs(maxOccurs);
					}
				}
			}
		}
	}

	private static ChildIbisBeanMapping getChildIbisBeanMapping(String ibisMethodName,
			List<ChildIbisBeanMapping> mappings) {
		for (ChildIbisBeanMapping mapping : mappings) {
			if (mapping.getMethodName().equals(ibisMethodName)) {
				return mapping;
			}
		}
		return null;
	}

	private void enrichIbisBeanWithProperties(final IbisBean ibisBean) {
		Map<String, Method> beanProperties = getBeanPropertiesXsd(ibisBean);
		ibisBean.setProperties(new TreeMap<>());
		for (String property : beanProperties.keySet()) {
			BeanProperty bp = new BeanProperty();
			bp.setName(property);
			bp.setMethod(beanProperties.get(property));
			ibisBean.getProperties().put(property, bp);
		}
	}

	private Map<String, Method> getBeanPropertiesXsd(IbisBean ibisBean) {
		Map<String, Method> beanProperties = InfoBuilderSource.getBeanPropertiesJson(ibisBean.getClazz());
		String name = ibisBean.getName();
		if (InfoBuilderSource.copyPropterties.containsKey(name)) {
			for (IbisBean copySource : docInfo.getIbisBeans()) {
				if (InfoBuilderSource.copyPropterties.get(name).equals(copySource.getName())) {
					beanProperties.putAll(InfoBuilderSource.getBeanPropertiesJson(copySource.getClazz()));
				}
			}
		}
		return beanProperties;
	}

	private void enrichPropertyOfIbisBean(BeanProperty beanProperty, IbisBean ibisBean) {
		boolean exclude = false;
		if (beanProperty.getName().equals("name")) {
			for (String filter : InfoBuilderSource.excludeNameAttribute) {
				if (ibisBean.getName().endsWith(filter)) {
					exclude = true;
				}
			}
		}
		beanProperty.setExcluded(exclude);
		if (!exclude) {
			FromAnnotations fromAnnotations = parseIbisDocAndIbisDocRef(beanProperty.getMethod(), false);
			if (fromAnnotations != null) {
				beanProperty.setHasDocumentation(true);
				beanProperty.setOrder(fromAnnotations.order);
				beanProperty.setDescription(fromAnnotations.description);
				beanProperty.setDefaultValue(fromAnnotations.defaultValue);
			}
		}
	}

	private static class FromAnnotations {
		String referredClass;
		String originalClass;
		int order;
		String description;
		String defaultValue;
	}

	private static FromAnnotations parseIbisDocAndIbisDocRef(Method method, boolean isJson) {
		String referredClassName = "";
		IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
		IbisDocRef ibisDocRef = AnnotationUtils.findAnnotation(method, IbisDocRef.class);
		String originalClass = method.getDeclaringClass().getSimpleName();
		boolean hasIbisDocRefWithOrder = false;
		int orderOfIbisDocRef = 0;
		if (ibisDocRef != null) {
			if (isJson || (ibisDoc == null)) {
				String[] orderAndPackageName = ibisDocRef.value();
				String packageName = null;
				if (orderAndPackageName.length == 1) {
					packageName = ibisDocRef.value()[0];
				} else if (orderAndPackageName.length == 2) {
					packageName = ibisDocRef.value()[1];
					hasIbisDocRefWithOrder = true;
					try {
						orderOfIbisDocRef = Integer.parseInt(ibisDocRef.value()[0]);
					} catch (Throwable t) {
						log.warn("Could not parse order in @IbisDocRef annotation: "
								+ Integer.parseInt(ibisDocRef.value()[0]));
					}
				}

				// Get the last element of the full package, to check if it is a class or a
				// method
				String classOrMethod = packageName.substring(packageName.lastIndexOf(".") + 1).trim();
				char[] firstLetter = classOrMethod.toCharArray();

				// Check the first letter of the last element (if lower case => method, else
				// class)
				Method parentMethod = null;
				if (Character.isLowerCase(firstLetter[0])) {

					// Get the full class name
					int lastIndexOf = packageName.lastIndexOf(".");
					String fullClassName = packageName.substring(0, lastIndexOf);
					parentMethod = getParentMethod(fullClassName, classOrMethod);
				} else {
					parentMethod = getParentMethod(packageName, method.getName());
				}
				if (parentMethod != null) {
					ibisDoc = AnnotationUtils.findAnnotation(parentMethod, IbisDoc.class);
					referredClassName = parentMethod.getDeclaringClass().getSimpleName();
					originalClass = referredClassName;
				}
			}
		}
		if (ibisDoc != null) {
			String[] ibisDocValues = ibisDoc.value();
			// TODO order output based on class inheritance and order value
			int order = MAX_ORDER;
			String description;
			String defaultValue = "";
			try {
				order = Integer.parseInt(ibisDocValues[0]);
			} catch (NumberFormatException e) {
				log.warn("Could not parse order in @IbisDoc annotation: " + ibisDocValues[0]);
			}
			if (order == MAX_ORDER) {
				description = ibisDocValues[0];
				if (ibisDocValues.length > 1) {
					defaultValue = ibisDocValues[1];
				}
			} else {
				description = ibisDocValues[1];
				if (ibisDocValues.length > 2) {
					defaultValue = ibisDocValues[2];
				}
			}
			if (hasIbisDocRefWithOrder) {
				order = orderOfIbisDocRef;
			}
			FromAnnotations result = new FromAnnotations();
			result.defaultValue = defaultValue;
			result.description = description;
			result.order = order;
			result.referredClass = referredClassName;
			result.originalClass = originalClass;
			return result;
		} else {
			return null;
		}
	}

	private static Method getParentMethod(String className, String methodName) {
		try {
			Class<?> parentClass = Class.forName(className);
			for (Method parentMethod : parentClass.getMethods()) {
				if (parentMethod.getName().equals(methodName)) {
					return parentMethod;
				}
			}
			return null;
		} catch (ClassNotFoundException e) {
			log.warn("Super class [" + e + "] was not found!");
			return null;
		}
	}

	private void calculateFolderClasses() {
		docInfo.getGroups().values().stream().flatMap(group -> group.stream()).map(ibisBean -> ibisBean.getClazz())
				.forEach(this::handleClassForFolders);
		setSuperclasses();
	}

	void handleClassForFolders(Class<?> clazz) {
		if (classJsonLookup.containsKey(clazz.getName())) {
			return;
		}
		ClassJson classJson = new ClassJson();
		classJson.setClazz(clazz);
		classJsonLookup.put(clazz.getName(), classJson);

		String javadocLink = clazz.getName().replaceAll("\\.", "/");
		classJson.setJavadocLink(javadocLink);
		Map<String, Method> beanProperties = InfoBuilderSource.getBeanPropertiesJson(clazz);
		enrichClassJsonWithMethods(classJson, beanProperties);
		enrichClassJsonWithReferredClassName(classJson);
	}

	private static void enrichClassJsonWithMethods(ClassJson classJson, Map<String, Method> beanProperties) {
		classJson.setMethods(new ArrayList<>());
		Iterator<String> iterator = new TreeSet<>(beanProperties.keySet()).iterator();
		while (iterator.hasNext()) {
			String property = iterator.next();
			Method method = beanProperties.get(property);
			FromAnnotations fromAnnotations = parseIbisDocAndIbisDocRef(method, true);
			if (fromAnnotations != null) {
				MethodJson methodJson = new MethodJson();
				methodJson.setName(property);
				Deprecated deprecated = AnnotationUtils.findAnnotation(method, Deprecated.class);
				boolean isDeprecated = deprecated != null;
				methodJson.setOriginalClassName(fromAnnotations.originalClass);
				methodJson.setDescription(fromAnnotations.description);
				methodJson.setDefaultValue(fromAnnotations.defaultValue);
				methodJson.setOrder(fromAnnotations.order);
				methodJson.setDeprecated(isDeprecated);
				methodJson.setReferredClassName(fromAnnotations.referredClass);
				classJson.getMethods().add(methodJson);
			}
		}
	}

	private static void enrichClassJsonWithReferredClassName(ClassJson classJson) {
		Set<String> candidates = classJson.getMethods().stream().map(m -> m.getReferredClassName())
				.collect(Collectors.toSet());
		candidates.remove("");
		classJson.setReferredClasses(new ArrayList<>(candidates));
	}

	private void setSuperclasses() {
		new SuperClassesAdder().run();
	}

	/**
	 * TODO: This algorithm has a flaw, but it works by coincidence. This applies to
	 * getting the superclasses of BlobLineIteratingPipe, ClobLineIteratingPipe or
	 * ResultSetIteratingPipe. All three inherit from JdbcIteratingPipeBase. That
	 * pipe has IbisDocRef annotations pointing to FixedQuerySender. That class has
	 * superclasses JdbcQuerySenderBase further up JdbcFacade. This algorithm does not
	 * guarentee that FixedQuerySender and its superclasses are given in the
	 * correct order.
	 * 
	 * @author martijn
	 *
	 */
	private class SuperClassesAdder {
		List<ClassJson> classesToIterate = new ArrayList<>();

		void run() {
			classesToIterate.addAll(classJsonLookup.values());
			for (ClassJson currentIterate : classesToIterate) {
				handle(currentIterate);
			}
		}

		void handle(ClassJson toHandle) {
			if (toHandle.getSuperClassesSimpleNames() != null) {
				return;
			}
			toHandle.setSuperClassesSimpleNames(new ArrayList<>());
			toHandle.getSuperClassesSimpleNames().addAll(toHandle.getReferredClasses());
			Class<?> superClazz = toHandle.getClazz().getSuperclass();
			ClassJson superClass = null;
			if ((superClazz != null) && classJsonLookup.containsKey(superClazz.getName())) {
				superClass = classJsonLookup.get(superClazz.getName());
				handle(superClass);
				toHandle.getSuperClassesSimpleNames().add(superClass.getClazz().getSimpleName());
				List<String> remainingSuperClasses = new ArrayList<>(superClass.getSuperClassesSimpleNames());
				remainingSuperClasses.removeAll(toHandle.getReferredClasses());
				toHandle.getSuperClassesSimpleNames().addAll(remainingSuperClasses);
			} else {
				while (superClazz != null) {
					toHandle.getSuperClassesSimpleNames().add(superClazz.getSimpleName());
					superClazz = superClazz.getSuperclass();
				}
			}
		}
	}

	private void addFolders() {
		Map<String, TreeSet<IbisBean>> groups = docInfo.getGroups();
		docInfo.setFolders(new ArrayList<>());
		for (String groupName : groups.keySet()) {
			FolderJson folderJson = new FolderJson(groupName);
			for (IbisBean ibisBean : groups.get(groupName)) {
				if ((ibisBean.getClazz() != null) && (ibisBean.getProperties().size() >= 1)) {
					folderJson.addClass(classJsonLookup.get(ibisBean.getClazz().getName()));
				}
			}
			docInfo.getFolders().add(folderJson);
		}
		// Folder "All" is expected to be empty.
		docInfo.getFolders().add(new FolderJson("All"));
	}
}
