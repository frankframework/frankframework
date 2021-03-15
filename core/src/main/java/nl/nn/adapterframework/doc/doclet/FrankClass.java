package nl.nn.adapterframework.doc.doclet;

import java.util.List;

public interface FrankClass extends FrankType {
	@Override
	default boolean isPrimitive() {
		return false;
	}

	@Override
	default boolean isAnnotation() {
		return false;
	}

	String getSimpleName();
	FrankClass getSuperclass();
	boolean isAbstract();
	boolean isInterface();
	boolean isPublic();

	/**
	 * Assumes that this object models a Java interface and get the non-abstract interface implementations.
	 */
	List<FrankClass> getInterfaceImplementations() throws DocletReflectiveOperationException;

	FrankMethod[] getDeclaredMethods();
}
