/*
   Copyright 2013, 2016-2017 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IScopeProvider;

/**
 * A collection of class management utility methods.
 * @author Johan Verrips
 *
 */
public abstract class ClassUtils {
	private static Logger log = LogUtil.getLogger(ClassUtils.class);
	private static final String DEFAULT_ALLOWED_PROTOCOLS = AppConstants.getInstance().getString("classloader.allowed.protocols", null);

	/**
	 * Return the context ClassLoader.
	 */
	private static ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Retrieves the constructor of a class, based on the parameters
	**/
	public static Constructor<?> getConstructorOnType(Class<?> clas, Class<?>[] parameterTypes) throws NoSuchMethodException {
		try {
			return clas.getDeclaredConstructor(parameterTypes);
		} catch (java.lang.NoSuchMethodException e) {
			StringBuilder builder = new StringBuilder("cannot create constructor for Class [" + clas.getName() + "]");
			for (int i = 0; i < parameterTypes.length; i++) {
				builder.append(", parameter ["+i+"] type [" + parameterTypes[i].getName()+"]");
			}
			log.error(builder.toString(), e);
			throw e;
		}
	}

	/**
	 * Get a resource-URL directly from the ClassPath
	 * @param resource name of the resource you are trying to fetch the URL from
	 * @return URL of the resource or null if it can't be not found
	 */
	public static URL getResourceURL(String resource) {
		return getResourceURL(null, resource);
	}

	/**
	 * Get a resource-URL from a specific IConfigurationClassLoader. This should be used by
	 * classes which are part of the Ibis configuration (like pipes and senders)
	 * because the configuration might be loaded from outside the webapp
	 * ClassPath. Hence the Thread.currentThread().getContextClassLoader() at
	 * the time the class was instantiated should be used.
	 *
	 * @see IbisContext#init()
	 */
	public static URL getResourceURL(IScopeProvider scopeProvider, String resource) {
		return getResourceURL(scopeProvider, resource, null);
	}

	/**
	 * Get a resource-URL from a ClassLoader, therefore the resource should not start with a leading slash
	 * @param scopeProvider to retrieve the file from, or NULL when you want to retrieve the resource directly from the ClassPath (using an absolute path)
	 * @param resource name of the resource you are trying to fetch the URL from
	 * @return URL of the resource or null if it can't be not found
	 */
	public static URL getResourceURL(IScopeProvider scopeProvider, String resource, String allowedProtocols) {
		ClassLoader classLoader = null;
		if(scopeProvider == null) { // Used by ClassPath resources
			classLoader = Thread.currentThread().getContextClassLoader();
		} else {
			classLoader = scopeProvider.getConfigurationClassLoader();
		}

		String resourceToUse = resource; //Don't change the original resource name for logging purposes
		if (resource.startsWith(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME)) {
			resourceToUse = resource.substring(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME.length());
		}

		// Remove slash like Class.getResource(String name) is doing before delegation to ClassLoader.
		// Resources retrieved from ClassLoaders should never start with a leading slash
		if (resourceToUse.startsWith("/")) {
			resourceToUse = resourceToUse.substring(1);
		}
		URL url = classLoader.getResource(resourceToUse);

		// then try to get it as a URL
		if (url == null) {
			if (resourceToUse.contains(":")) {
				String protocol = resourceToUse.substring(0, resourceToUse.indexOf(":"));
				if (allowedProtocols==null) {
					allowedProtocols = DEFAULT_ALLOWED_PROTOCOLS;
				}
				if (StringUtils.isNotEmpty(allowedProtocols)) {
					//log.debug("Could not find resource ["+resource+"] in classloader ["+classLoader+"] now trying via protocol ["+protocol+"]");

					List<String> protocols = Arrays.asList(allowedProtocols.split(","));
					if(protocols.contains(protocol)) {
						try {
							url = new URL(StringUtil.replace(resourceToUse, " ", "%20"));
						} catch(MalformedURLException e) {
							log.debug("Could not find resource ["+resource+"] in classloader ["+nameOf(classLoader)+"] and not as URL [" + resource + "]: "+e.getMessage());
						}
					} else if(log.isDebugEnabled()) log.debug("Cannot lookup resource ["+resource+"] in classloader ["+nameOf(classLoader)+"], not allowed with protocol ["+protocol+"] allowedProtocols "+protocols.toString());
				} else {
					if(log.isDebugEnabled()) log.debug("Could not find resource as URL [" + resource + "] in classloader ["+nameOf(classLoader)+"], with protocol ["+protocol+"], no allowedProtocols");
				}
			} else {
				if(log.isDebugEnabled()) log.debug("Cannot lookup resource ["+resource+"] in classloader ["+nameOf(classLoader)+"] and no protocol to try as URL");
			}
		}

		return url;
	}

