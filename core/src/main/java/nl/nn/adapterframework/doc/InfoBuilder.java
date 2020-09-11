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

import nl.nn.adapterframework.doc.objects.AClass;
import nl.nn.adapterframework.doc.objects.AFolder;
import nl.nn.adapterframework.doc.objects.AMethod;
import nl.nn.adapterframework.doc.objects.BeanProperty;
import nl.nn.adapterframework.doc.objects.DocInfo;
import nl.nn.adapterframework.doc.objects.IbisBean;
import nl.nn.adapterframework.doc.objects.MethodExtra;
import nl.nn.adapterframework.doc.objects.MethodNameToChildIbisBeanNameMapping;
import nl.nn.adapterframework.util.LogUtil;

public class InfoBuilder {
	private static Logger log = LogUtil.getLogger(InfoBuilder.class);
	private static final int MAX_ORDER = 999;

	private DocInfo docInfo;
	private final Map<String, AClass> aClassLookup = new TreeMap<>();

	/**
	 * @return The {@link DocInfo} object that holds all the information that is needed to
	 * build HTML Frank!Doc, the JSON text that is needed by the Angular Frank!Doc webpage,
	 * the Frank configuration schema (currently still called ibisdoc.xsd) and the
	 * syntax 1.0 lookup (currently still called uglify_lookup.xml).
	 *
	 * The correctness of the output can be tested by a group of Larva scenarios in
	 * subproject ibis-adapterframework-test. The scenarios are in directory
	 * {@code /src/test/resources/TestTool/IbisDoc}.
	 * 
	 * @throws Exception
	 */
	public DocInfo build() throws Exception {
		docInfo = new DocInfo();
		docInfo.setExcludeFilters(InfoBuilderSource.excludeFilters);
		docInfo.setIgnores(InfoBuilderSource.ignores);
		docInfo.setGroups(InfoBuilderSource.getGroups());
		docInfo.setIbisBeans(InfoBuilderSource.getIbisBeans(docInfo.getGroups()));
		docInfo.setMethodNameMappings(InfoBuilderSource.getMethodMappings());
		for (IbisBean ibisBean : docInfo.getIbisBeans()) {
			if (ibisBean.getClazz() != null) {
				enrichIbisBeanWithSortedClassMethods(ibisBean);
				for (MethodExtra methodExtra : ibisBean.getSortedClassMethods()) {
	                enrichMethodOfIbisBean(methodExtra, ibisBean);
				}
			}
			enrichIbisBeanWithProperties(ibisBean);
			ibisBean.getProperties().keySet().forEach(
					property -> enrichPropertyOfIbisBean(ibisBean.getProperties().get(property), ibisBean));			
			docInfo.getIbisBeans().add(ibisBean);
		}
		calculateFolderClasses();
		addFolders();
		return docInfo;
	}

	/**
	 * 
	 * @param ibisBean. The {@code clazz} field should not be null.
	 */
	public void enrichIbisBeanWithSortedClassMethods(IbisBean ibisBean) {
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
		Method[] classMethods = new Method[]{};
		try {
			classMethods = ibisBean.getClazz().getMethods();
		} catch (NoClassDefFoundError e) {
			//TODO Why is it trying to resolve (sub) interfaces?
			log.warn("Cannot retrieve methods of [" + ibisBean.getName() + "] due to a NoClassDefFoundError");
		}
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
		MethodExtra[] methodsExtra = new MethodExtra[classMethods.length];
		for(int i = 0; i < classMethods.length; i++) {
			MethodExtra m = new MethodExtra();
			m.setMethod(classMethods[i]);
			methodsExtra[i] = m;
		}
		ibisBean.setSortedClassMethods(methodsExtra);
	}

	private void enrichMethodOfIbisBean(MethodExtra methodExtra, IbisBean ibisBean) {
		MethodNameToChildIbisBeanNameMapping methodNameMapping =
				getMethodNameMapping(methodExtra.getMethod().getName(), docInfo.getMethodNameMappings());
		if (methodNameMapping != null) {
			methodExtra.setChildIbisBeanName(
					InfoBuilderSource.toUpperCamelCase(methodNameMapping.getChildIbisBeanName()));
			methodExtra.setChildIbisBeans(docInfo.getGroups().get(
					methodExtra.getChildIbisBeanName() + "s"));
			if (methodExtra.getChildIbisBeans() != null) {
				// Pipes, Senders, ...
				int maxOccursX = methodNameMapping.getMaxOccurs();
				if (InfoBuilderSource.overwriteMaxOccursToUnbounded.contains(ibisBean.getName())) {
					maxOccursX = -1;
				}
				methodExtra.setMaxOccurs(maxOccursX);
			} else {
				// Param, Forward, ...
				if (methodExtra.getChildIbisBeanName() != null) {
					boolean isExistingIbisBean = false;
					for (IbisBean existingIbisBean : docInfo.getIbisBeans()) {
						if (existingIbisBean.getName().equals(methodExtra.getChildIbisBeanName())) {
							isExistingIbisBean = true;
						}
					}
					methodExtra.setExistingIbisBean(isExistingIbisBean);
					if (isExistingIbisBean) {
						int maxOccurs = methodNameMapping.getMaxOccurs();
						if (InfoBuilderSource.overwriteMaxOccursToOne.contains(methodNameMapping.getMethodName())) {
							maxOccurs = 1;
						}
						methodExtra.setMaxOccurs(maxOccurs);
					}
				}
			}
		}
	}

