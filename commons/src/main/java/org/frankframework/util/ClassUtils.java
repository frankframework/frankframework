/*
   Copyright 2013-2017 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.INamedObject;

/**
 * A collection of class management utility methods.
 *
 * @author Johan Verrips
 */
@Log4j2
public class ClassUtils {

	private ClassUtils() {
		throw new IllegalStateException("Don't construct utility class");
	}
	/**
	 * Return the context ClassLoader.
	 */
	private static ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Attempts to locate the resource, first on the local class-path, if no match, it attempts to find the resource as file-path.
	 *
	 * @throws FileNotFoundException in case the resource name is invalid
	 */
	@Nullable
	public static URL getResourceURL(String resource) throws FileNotFoundException {
		String resourceToUse = resource; // Don't change the original resource name for logging purposes

		// Remove slash like Class.getResource(String name) is doing before delegation to ClassLoader.
		// Resources retrieved from ClassLoaders should never start with a leading slash
		if (resourceToUse.startsWith("/")) {
			resourceToUse = resourceToUse.substring(1);
		}
		URL url = getClassLoader().getResource(resourceToUse);

		// then try to get it as a URL
		if (url == null) {
			log.trace("did not find resource [{}] on local classpath", resourceToUse);
			url = getResourceNative(resourceToUse);
		}

		return url;
	}

	@Nullable
	private static URL getResourceNative(String resource) throws FileNotFoundException {
		try {
			if (resource.contains(":")) {
				String escapedURL = resource.replace(" ", "%20");
				log.trace("attempt to look up resource natively [{}]", escapedURL);
				return new URL(escapedURL);
			} else {
				// no URL -> treat as file path
				File file = new File(resource);
				log.trace("attempt to look up resource as file [{}]", file);
				if (file.exists()) {
					return file.toURI().toURL();
				}
			}
		} catch (MalformedURLException e) {
			FileNotFoundException fnfe = new FileNotFoundException("Resource location [" + resource + "] is neither a URL not a well-formed file path");
			fnfe.initCause(e);
			throw fnfe;
		}

		return null;
	}

	/**
	 * Retrieves the constructor of a class, based on the parameters
	 **/
	public static Constructor<?> getConstructorOnType(Class<?> clas, Class<?>[] parameterTypes) throws NoSuchMethodException {
		try {
			return clas.getDeclaredConstructor(parameterTypes);
		} catch (NoSuchMethodException e) {
			StringBuilder builder = new StringBuilder("cannot create constructor for Class [" + clas.getName() + "]");
			for (int i = 0; i < parameterTypes.length; i++) {
				builder.append(", parameter [").append(i).append("] type [").append(parameterTypes[i].getName()).append("]");
			}
			log.error(builder.toString(), e);
			throw e;
		}
	}

	/**
	 * Create a new instance given a class name. The constructor of the class does NOT have parameters.
	 *
	 * @param className    The class name to load
	 * @param expectedType The class type to expect
	 * @return A new instance
	 * @throws ReflectiveOperationException If an instantiation error occurs
	 * @throws SecurityException            If a security violation occurs
	 */
	@SuppressWarnings("unchecked") // because we checked it...
	public static <T> T newInstance(String className, Class<T> expectedType) throws ReflectiveOperationException, SecurityException {
		Class<?> clazz = loadClass(className);
		if (expectedType.isAssignableFrom(clazz)) {
			return (T) newInstance(clazz);
		}
		throw new InstantiationException("created class [" + className + "] is not of required type [" + expectedType.getSimpleName() + "]");
	}

	public static <T> T newInstance(Class<T> clazz) throws ReflectiveOperationException, SecurityException {
		return clazz.getDeclaredConstructor().newInstance();
	}

	/**
	 * Load a class given its name. We want to use a known ClassLoader.
	 *
	 * @param className A class name
	 * @return The class pointed to by <code>className</code>
	 * @throws ClassNotFoundException If a loading error occurs
	 */
	public static Class<?> loadClass(String className) throws ClassNotFoundException {
		return ClassUtils.getClassLoader().loadClass(className);
	}

	/**
	 * returns the className of the object, without the package name.
	 */
	@Nonnull
	public static String nameOf(Object o) {
		String tail = null;
		if (o instanceof INamedObject object) {
			String name = object.getName();
			if (StringUtils.isNotEmpty(name)) {
				tail = "[" + name + "]";
			}
		}
		return StringUtil.concatStrings(classNameOf(o), " ", tail);
	}

