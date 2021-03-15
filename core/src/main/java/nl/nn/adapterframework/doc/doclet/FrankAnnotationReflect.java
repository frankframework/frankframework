package nl.nn.adapterframework.doc.doclet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class FrankAnnotationReflect implements FrankAnnotation {
	private Annotation annotation;

	FrankAnnotationReflect(Annotation annotation) {
		this.annotation = annotation;
	}

	@Override
	public String getName() {
		return annotation.annotationType().getName();
	}
	
	@Override
	public boolean isPublic() {
		return Modifier.isPublic(annotation.annotationType().getModifiers());
	}

	public FrankAnnotation[] getAnnotations() {
		return new FrankAnnotation[] {};
	}

	public FrankAnnotation getAnnotation(String name) {
		return null;
	}

	@Override
	public Object getValue() throws DocletReflectiveOperationException {
		try {
			Method valueMethod = annotation.annotationType().getMethod("value");
			return valueMethod.invoke(annotation);
		} catch(Exception e) {
			throw new DocletReflectiveOperationException(String.format("Could not get value of annotation [%s]", getName()), e);
		}
	}
}
