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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Tag;

import nl.nn.adapterframework.util.LogUtil;

class FrankClassDoclet implements FrankClass {
	private static Logger log = LogUtil.getLogger(FrankClassDoclet.class);

	private final FrankClassRepository repository;
	private final ClassDoc clazz;
	private final Set<String> childClassNames = new HashSet<>();
	private final Map<String, FrankClass> interfaceImplementationsByName = new HashMap<>();
	private final LinkedHashMap<MethodDoc, FrankMethod> frankMethodsByDocletMethod = new LinkedHashMap<>();
	private final Map<String, FrankMethodDoclet> methodsBySignature = new HashMap<>();
	private final Map<String, FrankAnnotation> frankAnnotationsByName;

	FrankClassDoclet(ClassDoc clazz, FrankClassRepository repository) {
		this.repository = repository;
		this.clazz = clazz;
		for(MethodDoc methodDoc: clazz.methods()) {
			FrankMethodDoclet frankMethod = new FrankMethodDoclet(methodDoc, this);
			frankMethodsByDocletMethod.put(methodDoc, frankMethod);
			methodsBySignature.put(frankMethod.getSignature(), frankMethod);
		}
		AnnotationDesc[] annotationDescs = clazz.annotations();
		frankAnnotationsByName = FrankDocletUtils.getFrankAnnotationsByName(annotationDescs);
	}

	void addChild(String className) {
		childClassNames.add(className);
	}

	void recursivelyAddInterfaceImplementation(FrankClassDoclet implementation) throws FrankDocException {
		if(((FrankClassRepositoryDoclet) repository).classIsAllowedAsInterfaceImplementation(implementation)) {
			log.trace("Interface {} is implemented by {}", () -> getName(), () -> implementation.getName());
			// TODO: Test that children of omitted classes can be accepted again.
			interfaceImplementationsByName.put(implementation.getName(), implementation);
		} else {
			log.trace("From interface {} omitted implementation because of filtering {}", () -> getName(), () -> implementation.getName());			
		}
		for(String implementationChildClassName: implementation.childClassNames) {
			FrankClassDoclet implementationChild = (FrankClassDoclet) repository.findClass(implementationChildClassName);
			recursivelyAddInterfaceImplementation(implementationChild);
		}
	}

	@Override
	public boolean isEnum() {
		return clazz.isEnum();
	}

	@Override
	public String getName() {
		return clazz.qualifiedName();
	}

	@Override
	public FrankAnnotation[] getAnnotations() {
		List<FrankAnnotation> values = new ArrayList<>(frankAnnotationsByName.values());
		return values.toArray(new FrankAnnotation[] {});
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		return frankAnnotationsByName.get(name);
	}

	@Override
	public String getSimpleName() {
		String result = clazz.name();
		// For inner classes, we now have result == <outerClassName>.<innerClassName>
		// We want only <innerClassName>
		if(result.contains(".")) {
			result = result.substring(result.lastIndexOf(".") + 1);
		}
		return result;
	}

	@Override
	public String getPackageName() {
		return clazz.containingPackage().name();
	}

	@Override
	public FrankClass getSuperclass() {
		FrankClass result = null;
		ClassDoc superClazz = clazz.superclass();
		if(superClazz != null) {
			try {
				String superclassQualifiedName = superClazz.qualifiedName();
				boolean omit = ((FrankClassRepositoryDoclet) repository).getExcludeFiltersForSuperclass().stream().anyMatch(
						exclude -> superclassQualifiedName.startsWith(exclude));
				if(omit) {
					return null;
				}
				result = repository.findClass(superclassQualifiedName);
			} catch(FrankDocException e) {
				log.warn("Could not get superclass of {}", getName(), e);
			}
		}
		return result;
	}

	@Override
	public FrankClass[] getInterfaces() {
		List<FrankClass> resultList = getInterfacesAsList();
		return resultList.toArray(new FrankClass[] {});
	}

	List<FrankClass> getInterfacesAsList() {
		ClassDoc[] interfaceDocs = clazz.interfaces();
		List<FrankClass> resultList = new ArrayList<>();
		for(ClassDoc interfaceDoc: interfaceDocs) {
			try {
				FrankClass interfaze = repository.findClass(interfaceDoc.qualifiedName());
				if(interfaze != null) {
					resultList.add(interfaze);
				}
			} catch(FrankDocException e) {
				log.warn("Error searching for {}", interfaceDoc.name(), e);
			}
		}
		return resultList;
	}

	@Override
	public boolean isAbstract() {
		return clazz.isAbstract();
	}

	@Override
	public boolean isInterface() {
		return clazz.isInterface();
	}

	@Override
	public boolean isPublic() {
		return clazz.isPublic();
	}

