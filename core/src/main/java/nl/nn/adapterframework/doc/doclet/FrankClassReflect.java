package nl.nn.adapterframework.doc.doclet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.doc.objects.SpringBean;

class FrankClassReflect implements FrankClass {
	private final Class<?> clazz;
	private final Map<String, FrankAnnotation> annotations;

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
	public List<FrankClass> getInterfaceImplementations() throws DocletReflectiveOperationException {
		List<SpringBean> springBeans;
		try {
			springBeans = Utils.getSpringBeans(clazz.getName());
		} catch(ReflectiveOperationException e) {
			throw new DocletReflectiveOperationException(String.format("Could not get interface implementations of Java class [%s]", getName()), e);
		}
		// We sort here to make the order deterministic.
		Collections.sort(springBeans);
		return springBeans.stream()
				.map(SpringBean::getClazz)
				.map(FrankClassReflect::new)
				.collect(Collectors.toList());
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
		FrankMethod[] result = new FrankMethod[rawDeclaredMethods.length];
		for(int i = 0; i < rawDeclaredMethods.length; ++i) {
			result[i] = new FrankMethodReflect(rawDeclaredMethods[i]);
		}
		return result;
	}
}
