package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationValue;

import nl.nn.adapterframework.util.LogUtil;

class FrankAnnotationDoclet implements FrankAnnotation {
	private static Logger log = LogUtil.getLogger(FrankAnnotationDoclet.class);

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
	public FrankAnnotation[] getAnnotations() {
		// TODO: Implement or throw exception that it is not implemented.
		return new FrankAnnotation[] {};
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		// TODO Implement or throw exception that it is not implemented.
		return null;
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
				log.warn("Could not cast annotation value to array: {}", rawValue.toString(), e);
				return rawValue.toString();
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
