package org.frankframework.dataconversion;

abstract sealed public class AbstractTypedDataConverter<T> permits TypedBinaryDataConverter, TypedCharacterDataConverter {
	protected final T data;

	protected AbstractTypedDataConverter(T data) {
		this.data = data;
	}
}
