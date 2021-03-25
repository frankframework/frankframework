package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.javadoc.AnnotationDesc;

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
				.collect(Collectors.toList());
		if(candidates.isEmpty()) {
			return null;
		} else {
			return candidates.get(0);
		}
	}
}
