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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationValue;

class FrankAnnotationDoclet implements FrankAnnotation {
	private final AnnotationDesc annotation;

	FrankAnnotationDoclet(AnnotationDesc annotation) {
		this.annotation = annotation;
	}

	@Override
	public String getName() {
		return annotation.annotationType().qualifiedName();
	}

	@Override
	public boolean isPublic() {
		return annotation.annotationType().isPublic();
	}

	@Override
	public Object getValue() throws FrankDocException {
		List<Object> candidates = getField("value");
		return getValueFromFieldRemovingRepetition(candidates);
	}

	@Override
	public Object getValueOf(String fieldName) throws FrankDocException {
		List<Object> candidates = getField(fieldName);
		return getValueFromFieldRemovingRepetition(candidates);
	}

	private List<Object> getField(String fieldName) {
		List<Object> candidates = Arrays.asList(annotation.elementValues()).stream()
				.filter(ev -> ev.element().name().equals(fieldName))
				.map(ev -> ev.value().value())
				.collect(Collectors.toList());
		return candidates;
	}

	private Object getValueFromFieldRemovingRepetition(List<Object> candidates) throws FrankDocException {
		if(candidates.isEmpty()) {
			return null;
		} else {
			Object raw = candidates.get(0);
			if((raw instanceof Integer) || (raw instanceof String)) {
				return raw;
			} else {
				return parseAnnotationValueAsStringArray(raw);
			}
		}
	}

	private Object parseAnnotationValueAsStringArray(Object rawValue) throws FrankDocException {
		AnnotationValue[] valueAsArray = null;
		try {
			valueAsArray = (AnnotationValue[]) rawValue;
		} catch(ClassCastException e) {
			throw new FrankDocException(String.format("Annotation has unknown type: [%s]", getName()), e);
		}
		List<String> valueAsStringList = Arrays.asList(valueAsArray).stream()
				.map(v -> v.value().toString())
				.collect(Collectors.toList());
		String[] result = new String[valueAsStringList.size()];
		for(int i = 0; i < valueAsStringList.size(); ++i) {
			result[i] = valueAsStringList.get(i);
		}
		return result;
	}
}
