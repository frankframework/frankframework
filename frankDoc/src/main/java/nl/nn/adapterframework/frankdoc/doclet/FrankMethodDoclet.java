package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Map;

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
	private final Map<String, FrankAnnotation> frankAnnotationsByName;

	FrankMethodDoclet(MethodDoc method, FrankClassDoclet declaringClass) {
		this.method = method;
		this.declaringClass = declaringClass;
		AnnotationDesc[] annotationDescs = method.annotations();
		frankAnnotationsByName = FrankDocletUtils.getFrankAnnotationsByName(annotationDescs);
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
		return frankAnnotationsByName.values().toArray(new FrankAnnotation[] {});
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		return frankAnnotationsByName.get(name);
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
	public boolean isVarargs() {
		return method.isVarArgs();
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

	void removeOverriddenFrom(Map<MethodDoc, FrankMethod> methodRepository) {
		MethodDoc toRemove = method.overriddenMethod();
		methodRepository.remove(toRemove);
	}

	void addToRepository(Map<MethodDoc, FrankMethod> methodRepository) {
		methodRepository.put(method, this);
	}
}