	/**
	 * returns the className of the object, like {@link #nameOf(Object)}, but without [name] suffix for a {@link INamedObject}.
	 */
	@Nonnull
	public static String classNameOf(Object o) {
		if (o == null) {
			return "<null>";
		}
		Class<?> clazz;
		if (isClassPresent("org.springframework.util.ClassUtils")) {
			if (o instanceof Class<?> class1) {
				clazz = org.springframework.util.ClassUtils.getUserClass(class1);
			} else {
				clazz = org.springframework.util.ClassUtils.getUserClass(o);
			}
		} else {
			clazz = o instanceof Class<?> c ? c : o.getClass();
		}

		final String simpleName = clazz.getSimpleName();
		return StringUtils.isNotEmpty(simpleName) ? simpleName : clazz.getTypeName();
	}

	public static boolean isClassPresent(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (Throwable ex) {
			// Class or one of its dependencies is not present...
			return false;
		}
	}

	/**
	 * Throws IllegalArgumentException if the argument type is incompatible
	 * Throws IllegalStateException if the argument cannot be set on the target bean
	 */
	public static void invokeSetter(Object bean, Method method, String valueToSet) {
		if (!method.getName().startsWith("set") || method.getParameterTypes().length != 1) {
			throw new IllegalStateException("method must start with [set] and may only contain [1] parameter");
		}

		try {// Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
			Object castValue = parseValueToSet(method, valueToSet);
			log.trace("trying to set method [{}] with value [{}] of type [{}] on [{}]", method::getName, () -> valueToSet, () -> castValue.getClass()
					.getCanonicalName(), () -> ClassUtils.nameOf(bean));

			method.invoke(bean, castValue);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException("error while calling method [" + method.getName() + "] on [" + ClassUtils.nameOf(bean) + "]", e);
		}
	}

	private static Object parseValueToSet(Method method, String value) throws IllegalArgumentException {
		Class<?> setterArgumentClass = method.getParameters()[0].getType();

		// Try to parse as primitive
		try {
			return convertToType(setterArgumentClass, value);
		} catch (IllegalArgumentException e) {
			String fieldName = StringUtil.lcFirst(method.getName().substring(3));
			throw new IllegalArgumentException("cannot set field [" + fieldName + "]: " + e.getMessage(), e);
		}
	}