	public static List<String> getAllowedProtocols() {
		if(StringUtils.isEmpty(DEFAULT_ALLOWED_PROTOCOLS)) {
			return new ArrayList<>(); //Arrays.asList(..) won't return an empty List when empty.
		}
		return Arrays.asList(DEFAULT_ALLOWED_PROTOCOLS.split(","));
	}

	public static InputStream urlToStream(URL url, int timeoutMs) throws IOException {
		URLConnection conn = url.openConnection();
		if (timeoutMs==0) {
			timeoutMs = 10000;
		}
		if (timeoutMs>0) {
			conn.setConnectTimeout(timeoutMs);
			conn.setReadTimeout(timeoutMs);
		}
		return conn.getInputStream(); //SCRV_269S#072 //SCRV_286S#077
	}

	public static Reader urlToReader(URL url) throws IOException {
		return urlToReader(url, 0);
	}

	public static Reader urlToReader(URL url, int timeoutMs) throws IOException {
		return StreamUtil.getCharsetDetectingInputStreamReader(urlToStream(url,timeoutMs));
	}

	/**
	 * Determine the last modification date for this class file or its enclosing library
	 *
	 * @param aClass A class whose last modification date is queried
	 * @return The time the given class was last modified
	 * @exception IllegalArgumentException The class was not loaded from a file or directory
	 */
	public static long lastModified(Class<?> aClass) throws IllegalArgumentException {
		URL url = aClass.getProtectionDomain().getCodeSource().getLocation();

		if(!url.getProtocol().equals("file")) {
			throw new IllegalArgumentException("Class was not loaded from a file url");
		}

		File directory = new File(url.getFile());
		if(!directory.isDirectory()) {
			throw new IllegalArgumentException("Class was not loaded from a directory");
		}

		String className = aClass.getName();
		String basename = className.substring(className.lastIndexOf(".") + 1);

		File file = new File(directory, basename + ".class");

		return file.lastModified();
	}

	/**
	 * Create a new instance given a class name. The constructor of the class does NOT have parameters.
	 *
	 * @param className A class name
	 * @return A new instance
	 * @exception Exception If an instantiation error occurs
	 */
	public static Object newInstance(String className) throws Exception {
		return ClassUtils.loadClass(className).newInstance();
	}

	/**
	 * Load a class given its name. BL: We wan't to use a known
	 * ClassLoader--hopefully the hierarchy is set correctly.
	 *
	 * @param className A class name
	 * @return The class pointed to by <code>className</code>
	 * @exception ClassNotFoundException If a loading error occurs
	 */
	public static Class<?> loadClass(String className) throws ClassNotFoundException {
		return ClassUtils.getClassLoader().loadClass(className);
	}

	/**
	 * Gets the absolute pathname of the class file containing the specified class name, as prescribed by the current classpath.
	 */
	public static String which(Class<?> aClass) {
		String path = null;
		try {
			path = aClass.getProtectionDomain().getCodeSource().getLocation().toString();
		} catch (Throwable t) {
			// Catch all exceptions, return null if the path cannot be determined.
		}
		return path;
	}

