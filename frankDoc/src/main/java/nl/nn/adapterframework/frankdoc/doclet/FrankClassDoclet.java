package nl.nn.adapterframework.frankdoc.doclet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;

import nl.nn.adapterframework.util.LogUtil;

class FrankClassDoclet implements FrankClass {
	private static Logger log = LogUtil.getLogger(FrankClassDoclet.class);

	private final FrankClassRepository repository;
	private final ClassDoc clazz;
	private final Set<String> childClassNames = new HashSet<>();
	private final Map<String, FrankClass> interfaceImplementationsByName = new HashMap<>();
	private final Map<MethodDoc, FrankMethod> frankMethodsByDocletMethod = new HashMap<>();
	private final Map<String, FrankAnnotation> frankAnnotationsByName;

	FrankClassDoclet(ClassDoc clazz, FrankClassRepository repository) {
		this.repository = repository;
		this.clazz = clazz;
		for(MethodDoc methodDoc: clazz.methods()) {
			frankMethodsByDocletMethod.put(methodDoc, new FrankMethodDoclet(methodDoc, this));
		}
		AnnotationDesc[] annotationDescs = clazz.annotations();
		frankAnnotationsByName = FrankDocletUtils.getFrankAnnotationsByName(annotationDescs);
	}

	void addChild(String className) {
		childClassNames.add(className);
	}

	void recursivelyAddInterfaceImplementation(FrankClassDoclet implementation) throws FrankDocException {
		interfaceImplementationsByName.put(implementation.getName(), implementation);
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
		return clazz.name();
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
	public FrankClass[] getInterfaces() throws FrankDocException {
		if(! isInterface()) {
			throw new FrankDocException(String.format("Class [%s] is not an interfaces, and hence method isInterfaces is not supported", getName()), null);
		}
		List<FrankClass> resultList = getInterfacesRaw();
		return resultList.toArray(new FrankClass[] {});
	}

	List<FrankClass> getInterfacesRaw() {
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
	public String[] getEnumConstants() {
		FieldDoc[] fieldDocs = clazz.enumConstants();
		String[] result = new String[fieldDocs.length];
		for(int i = 0; i < fieldDocs.length; ++i) {
			result[i] = fieldDocs[i].name();
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
}
