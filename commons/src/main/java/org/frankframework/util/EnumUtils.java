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
package org.frankframework.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import jakarta.annotation.Nullable;

import org.frankframework.doc.DocumentedEnum;

/**
 * @author Niels Meijer
 */
public class EnumUtils {

	private EnumUtils() {
		throw new IllegalStateException("Don't constuct utility class");
	}

	public static <E extends Enum<E>> E parse(Class<E> enumClass, String value) {
		return parse(enumClass, value, false);
	}

	public static <E extends Enum<E>> E parse(Class<E> enumClass, String value, boolean fallbackToStandardEnumParsing) {
		return parse(enumClass, getFieldName(enumClass), value, fallbackToStandardEnumParsing);
	}

	private static String getFieldName(Class<?> enumClass) {
		String className = org.springframework.util.ClassUtils.getUserClass(enumClass).getSimpleName();
		char[] c = className.toCharArray();
		c[0] = Character.toLowerCase(c[0]);
		return new String(c);
	}

	public static <E extends Enum<E>> E parse(Class<E> enumClass, String fieldName, String value) {
		return parse(enumClass, fieldName, value, false);
	}

	public static <E extends Enum<E>> E parse(Class<E> enumClass, String fieldName, String value, boolean fallbackToStandardEnumParsing) {
		if (DocumentedEnum.class.isAssignableFrom(enumClass)) {
			try {
				return parseDocumented(enumClass, fieldName, value);
			} catch (IllegalArgumentException e1) {
				if (fallbackToStandardEnumParsing) {
					try {
						return parseNormal(enumClass, fieldName, value);
					} catch (IllegalArgumentException e2) {
						e1.addSuppressed(e2);
						throw e1;
					}
				} else {
					throw e1;
				}
			}
		}
		return parseNormal(enumClass, fieldName, value);
	}

	protected static <E extends Enum<E>> E parseNormal(Class<E> enumClass, String fieldName, String value) {
		E result = parseIgnoreCase(enumClass, value);
		if (result == null) {
			throw new IllegalArgumentException((fieldName != null ? "cannot set field [" + fieldName + "] to " : "") + "unparsable value [" + value + "]. Must be one of " + getEnumList(enumClass));
		}
		return result;
	}

	/**
	 * Solely for DocumentedEnums !
	 */
	protected static <E extends Enum<E>> E parseDocumented(Class<E> enumClass, String fieldName, String value) {
		return parseFromField(enumClass, fieldName, value, e -> ((DocumentedEnum) e).getLabel());
	}

	public static <E extends Enum<E>> E parseFromField(Class<E> enumClass, String fieldName, String value, Function<E, String> field) {
		List<String> fieldValues = new ArrayList<>();
		for (E e : getEnumList(enumClass)) {
			String fieldValue = field.apply(e);
			if (fieldValue.equalsIgnoreCase(value)) {
				return e;
			}
			fieldValues.add(fieldValue);
		}
		throw new IllegalArgumentException((fieldName != null ? "cannot set field [" + fieldName + "] to " : "") + "unparsable value [" + value + "]. Must be one of " + fieldValues);
	}

	public static <E extends Enum<E>> E parseFromField(Class<E> enumClass, String fieldName, int value, Function<E, Integer> field) {
		List<Integer> fieldValues = new ArrayList<>();
		for (E e : getEnumList(enumClass)) {
			int fieldValue = field.apply(e);
			if (fieldValue == value) {
				return e;
			}
			fieldValues.add(fieldValue);
		}
		throw new IllegalArgumentException((fieldName != null ? "cannot set field [" + fieldName + "] to " : "") + "unparsable value [" + value + "]. Must be one of " + fieldValues);
	}

	public static <E extends Enum<E>> List<E> getEnumList(final Class<E> enumClass) {
		return new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));
	}

	private static <E extends Enum<E>> E parseIgnoreCase(final Class<E> enumClass, final String enumName) {
		if (enumName == null) {
			return null;
		}
		for (final E each : enumClass.getEnumConstants()) {
			if (each.name().equalsIgnoreCase(enumName)) {
				return each;
			}
		}
		return null;
	}

	/**
	 * Find a single {@link Annotation} of {@code annotationType} on the supplied {@link Enum}.
	 *
	 * @param enumValue      the enum field to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @return the first matching annotation, or {@code null} if not found
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(Enum<?> enumValue, @Nullable Class<A> annotationType) {
		try {
			Field field = enumValue.getClass().getField(enumValue.name());
			return field.getAnnotation(annotationType);
		} catch (NoSuchFieldException | SecurityException ignored) {
			return null;
		} // No field found or not accessible, no warning
	}
}