	/**
	 * If the classLoader is derivable of IConfigurationClassLoader return the className + configurationName,
	 * else return the className of the object. Don't return the package name to avoid cluttering the logs.
	 */
	public static String nameOf(ClassLoader classLoader) {
		if(classLoader == null) {
			return "<null>";
		}

		String logPrefix = nameOf((Object) classLoader) + "@" + Integer.toHexString(classLoader.hashCode());
		if(classLoader instanceof IConfigurationClassLoader) {
			String configurationName = ((IConfigurationClassLoader) classLoader).getConfigurationName();
			if(StringUtils.isNotEmpty(configurationName)) {
				logPrefix += "["+configurationName+"]";
			}
		}
		return logPrefix;
	}

	/**
	 * returns the className of the object, without the package name.
	 */
	public static String nameOf(Object o) {
		String tail=null;
		if (o instanceof INamedObject) {
			String name = ((INamedObject)o).getName();
			if (name!=null) {
				tail = "["+ name +"]";
			}
		}
		return StringUtil.concatStrings(classNameOf(o), " ", tail);
	}

	/**
	 * returns the className of the object, like {@link #nameOf(Object)}, but without [name] suffix for a {@link INamedObject}.
	 */
	public static String classNameOf(Object o) {
		if (o==null) {
			return "<null>";
		}
		if(o instanceof Class) {
			return org.springframework.util.ClassUtils.getUserClass((Class<?>)o).getSimpleName();
		}
		Class<?> clazz = org.springframework.util.ClassUtils.getUserClass(o);
		String simpleName = clazz.getSimpleName();

		if (StringUtils.isEmpty(simpleName)) {
			simpleName = clazz.getTypeName();
		}
		return simpleName;
	}

	public static void invokeSetter(Object bean, Method method, String valueToSet) {
		if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1) {
			throw new IllegalArgumentException("method must start with [set] and may only contain [1] parameter");
		}

