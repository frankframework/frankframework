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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;

class FrankMethodReflect implements FrankMethod {
	private final Method method;
	private final FrankClass declaringClass;
	private final Map<String, FrankAnnotation> annotations;

	FrankMethodReflect(Method method, FrankClass declaringClass) {
		this.method = method;
		this.declaringClass = declaringClass;
		annotations = new HashMap<>();
		for(Annotation r: method.getAnnotations()) {
			FrankAnnotation frankAnnotation = new FrankAnnotationReflect(r);
			annotations.put(frankAnnotation.getName(), frankAnnotation);
		}
	}

	@Override
	public String getName() {
		return method.getName();
	}

	@Override
	public FrankClass getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(method.getModifiers());
	}

	@Override
	public FrankType getReturnType() {
		return typeOf(method.getReturnType());
	}

	private FrankType typeOf(Class<?> clazz) {
		if(clazz.isPrimitive()) {
			return new FrankPrimitiveType(clazz.getName());
		} else {
			return new FrankClassReflect(clazz, (FrankClassRepositoryReflect) ((FrankClassReflect) declaringClass).getRepository());
		}
	}

	@Override
	public int getParameterCount() {
		return method.getParameterCount();
	}

	@Override
	public boolean isVarargs() {
		return method.isVarArgs();
	}

	@Override
	public FrankType[] getParameterTypes() {
		FrankType[] result = new FrankType[method.getParameterCount()];
		for(int i = 0; i < method.getParameterCount(); ++i) {
			result[i] = typeOf(method.getParameterTypes()[i]);
		}
		return result;
	}

	@Override
	public FrankAnnotation[] getJava5Annotations() {
		List<FrankAnnotation> annotationList = new ArrayList<>(annotations.values());
		FrankAnnotation[] result = new FrankAnnotation[annotationList.size()];
		for(int i = 0; i < annotationList.size(); ++i) {
			result[i] = annotationList.get(i);
		}
		return result;
	}

	@Override
	public FrankAnnotation getJava5Annotation(String name) {
		return annotations.get(name);
	}

	@Override
	public FrankAnnotation getJava5AnnotationInludingInherited(String name) throws FrankDocException {
		Annotation rawAnnotation = null;
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) Class.forName(name);
			rawAnnotation = AnnotationUtils.findAnnotation(method, annotationClass);
		} catch(Exception e) {
			throw new FrankDocException(String.format("Could not get annotation [%s] including inherited", name), e);
		}
		if(rawAnnotation == null) {
			return null;
		} else {
			return new FrankAnnotationReflect(rawAnnotation);
		}
	}

	@Override
	public String getJavaDoc() {
		return null;
	}
	
	@Override
	public String getJavaDocIncludingInherited() {
		return null;
	}

	@Override
	public String getJavaDocTag(String tagName) {
		return null;
	}

	@Override
	public String getJavaDocTagIncludingInherited(String tagName) {
		return null;
	}

	@Override
	public String toString() {
		return toStringImpl();
	}
}
