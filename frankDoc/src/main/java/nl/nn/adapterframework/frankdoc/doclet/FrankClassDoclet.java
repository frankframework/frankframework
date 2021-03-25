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
import com.sun.javadoc.MethodDoc;

import nl.nn.adapterframework.util.LogUtil;

class FrankClassDoclet implements FrankClass {
	private static Logger log = LogUtil.getLogger(FrankClassDoclet.class);

	private final FrankClassRepository repository;
	private final ClassDoc clazz;
	private final Set<String> childClassNames = new HashSet<>();
	private final Map<String, FrankClass> interfaceImplementationsByName = new HashMap<>();

	FrankClassDoclet(ClassDoc clazz, FrankClassRepository repository) {
		this.repository = repository;
		this.clazz = clazz;
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
		AnnotationDesc[] annotationDescs = clazz.annotations();
		FrankAnnotation[] result = new FrankAnnotation[annotationDescs.length];
		for(int i = 0; i < annotationDescs.length; ++i) {
			result[i] = new FrankAnnotationDoclet(annotationDescs[i]);
		}
		return result;
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		FrankAnnotation[] allAnnotations = getAnnotations();
		List<FrankAnnotation> candidates = Arrays.asList(allAnnotations).stream()
				.filter(a -> a.getName().equals(name))
				.collect(Collectors.toList());
		if(candidates.isEmpty()) {
			return null;
		} else {
			return candidates.get(0);
		}
	}

	@Override
	public String getSimpleName() {
		return clazz.name();
	}

	@Override
	public FrankClass getSuperclass() {
		FrankClass result = null;
		ClassDoc superClazz = clazz.superclass();
		if(superClazz != null) {
			try {
				result = repository.findClass(superClazz.qualifiedName());
			} catch(FrankDocException e) {
				log.warn("Could not get superclass of {}", getName(), e);
			}
		}
		return result;
	}

	@Override
	public FrankClass[] getInterfaces() {
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
		FrankClass[] result = new FrankClass[resultList.size()];
		for(int i = 0; i < resultList.size(); ++i) {
			result[i] = resultList.get(i);
		}
		return result;
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
		List<FrankClass> result = new ArrayList<>();
		result.addAll(interfaceImplementationsByName.values());
		return result;
	}

	@Override
	public FrankMethod[] getDeclaredMethods() {
		MethodDoc[] methodDocs = clazz.methods();
		FrankMethod[] result = new FrankMethod[methodDocs.length];
		for(int i = 0; i < methodDocs.length; ++i) {
			result[i] = new FrankMethodDoclet(methodDocs[i], this);
		}
		return result;
	}

	@Override
	public FrankMethod[] getDeclaredAndInheritedMethods() {
		Map<String, List<FrankMethod>> inheritedMethodsByName = new HashMap<>();
		if(getSuperclass() != null) {
			inheritedMethodsByName = Arrays.asList(getSuperclass().getDeclaredAndInheritedMethods()).stream()
					.collect(Collectors.groupingBy(FrankMethod::getName));
		}
		for(FrankMethod declaredMethod: getDeclaredMethods()) {
			String declaredMethodName = declaredMethod.getName();
			if(inheritedMethodsByName.containsKey(declaredMethodName)) {
				List<FrankMethod> bucket = inheritedMethodsByName.get(declaredMethodName);
				inheritedMethodsByName.put(declaredMethodName, ((FrankMethodDoclet) declaredMethod).removeOverriddenMethod(bucket));
			}
		}
		List<FrankMethod> resultList = inheritedMethodsByName.values().stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());
		resultList.addAll(Arrays.asList(getDeclaredMethods()));
		FrankMethod[] result = new FrankMethod[resultList.size()];
		for(int i = 0; i < resultList.size(); ++i) {
			result[i] = resultList.get(i);
		}
		return result;
	}

	@Override
	public String[] getEnumConstants() {
		// TODO Auto-generated method stub
		return null;
	}

}