	private static MethodNameToChildIbisBeanNameMapping getMethodNameMapping(
			String ibisMethodName, List<MethodNameToChildIbisBeanNameMapping> mappings) {
		for (MethodNameToChildIbisBeanNameMapping mapping : mappings) {
			if (mapping.getMethodName().equals(ibisMethodName)) {
				return mapping;
			}
		}
		return null;
	}

	private void enrichIbisBeanWithProperties(final IbisBean ibisBean) {
		Map<String, Method> beanProperties = getBeanPropertiesXSD(ibisBean);
		ibisBean.setProperties(new TreeMap<>());
		for(String property: beanProperties.keySet()) {
			BeanProperty bp = new BeanProperty();
			bp.setName(property);
			bp.setMethod(beanProperties.get(property));
			ibisBean.getProperties().put(property, bp);
		}
	}

	private Map<String, Method> getBeanPropertiesXSD(IbisBean ibisBean) {
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
			FromAnnotations fromAnnotations = parseIbisDocAndIbisDocRef(
					beanProperty.getMethod(), false);
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
			if(isJson || (ibisDoc == null)) {
				String[] orderAndPackageName = ibisDocRef.value();
				String packageName = null;
				if(orderAndPackageName.length == 1) {
					packageName = ibisDocRef.value()[0];
				} else if(orderAndPackageName.length == 2) {
					packageName = ibisDocRef.value()[1];
					hasIbisDocRefWithOrder = true;
					try {
						orderOfIbisDocRef = Integer.parseInt(ibisDocRef.value()[0]);
					} catch(Throwable t) {
						log.warn("Could not parse order in @IbisDocRef annotation: " + Integer.parseInt(ibisDocRef.value()[0]));
					}
				}
				
				// Get the last element of the full package, to check if it is a class or a method
				String classOrMethod = packageName.substring(packageName.lastIndexOf(".") + 1).trim();
				char[] firstLetter = classOrMethod.toCharArray();

				// Check the first letter of the last element (if lower case => method, else class)
				if (Character.isLowerCase(firstLetter[0])) {

					// Get the full class name
					int lastIndexOf = packageName.lastIndexOf(".");
					String fullClassName = packageName.substring(0, lastIndexOf);

					// Get the reference values of the specified method
					ibisDoc = getIbisDocAnnotation(fullClassName, classOrMethod);
					referredClassName = fullClassName.substring(fullClassName.lastIndexOf(".") + 1).trim();
				} else {
					// Get the reference values of this method
					ibisDoc = getIbisDocAnnotation(packageName, method.getName());
					referredClassName = classOrMethod;
				}
				originalClass = referredClassName;
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
			if(hasIbisDocRefWithOrder) {
				order = orderOfIbisDocRef;
			}
			FromAnnotations result = new FromAnnotations();
			result.defaultValue = defaultValue;
			result.description = description;
			result.order = order;
			result.referredClass = referredClassName;
			result.originalClass = originalClass;
			return result;
		}
		else {
			return null;
		}
    }

    /**
     * 
     * @param className
     * @param methodName
     * @return The IbisDoc annotation of the named method on the named class, or null.
     */
    private static IbisDoc getIbisDocAnnotation(String className, String methodName) {
        IbisDoc ibisDoc = null;
        try {
            Class<?> parentClass = Class.forName(className);
            for (Method parentMethod : parentClass.getDeclaredMethods()) {
                if (parentMethod.getName().equals(methodName)) {

                    // Get the IbisDoc values of that method
                    ibisDoc = AnnotationUtils.findAnnotation(parentMethod, IbisDoc.class);
                    break;
                }
            }

        } catch (ClassNotFoundException e) {
            log.warn("Super class [" + e +  "] was not found!");
        }

        return ibisDoc;
    }

