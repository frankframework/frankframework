/* 
Copyright 2021 WeAreFrank! 

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

package nl.nn.adapterframework.frankdoc.doclet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

import nl.nn.adapterframework.util.LogUtil;

class FrankClassReflect implements FrankClass {
	private static Logger log = LogUtil.getLogger(FrankClassReflect.class);

	private final Class<?> clazz;
	private final Map<String, FrankAnnotation> annotations;

	private static Set<String> excludeFilters = new TreeSet<String>();
	static {
		// Exclude classes that will give conflicts with existing, non-compatible bean definition of same name and class
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.esb\\.WsdlGeneratorPipe");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.SapSender");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.SapListener");
		excludeFilters.add("nl\\.nn\\.adapterframework\\.extensions\\.sap\\.SapLUWManager");
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

	FrankClassReflect(Class<?> clazz) {
		this.clazz = clazz;
		Annotation[] reflectAnnotations = clazz.getAnnotations();
		annotations = new HashMap<>();
		for(Annotation r: reflectAnnotations) {
			FrankAnnotation frankAnnotation = new FrankAnnotationReflect(r);
			annotations.put(frankAnnotation.getName(), frankAnnotation);
		}
	}

	@Override
	public String getName() {
		return clazz.getName();
	}

	@Override
	public String getSimpleName() {
		return clazz.getSimpleName();
	}

	@Override
	public FrankClass getSuperclass() {
		Class<?> superClazz = clazz.getSuperclass();
		if(superClazz == null) {
			return null;
		} else {
			return new FrankClassReflect(superClazz);
		}
	}

	@Override
	public FrankClass[] getInterfaces() throws FrankDocException {
		if(! isInterface()) {
			throw new FrankDocException(String.format("Class [%s] is not an interfaces, and hence method isInterfaces is not supported", getName()), null);
		}
		Class<?>[] interfazes = clazz.getInterfaces();
		FrankClass[] result = new FrankClass[interfazes.length];
		for(int i = 0; i < interfazes.length; ++i) {
			result[i] = new FrankClassReflect(interfazes[i]);
		}
		return result;
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract(clazz.getModifiers());
	}

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(clazz.getModifiers());		
	}

	@Override
	public boolean isInterface() {
		return clazz.isInterface();
	}

	@Override
	public List<FrankClass> getInterfaceImplementations() throws FrankDocException {
		List<SpringBean> springBeans;
		try {
			springBeans = getSpringBeans(clazz.getName());
		} catch(ReflectiveOperationException e) {
			throw new FrankDocException(String.format("Could not get interface implementations of Java class [%s]", getName()), e);
		}
		// We sort here to make the order deterministic.
		Collections.sort(springBeans);
		return springBeans.stream()
				.map(SpringBean::getClazz)
				.map(FrankClassReflect::new)
				.collect(Collectors.toList());
	}

	/**
	 * @param interfaceName The interface for which we want SpringBean objects.
	 * @return All classes implementing interfaceName, ordered by their full class name.
	 */
	private static List<SpringBean> getSpringBeans(final String interfaceName) throws ReflectiveOperationException {
		Class<?> interfaze = getClass(interfaceName);
		if(interfaze == null) {
			throw new ReflectiveOperationException("Class or interface is not available on the classpath: " + interfaceName);
		}
		if(!interfaze.isInterface()) {
			throw new ReflectiveOperationException("This exists on the classpath but is not an interface: " + interfaceName);
		}
		Set<SpringBean> unfiltered = getSpringBeans(interfaze);
		List<SpringBean> result = new ArrayList<SpringBean>();
		for(SpringBean b: unfiltered) {
			if(interfaze.isAssignableFrom(b.getClazz())) {
				result.add(b);
			}
		}
		return result;
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
				if(log.isWarnEnabled()) {
					log.warn(excludeFilter, e);
				}
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

	@Override
	public FrankAnnotation[] getAnnotations() {
		List<FrankAnnotation> annotationList = new ArrayList<>(annotations.values());
		FrankAnnotation[] result = new FrankAnnotation[annotationList.size()];
		for(int i = 0; i < annotationList.size(); ++i) {
			result[i] = (FrankAnnotation) annotationList.get(i);
		}
		return result;
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		return annotations.get(name);
	}

	@Override
	public FrankMethod[] getDeclaredMethods() {
		Method[] rawDeclaredMethods = clazz.getDeclaredMethods();
		return wrapReflectMethodsInArray(rawDeclaredMethods);
	}

	private FrankMethod[] wrapReflectMethodsInArray(Method[] rawDeclaredMethods) {
		FrankMethod[] result = new FrankMethod[rawDeclaredMethods.length];
		for(int i = 0; i < rawDeclaredMethods.length; ++i) {
			result[i] = new FrankMethodReflect(rawDeclaredMethods[i], new FrankClassReflect(rawDeclaredMethods[i].getDeclaringClass()));
		}
		return result;
	}

	@Override
	public FrankMethod[] getDeclaredAndInheritedMethods() {
		Method[] rawDeclaredMethods = clazz.getMethods();
		return wrapReflectMethodsInArray(rawDeclaredMethods);		
	}

	@Override
	public boolean isEnum() {
		return clazz.isEnum();
	}

	@Override
	public String[] getEnumConstants() {
		@SuppressWarnings("unchecked")
		Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) clazz;
		Enum<?>[] enumConstants = enumClass.getEnumConstants();
		String[] result = new String[enumConstants.length];
		for(int i = 0; i < enumConstants.length; ++i) {
			result[i] = enumConstants[i].name();
		}
		return result;
	}
}