	@Override
	public List<FrankClass> getInterfaceImplementations() throws FrankDocException {
		if(! isInterface()) {
			throw new FrankDocException(String.format("Cannot get implementations of non-interface [%s]", getName()), null);
		}
		return interfaceImplementationsByName.values().stream()
				// Remove abstract classes to make it the same as reflection does it.
				.filter(c -> ! c.isAbstract())
				.collect(Collectors.toList());
	}

	@Override
	public FrankMethod[] getDeclaredMethods() {
		List<FrankMethod> resultList = new ArrayList<>(frankMethodsByDocletMethod.values());
		return resultList.toArray(new FrankMethod[] {});
	}

	@Override
	public FrankMethod[] getDeclaredAndInheritedMethods() {
		List<FrankMethod> resultList = getDeclaredAndInheritedMethodsAsMap().values().stream()
				.filter(FrankMethod::isPublic)				
				.collect(Collectors.toList());
		FrankMethod[] result = new FrankMethod[resultList.size()];
		for(int i = 0; i < resultList.size(); ++i) {
			result[i] = resultList.get(i);
		}
		return result;
	}

	private Map<MethodDoc, FrankMethod> getDeclaredAndInheritedMethodsAsMap() {
		final Map<MethodDoc, FrankMethod> result = new HashMap<>();
		if(getSuperclass() != null) {
			result.putAll(((FrankClassDoclet) getSuperclass()).getDeclaredAndInheritedMethodsAsMap());
		}
		List<FrankMethod> declaredMethodList = Arrays.asList(getDeclaredMethods());
		for(FrankMethod declaredMethod: declaredMethodList) {
			((FrankMethodDoclet) declaredMethod).removeOverriddenFrom(result);
		}
		declaredMethodList.forEach(dm -> ((FrankMethodDoclet) dm).addToRepository(result));
		return result;
	}

	@Override
	public FrankEnumConstant[] getEnumConstants() {
		FieldDoc[] fieldDocs = clazz.enumConstants();
		FrankEnumConstant[] result = new FrankEnumConstant[fieldDocs.length];
		for(int i = 0; i < fieldDocs.length; ++i) {
			result[i] = new FrankEnumConstantDoclet(fieldDocs[i]);
		}
		return result;
	}

	FrankClassRepositoryDoclet getRepository() {
		return (FrankClassRepositoryDoclet) repository;
	}

	FrankMethod recursivelyFindFrankMethod(MethodDoc methodDoc) {
		if(frankMethodsByDocletMethod.containsKey(methodDoc)) {
			return frankMethodsByDocletMethod.get(methodDoc);
		} else if(getSuperclass() != null) {
			return ((FrankClassDoclet) getSuperclass()).recursivelyFindFrankMethod(methodDoc);
		} else {
			return null;
		}
	}

	FrankMethodDoclet getMethodFromSignature(String signature) {
		return methodsBySignature.get(signature);
	}

	<T> T getMethodItemFromSignature(String methodSignature, Function<FrankMethodDoclet, T> getter) {
		FrankMethodDoclet frankMethod = getMethodFromSignature(methodSignature);
		if(frankMethod != null) {
			T result = getter.apply(frankMethod);
			if(result != null) {
				return result;
			}
		}
		return null;
	}

	boolean isTopLevel() {
		return clazz.containingClass() == null;
	}

	@Override
	public String getJavaDoc() {
		return clazz.commentText();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public FrankAnnotation getAnnotationIncludingInherited(String annotationFullName) throws FrankDocException {
		FrankAnnotation result = getAnnotationExcludingImplementedInterfaces(annotationFullName);
		if(result == null) {
			result = getAnnotationFromImplementedInterfaces(annotationFullName);
		}
		return result;
	}

	private FrankAnnotation getAnnotationExcludingImplementedInterfaces(String annotationFullName) throws FrankDocException {
		FrankAnnotation result = getAnnotation(annotationFullName);
		if((result == null) && (getSuperclass() != null)) {
			result = ((FrankClassDoclet) getSuperclass()).getAnnotationExcludingImplementedInterfaces(annotationFullName);
		}
		return result;
	}

	private FrankAnnotation getAnnotationFromImplementedInterfaces(String annotationFullName) throws FrankDocException {
		TransitiveImplementedInterfaceBrowser<FrankAnnotation> browser = new TransitiveImplementedInterfaceBrowser<>(this);
		FrankAnnotation result = browser.search(c -> ((FrankClassDoclet) c).getAnnotation(annotationFullName));
		if((result == null) && (getSuperclass() != null)) {
			result = ((FrankClassDoclet) getSuperclass()).getAnnotationFromImplementedInterfaces(annotationFullName);
		}
		return result;
	}

	@Override
	public String getJavaDocTag(String tagName) {
		Tag[] tags = clazz.tags(tagName);
		if((tags == null) || (tags.length == 0)) {
			return null;
		}
		// The Doclet API trims the value.
		return tags[0].text();
	}
}