    private void calculateFolderClasses() {
    	docInfo.getGroups().values().stream()
    		.flatMap(group -> group.stream())
    		.map(ibisBean -> ibisBean.getClazz())
    		.forEach(this::handleClass);
    	setSuperclasses();
    }

    void handleClass(Class<?> clazz) {
    	if(aClassLookup.containsKey(clazz.getName())) {
    		return;
    	}
		AClass aClass = new AClass();
		aClass.setClazz(clazz);
		aClassLookup.put(clazz.getName(), aClass);

		// Get the javadoc link for the class
		String javadocLink = clazz.getName().replaceAll("\\.", "/");
		aClass.setJavadocLink(javadocLink);
		Map<String, Method> beanProperties = InfoBuilderSource.getBeanPropertiesJson(clazz);
		enrichAClassWithMethods(aClass, beanProperties);
        enrichAClassWithReferredClassName(aClass);
    }


    /**
     * Add the methods to the class object.
     *
     * @param beanProperties - The properties of a class
     * @param newClass       - The class object we have to add the methods to
     */
    private static void enrichAClassWithMethods(AClass newClass, Map<String, Method> beanProperties) {
    	newClass.setMethods(new ArrayList<>());
    	Iterator<String> iterator = new TreeSet<>(beanProperties.keySet()).iterator();
        while (iterator.hasNext()) {
            String property = iterator.next();
    		Method method = beanProperties.get(property);
    		FromAnnotations fromAnnotations = parseIbisDocAndIbisDocRef(method, true);
    		if(fromAnnotations != null) {
    			AMethod aMethod = new AMethod();
    			aMethod.setName(property);
    			Deprecated deprecated = AnnotationUtils.findAnnotation(method, Deprecated.class);
    			boolean isDeprecated = deprecated != null;
    		    aMethod.setOriginalClassName(fromAnnotations.originalClass);
    		    aMethod.setDescription(fromAnnotations.description);
    		    aMethod.setDefaultValue(fromAnnotations.defaultValue);
    		    aMethod.setOrder(fromAnnotations.order);
    		    aMethod.setDeprecated(isDeprecated);
    		    aMethod.setReferredClassName(fromAnnotations.referredClass);
    		    newClass.getMethods().add(aMethod);
    		}
        }
    }

	private static void enrichAClassWithReferredClassName(AClass aClass) {
		Set<String> candidates = aClass.getMethods().stream()
				.map(m -> m.getReferredClassName()).collect(Collectors.toSet());
		candidates.remove("");
		aClass.setReferredClasses(new ArrayList<>(candidates));
	}

    private void setSuperclasses() {
    	new SuperClassesAdder().run(aClassLookup);
    }

    private static class SuperClassesAdder {
    	List<AClass> classesToIterate = new ArrayList<>();
    	Map<String, AClass> aClassLookup = null;

    	void run(Map<String, AClass> aClassLookup) {
    		this.aClassLookup = aClassLookup;
    		classesToIterate.addAll(aClassLookup.values());
    		for(AClass currentIterate: classesToIterate) {
  				handle(currentIterate);
    		}
    	}

    	void handle(AClass toHandle) {
    		if(toHandle.getSuperClassesSimpleNames() != null) {
    			return;
    		}
    		toHandle.setSuperClassesSimpleNames(new ArrayList<>());
			toHandle.getSuperClassesSimpleNames().addAll(toHandle.getReferredClasses());
    		Class<?> superClazz = toHandle.getClazz().getSuperclass();
    		AClass superClass = null;
    		if((superClazz != null) && aClassLookup.containsKey(superClazz.getName())) {
    			superClass = aClassLookup.get(superClazz.getName());
    			handle(superClass);
    			toHandle.getSuperClassesSimpleNames().add(superClass.getClazz().getSimpleName());
    			List<String> remainingSuperClasses = new ArrayList<>(superClass.getSuperClassesSimpleNames());
    			remainingSuperClasses.removeAll(toHandle.getReferredClasses());
    			toHandle.getSuperClassesSimpleNames().addAll(remainingSuperClasses);
    		}
    		else {
    			while(superClazz != null) {
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
    		AFolder newFolder = new AFolder(groupName);
    		for (IbisBean ibisBean : groups.get(groupName)) {
    			if((ibisBean.getClazz() != null)
    					&& (ibisBean.getProperties().size() >= 1)) {
    				newFolder.addClass(aClassLookup.get(ibisBean.getClazz().getName()));
    			}
    		}
    		docInfo.getFolders().add(newFolder);
    	}
    	// Folder "All" is expected to be empty.
    	docInfo.getFolders().add(new AFolder("All"));
    }
}
