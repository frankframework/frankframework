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
		List<Object> candidates = Arrays.asList(annotation.elementValues()).stream()
				.filter(ev -> ev.element().name().equals("value"))
				.map(ev -> ev.value().value())
				.collect(Collectors.toList());
		if(candidates.isEmpty()) {
			return null;
		} else {
			Object rawValue = candidates.get(0);
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
}
