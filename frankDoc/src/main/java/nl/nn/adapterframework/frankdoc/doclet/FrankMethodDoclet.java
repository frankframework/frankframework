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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;
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
	public String getJavaDoc() {
		String result = method.commentText();
		// We need null when there is no JavaDoc, not the empty string.
		// We use the null result in getJavaDocIncludingInherited() to
		// continue searching.
		if(StringUtils.isBlank(result)) {
			return null;
		}
		return result;
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
		Function<FrankMethodDoclet, FrankAnnotation> getter = m -> m.getAnnotation(name);
		return searchIncludingInherited(getter);
	}

	@Override
	public String getJavaDocIncludingInherited() throws FrankDocException {
		Function<FrankMethodDoclet, String> getter = m -> m.getJavaDoc();
		return searchIncludingInherited(getter);
	}

	private <T> T searchIncludingInherited(Function<FrankMethodDoclet, T> getter) throws FrankDocException {
		T result = searchExcludingImplementedInterfaces(getter);
		if(result == null) {
			result = searchImplementedInterfaces(this.getDeclaringClass(), this.getSignature(), getter);
		}
		return result;
	}

	private <T> T searchExcludingImplementedInterfaces(Function<FrankMethodDoclet, T> getter) throws FrankDocException {
		T result = getter.apply(this);
		if(result != null) {
			return result;
		}
		MethodDoc overriddenMethodDoc = method.overriddenMethod();
		if(overriddenMethodDoc != null) {
			FrankMethodDoclet overriddenMethod = (FrankMethodDoclet) declaringClass.recursivelyFindFrankMethod(overriddenMethodDoc);
			if(overriddenMethod != null) {
				return overriddenMethod.searchExcludingImplementedInterfaces(getter);
			} else {
				// The overridden method is not included in the produced JavaDocs. This
				// means that the overridden method is not public. Therefore it is not
				// relevant.
				//
				// This empty else branch is covered by test
				// FrankMethodOverrideTest.whenPackagePrivateOverriddenByPublicThenOnlyChildMethodConsidered()
			}
		}
		return null;
	}

	String getSignature() {
		List<String> components = new ArrayList<>();
		components.add(getName());
		for(FrankType type: getParameterTypes()) {
			components.add(type.getName());
		}
		return components.stream().collect(Collectors.joining(", "));
	}

	private <T> T searchImplementedInterfaces(FrankClass clazz, String methodSignature, Function<FrankMethodDoclet, T> getter) throws FrankDocException {
		TransitiveImplementedInterfaceBrowser<T> interfaceBrowser = new TransitiveImplementedInterfaceBrowser<>((FrankClassDoclet) clazz);
		Function<FrankClass, T> classGetter = interfaze -> ((FrankClassDoclet) interfaze).getMethodItemFromSignature(methodSignature, getter);
		T result = interfaceBrowser.search(classGetter);
		if(result != null) {
			return result;
		}
		if(clazz.getSuperclass() == null) {
			return null;
		}
		return searchImplementedInterfaces(clazz.getSuperclass(), methodSignature, getter);
	}

	void removeOverriddenFrom(Map<MethodDoc, FrankMethod> methodRepository) {
		MethodDoc toRemove = method.overriddenMethod();
		methodRepository.remove(toRemove);
	}

	void addToRepository(Map<MethodDoc, FrankMethod> methodRepository) {
		methodRepository.put(method, this);
	}

	@Override
	public String getJavaDocTag(String tagName) {
		Tag[] tags = method.tags(tagName);
		if((tags == null) || (tags.length == 0)) {
			return null;
		}
		// The Doclet API trims the value.
		return tags[0].text();
	}

	@Override
	public String getJavaDocTagIncludingInherited(String tagName) throws FrankDocException {
		Function<FrankMethodDoclet, String> getter = m -> m.getJavaDocTag(tagName);
		return searchIncludingInherited(getter);		
	}

	@Override
	public String toString() {
		return toStringImpl();
	}
}
