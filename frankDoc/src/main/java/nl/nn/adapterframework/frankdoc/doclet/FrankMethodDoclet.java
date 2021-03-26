package nl.nn.adapterframework.frankdoc.doclet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

import nl.nn.adapterframework.util.LogUtil;

class FrankMethodDoclet implements FrankMethod {
	private static Logger log = LogUtil.getLogger(FrankMethodDoclet.class);

	private final MethodDoc method;
	private final FrankClassDoclet declaringClass;

	FrankMethodDoclet(MethodDoc method, FrankClassDoclet declaringClass) {
		this.method = method;
		this.declaringClass = declaringClass;
	}

	@Override
	public String getName() {
		return method.name();
	}

	@Override
	public boolean isPublic() {
		return method.isPublic();
	}

	@Override
	public FrankAnnotation[] getAnnotations() {
		AnnotationDesc[] annotationDescs = method.annotations();
		FrankAnnotation[] annotations = new FrankAnnotation[annotationDescs.length];
		for(int i = 0; i < annotationDescs.length; ++i) {
			annotations[i] = new FrankAnnotationDoclet(annotationDescs[i]);
		}
		return annotations;
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		List<FrankAnnotation> candidates = Arrays.asList(getAnnotations()).stream()
				.filter(fa -> fa.getName().equals(name))
				.collect(Collectors.toList());
		if(candidates.isEmpty()) {
			return null;
		} else {
			return candidates.get(0);
		}
	}

	@Override
	public FrankClass getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public FrankType getReturnType() {
		Type docletType = method.returnType();
		return typeOf(docletType);
	}

	private FrankType typeOf(Type docletType) {
		if(docletType.isPrimitive()) {
			return new FrankPrimitiveType(docletType.simpleTypeName());
		} else {
			String typeName = docletType.qualifiedTypeName();
			try {
				FrankClass clazz = declaringClass.getRepository().findClass(typeName);
				if(clazz == null) {
					return new FrankNonCompiledClassDoclet(typeName);
				} else {
					return clazz;
				}
			} catch(FrankDocException e) {
				log.warn("Failed to search for class with name {}", typeName, e);
				return new FrankNonCompiledClassDoclet(typeName);
			}
		}
	}

	@Override
	public int getParameterCount() {
		return method.parameters().length;
	}

	@Override
	public FrankType[] getParameterTypes() {
		Parameter[] parametersDoclet = method.parameters();
		FrankType[] result = new FrankType[parametersDoclet.length];
		for(int i = 0; i < parametersDoclet.length; ++i) {
			result[i] = typeOf(parametersDoclet[i].type());
		}
		return result;
	}

	@Override
	public FrankAnnotation getAnnotationInludingInherited(String name) throws FrankDocException {
		FrankAnnotation result = getAnnotation(name);
		if(result != null) {
			return result;
		}
		MethodDoc overriddenMethodDoc = method.overriddenMethod();
		FrankMethod overriddenMethod = null;
		if(overriddenMethodDoc != null) {
			overriddenMethod = declaringClass.recursivelyFindFrankMethod(overriddenMethodDoc);
		}
		if(overriddenMethod != null) {
			result = overriddenMethod.getAnnotationInludingInherited(name);
		}
		return result;
	}

	List<FrankMethod> removeOverriddenMethod(List<FrankMethod> from) {
		MethodDoc toRemove = method.overriddenMethod();
		List<FrankMethod> result = new ArrayList<>();
		for(FrankMethod candidate: from) {
			if(! ((FrankMethodDoclet) candidate).method.equals(toRemove)) {
				result.add(candidate);
			}
		}
		return result;
	}
}