	/**
	 * Converts the String value to the supplied type.
	 *
	 * @param type to convert the input value to
	 * @return The converted value, of type {@literal <T>}.
	 * @throws IllegalArgumentException (or NumberFormatException) when the value cannot be converted to the given type T.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T convertToType(Class<T> type, String value) throws IllegalArgumentException {
		return (T) convertToTypeRawTyped(type, value);
	}

	@Nullable
	private static Object convertToTypeRawTyped(Class<?> type, String value) throws IllegalArgumentException {
		if (value == null) {
			return null;
		}
		// Try to parse the value as an Enum
		if (type.isEnum()) {
			return parseAsEnum(type, value);
		}
		// Unbox an array to its component type. Convert string input to values. Put back into an array with the right type
		if (type.isArray()) {
			List<Object> list = StringUtil.splitToStream(value)
					.map(part -> convertToTypeRawTyped(type.getComponentType(), part))
					.toList();

			return list.toArray((Object[]) Array.newInstance(type.getComponentType(), 1));
		}

		try {
			return switch (type.getTypeName()) {
				case "int", "java.lang.Integer" -> Integer.parseInt(value);
				case "long", "java.lang.Long" -> Long.parseLong(value);
				case "boolean", "java.lang.Boolean" -> {
					if (value.isEmpty()) { // parseBoolean returns FALSE when it cannot parse the value
						throw new IllegalArgumentException("cannot convert empty string to boolean");
					}
					yield Boolean.parseBoolean(value); // parseBoolean returns FALSE when it cannot parse the value
				}
				case "java.lang.String" -> value;
				default -> throw new IllegalArgumentException("cannot convert to type [" + type.getName() + "], not implemented");
			};
		} catch (NumberFormatException e) { // Throw a -more descriptive- NumberFormatException instead of 'For input string'
			throw new NumberFormatException("value [" + value + "] cannot be converted to a number [" + type.getName() + "]");
		}
	}

	/**
	 * Attempt to parse the attributes value as an Enum.
	 *
	 * @param enumClass The Enum class used to parse the value
	 * @param value     The value to be parsed
	 * @return The Enum constant or <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	private static <E extends Enum<E>> E parseAsEnum(Class<?> enumClass, String value) throws IllegalArgumentException {
		return EnumUtils.parse((Class<E>) enumClass, null, value);
	}

	public static void invokeSetter(Object o, String name, Object value) throws SecurityException, ReflectiveOperationException, IllegalArgumentException {
		invokeSetter(o, name, value, value.getClass());
	}

	public static void invokeSetter(Object o, String name, Object value, Class<?> clazz) throws SecurityException, ReflectiveOperationException, IllegalArgumentException {
		List<Method> methods = Stream.of(o.getClass().getMethods())
				.filter(m -> m.getParameterCount() == 1) // Only Setters with 1 argument
				.filter(m -> name.equals(m.getName())) // Method name must match
				.filter(m -> m.getParameterTypes()[0].isAssignableFrom(clazz)) // Method argument must be assignable from the value class
				.toList();

		if (methods.isEmpty()) {
			throw new ReflectiveOperationException("no setter found matching signature "+o.getClass().getCanonicalName()+"."+name+"("+clazz.getCanonicalName()+")");
		} else if (methods.size() > 1) {
			throw new ReflectiveOperationException("more then one setter found matching signature "+o.getClass().getCanonicalName()+"."+name+"("+clazz.getCanonicalName()+")");
		}

		Method setterMtd = methods.get(0);
		Object[] args = { value };
		setterMtd.invoke(o, args);
	}

	public static Object invokeGetter(Object o, String name, boolean forceAccess) throws SecurityException, ReflectiveOperationException, IllegalArgumentException {
		Method getterMtd = o.getClass().getMethod(name, (Class<?>[]) null);
		if (forceAccess) {
			getterMtd.setAccessible(true);
		}
		return getterMtd.invoke(o, (Object[]) null);
	}

	public static Object getDeclaredFieldValue(Object o, Class<?> c, String name) throws IllegalArgumentException, SecurityException, NoSuchFieldException {
		Field f = c.getDeclaredField(name);
		try {
			f.setAccessible(true);
			return f.get(o);
		} catch (Exception e) {
			log.error("unable to retrieve field [{}] from object [{}]", name, o, e);
			return e.getMessage();
		}
	}

	public static Object getDeclaredFieldValue(Object o, String name) throws IllegalArgumentException, SecurityException, NoSuchFieldException {
		return getDeclaredFieldValue(o, o.getClass(), name);
	}

	public static List<Object> getClassInfoList(Class<?> clazz) {
		ClassLoader classLoader = clazz.getClassLoader();
		List<Object> infoList = new ArrayList<>();
		String className = clazz.getName();
		while (true) {
			infoList.add(getClassInfo(clazz, classLoader));
			if (classLoader == null || classLoader.equals(ClassLoader.getSystemClassLoader())) {
				break;
			}
			classLoader = classLoader.getParent();
			try {
				if (classLoader != null) {
					clazz = classLoader.loadClass(className);
				} else {
					clazz = ClassLoader.getSystemClassLoader().loadClass(className);
				}
			} catch (ClassNotFoundException e) {
				clazz = null;
			}
		}
		return infoList;
	}

	public static Map<String, Object> getClassInfo(Class<?> clazz, ClassLoader classLoader) {
		Map<String, Object> result = new LinkedHashMap<>();
		String classLoaderName = classLoader != null ? classLoader.toString() : "<system classloader>";
		result.put("classLoader", classLoaderName);
		if (clazz != null) {
			Package pkg = clazz.getPackage();
			result.put("specification", pkg.getSpecificationTitle() + " version " + pkg.getSpecificationVersion() + " by " + pkg.getSpecificationVendor());
			result.put("implementation", pkg.getImplementationTitle() + " version " + pkg.getImplementationVersion() + " by " + pkg.getImplementationVendor());

			CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
			result.put("codeSource", codeSource != null ? codeSource.getLocation().toString() : "unknown");

			URL classLocation = clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class");
			result.put("location", classLocation != null ? classLocation.toString() : "unknown");
		} else {
			result.put("message", "Class not found in this classloader");
		}
		return result;
	}

}