		try {//Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
			Object castValue = castAsType(method.getParameterTypes()[0], valueToSet);
			if(log.isDebugEnabled()) log.debug("trying to set method ["+method.getName()+"] with value ["+valueToSet+"] of type ["+castValue.getClass().getCanonicalName()+"] on ["+ClassUtils.nameOf(bean)+"]");

			method.invoke(bean, castValue);
		} catch (Exception e) {
			throw new IllegalArgumentException("error while calling method ["+method.getName()+"] on ["+ClassUtils.nameOf(bean)+"]", e);
		}
	}

	private static Object castAsType(Class<?> type, String value) {
		String className = type.getName().toLowerCase();
		if("boolean".equals(className))
			return Boolean.parseBoolean(value);
		else if("int".equals(className) || "integer".equals(className))
			return Integer.parseInt(value);
		else
			return value;
	}

	public static void invokeSetter(Object o, String name, Object value) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		invokeSetter(o, name, value, value.getClass());
	}
	public static void invokeSetter(Object o, String name, Object value, Class<?> clazz) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class<?>[] argsTypes = { clazz };
		Method setterMtd = o.getClass().getMethod(name, argsTypes );
		Object[] args = { value };
		setterMtd.invoke(o, args);
	}
	public static Object invokeGetterSafe(Object o, String name, boolean forceAccess) {
		try {
			return invokeGetter(o,name,forceAccess);
		} catch (Exception e) {
			return nameOf(o)+"."+name+"() "+nameOf(e)+": "+e.getMessage();
		}
	}
	public static Object invokeGetter(Object o, String name, boolean forceAccess) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Method getterMtd = o.getClass().getMethod(name, (Class<?>[]) null);
		if (forceAccess) {
			getterMtd.setAccessible(true);
		}
		return getterMtd.invoke(o, (Object[]) null);
	}
	public static Object invokeGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return invokeGetter(o,name,false);
	}

	public static String invokeStringGetterSafe(Object o, String name) {
		try {
			return invokeStringGetter(o,name);
		} catch (Exception e) {
			return nameOf(o)+"."+name+"() "+nameOf(e)+": "+e.getMessage();
		}
	}
	public static String invokeStringGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return (String)invokeGetter(o,name);
	}

	public static Object getFieldValueSafe(Object o, String name) {
		try {
			return getFieldValue(o,name);
		} catch (Exception e) {
			return nameOf(o)+"."+name+" "+nameOf(e)+": "+e.getMessage();
		}
	}
	public static Object getFieldValue(Object o, Class<?> c, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return c.getField(name).get(o);
	}
	public static Object getFieldValue(Object o, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return getFieldValue(o, o.getClass(), name);
	}

	public static Object getDeclaredFieldValue(Object o, Class<?> c, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		Field f = c.getDeclaredField(name);
		try {
			f.setAccessible(true);
			return f.get(o);
		} catch (Exception e) {
			log.error("unable to retrieve field [{}] from object [{}]", name, o, e);
			return e.getMessage();
		}
	}
	public static Object getDeclaredFieldValue(Object o, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return getDeclaredFieldValue(o, o.getClass(), name);
	}

	private static void appendFieldsAndMethods(StringBuffer result, Object o, String type, Class<?> c) {
		Field[] fields = c.getDeclaredFields();
		Method[] methods = c.getDeclaredMethods();
		result.append(type+ " "+c.getName()+" #fields ["+fields.length+"] #methods ["+methods.length+"]");
		if (fields.length>0 || methods.length>0) {
			result.append(" {\n");
			for (int i=0; i<fields.length; i++) {
				Field f=fields[i];
				Object value;
				try {
					f.setAccessible(true);
					value=f.get(o);
				} catch (Exception e) {
					value="Could not get value: "+ClassUtils.nameOf(e)+": "+e.getMessage();
				}
				result.append("  field["+i+"] "+f.getName()+"("+f.getType().getName()+"): ["+value+"]\n");
			}
			for (int i=0; i<methods.length; i++) {
				Method m=methods[i];
				result.append("  method["+i+"] "+m.getName());
				result.append("\n");
			}
			result.append("}");
		}
		result.append("\n");
	}

	public static String debugObject(Object o) {
		if (o==null) {
			return null;
		}

		StringBuffer result = new StringBuffer(nameOf(o)+"\n");
		Class<?> c=o.getClass();
		Class<?>[] interfaces = c.getInterfaces();
		for (int i=0;i<interfaces.length; i++) {
			appendFieldsAndMethods(result,o,"Interface",interfaces[i]);
		}
		while (c!=Object.class) {
			appendFieldsAndMethods(result,o,"Class",c);
			c=c.getSuperclass();
		}
		result.append("toString=["+o.toString()+"]\n");
		return result.toString();
	}

	/**
	 * clean up file path, to replace websphere specific classpath references with generic ones.
	 */
	public static String getCleanedFilePath(String path) {
		if(path.contains("wsjar:")) {
			return path.replace("wsjar:", "jar:");
		}
		return path;
	}

	public static List<Object> getClassInfoList(Class<?> clazz) throws IOException {
		ClassLoader classLoader = clazz.getClassLoader();
		List<Object> infoList = new LinkedList<>();
		String className = clazz.getName();
		while (true) {
			infoList.add(getClassInfo(clazz, classLoader));
			if (classLoader == null || classLoader.equals(ClassLoader.getSystemClassLoader())) {
				break;
			}
			classLoader = classLoader.getParent();
			try {
				if (classLoader!=null) {
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

	public static Map<String,Object> getClassInfo(Class clazz, ClassLoader classLoader) throws IOException {

		Map<String,Object> result = new LinkedHashMap<>();
		String classLoaderName=classLoader!=null? classLoader.toString() : "<system classloader>";
		result.put("classLoader", classLoaderName);
		if (clazz!=null) {
			Package pkg = clazz.getPackage();
			result.put("specification",  pkg.getSpecificationTitle() +" version " + pkg.getSpecificationVersion() +" by "+ pkg.getSpecificationVendor());
			result.put("implementation", pkg.getImplementationTitle()+" version " + pkg.getImplementationVersion()+" by "+ pkg.getImplementationVendor());

			CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
			result.put("codeSource", codeSource!=null ? codeSource.getLocation().toString() : "unknown");

			URL classLocation = clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class");
			result.put("location", classLocation!=null ? classLocation.toString() : "unknown");
		} else {
			result.put("message", "Class not found in this classloader");
		}
		return result;
	}

}
