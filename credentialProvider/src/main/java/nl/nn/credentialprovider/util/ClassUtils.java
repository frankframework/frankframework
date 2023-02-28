/*
   Copyright 2013, 2016-2017 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.credentialprovider.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;



public class ClassUtils {
	protected static Logger log = Logger.getLogger(ClassUtils.class.getName());

    /**
     * Return the context classloader.
     * BL: if this is command line operation, the classloading issues
     *     are more sane.  During servlet execution, we explicitly set
     *     the ClassLoader.
     *
     * @return The context classloader.
     */
    private static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
	/**
	* Retrieves the constructor of a class, based on the parameters
	*
	**/
	public static Constructor getConstructorOnType(Class clas, Class[] parameterTypes) {
		Constructor theConstructor = null;
		try {
			theConstructor = clas.getDeclaredConstructor(parameterTypes);
		} catch (NoSuchMethodException e) {
			log.log(Level.SEVERE, "cannot create constructor for Class [" + clas.getName() + "]", e);
			for (int i = 0; i < parameterTypes.length; i++)
				log.severe("Parameter " + i + " type " + parameterTypes[i].getName());
		}
		return theConstructor;
	}

	public static URL getResourceURL(String resource) throws FileNotFoundException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		String resourceToUse = resource; //Don't change the original resource name for logging purposes

		// Remove slash like Class.getResource(String name) is doing before delegation to ClassLoader.
		// Resources retrieved from ClassLoaders should never start with a leading slash
		if (resourceToUse.startsWith("/")) {
			resourceToUse = resourceToUse.substring(1);
		}
		URL url = classLoader.getResource(resourceToUse);

		// then try to get it as a URL
		if (url == null && resourceToUse.contains(":")) {
			try {
				url = new URL(StringUtil.replace(resourceToUse, " ", "%20"));
			} catch (MalformedURLException e) {
				FileNotFoundException fnfe = new FileNotFoundException("Cannot find resource ["+resourceToUse+"]");
				fnfe.initCause(e);
				throw fnfe;
			}
		}

		return url;
	}


    /**
     * Tests if a class implements a given interface
     *
     * @return true if class implements given interface.
     */
    public static boolean implementsInterface(Class class1, Class iface) {
        return iface.isAssignableFrom (class1);
    }
    /**
     * Tests if a class implements a given interface
     *
     * @return true if class implements given interface.
     */
    public static boolean implementsInterface(String className, String iface) throws Exception {
        Class class1 = ClassUtils.loadClass (className);
        Class class2 = ClassUtils.loadClass (iface);
        return ClassUtils.implementsInterface(class1, class2);
    }
    /**
     * Determine the last modification date for this
     * class file or its enclosing library
     *
     * @param aClass A class whose last modification date is queried
     * @return The time the given class was last modified
     * @exception IOException IOError
     * @exception IllegalArgumentException The class was not loaded from a file
     * or directory
     */
    public static long lastModified(Class aClass)
        throws IOException, IllegalArgumentException  {
        URL url = aClass.getProtectionDomain().getCodeSource().getLocation();

        if (!url.getProtocol().equals("file")) {
            throw new IllegalArgumentException("Class was not loaded from a file url");
        }

        File directory = new File(url.getFile());
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Class was not loaded from a directory");
        }

        String className = aClass.getName();
        String basename = className.substring(className.lastIndexOf(".") + 1);

        File file = new File(directory, basename + ".class");

        return file.lastModified();
    }
    /**
     * Load a class given its name.
     * BL: We wan't to use a known ClassLoader--hopefully the heirarchy
     *     is set correctly.
     *
     * @param className A class name
     * @return The class pointed to by <code>className</code>
     * @exception ClassNotFoundException If a loading error occurs
     */
    public static Class loadClass(String className) throws ClassNotFoundException {
        return ClassUtils.getClassLoader().loadClass(className);
    }
    /**
     * Create a new instance given a class name. The constructor of the class
     * does NOT have parameters.
     *
     * @param className A class name
     * @return A new instance
     * @exception Exception If an instantiation error occurs
     */
    public static Object newInstance(String className) throws Exception {
        return ClassUtils.loadClass(className).newInstance();
    }
  /**
   * creates a new instance of an object, based on the classname as string, the classes
   * and the actual parameters.
   */
  public static Object newInstance(String className, Class[] parameterClasses, Object[] parameterObjects) {
    // get a class object
    Class clas = null;
    try {
      clas=ClassUtils.loadClass(className);
    } catch (ClassNotFoundException C) {System.err.println(C);}

     Constructor con;
     con= ClassUtils.getConstructorOnType(clas, parameterClasses);
     Object theObject=null;
     try {
        theObject=con.newInstance(parameterObjects);
      } catch(InstantiationException E) {System.err.println(E);}
        catch(IllegalAccessException A) {System.err.println(A);}
        catch(InvocationTargetException T) {System.err.println(T);}
     return theObject;

  }

	/**
	 * Creates a new instance from a class, while it looks for a constructor
	 * that matches the parameters, and initializes the object (by calling the constructor)
	 * Notice: this does not work when the instantiated object uses an interface class
	 * as a parameter, as the class names are, in that case, not the same..
	 *
	 * @param className a class Name
	 * @param parameterObjects the parameters for the constructor
	 * @return A new Instance
	 *
	 **/
	public static Object newInstance(String className, Object[] parameterObjects) {
	    Class parameterClasses[] = new Class[parameterObjects.length];
	    for (int i = 0; i < parameterObjects.length; i++)
	        parameterClasses[i] = parameterObjects[i].getClass();
	    return newInstance(className, parameterClasses, parameterObjects);
	}

    /**
     * Gets the absolute pathname of the class file
     * containing the specified class name, as prescribed
     * by the current classpath.
     *
     * @param aClass A class
     */
     public static String which(Class aClass) {
        String path = null;
        try {
            path = aClass.getProtectionDomain().getCodeSource().getLocation().toString();
        } catch (Throwable t){
        }
        return path;
    }


	/**
	 * returns the className of the object, without the package name.
	 */
	public static String nameOf(Object o) {
		if (o==null) {
			return "<null>";
		}
		return o.getClass().getTypeName();
	}

	public static void invokeSetter(Object o, String name, Object value) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		invokeSetter(o,name,value,value.getClass());
	}
	public static void invokeSetter(Object o, String name, Object value, Class clazz) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class argsTypes[] = { clazz };
		Method setterMtd = o.getClass().getMethod(name, argsTypes );
		Object args[] = { value };
		setterMtd.invoke(o,args);
	}
	public static Object invokeGetterSafe(Object o, String name, boolean forceAccess) {
		try {
			return invokeGetter(o,name,forceAccess);
		} catch (Exception e) {
			return nameOf(o)+"."+name+"() "+nameOf(e)+": "+e.getMessage();
		}
	}
	public static Object invokeGetter(Object o, String name, boolean forceAccess) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Method getterMtd = o.getClass().getMethod(name);
		if (forceAccess) {
			getterMtd.setAccessible(true);
		}
		return getterMtd.invoke(o);
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
	public static Object getFieldValue(Object o, Class c, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return c.getField(name).get(o);
	}
	public static Object getFieldValue(Object o, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return getFieldValue(o, o.getClass(), name);
	}

	public static Object getDeclaredFieldValue(Object o, Class c, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		Field f = c.getDeclaredField(name);
		try {
			f.setAccessible(true);
			return f.get(o);
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			return e.getMessage();
		}
	}
	public static Object getDeclaredFieldValue(Object o, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return getDeclaredFieldValue(o, o.getClass(), name);
	}

	private static void appendFieldsAndMethods(StringBuffer result, Object o, String type, Class c) {
		Field fields[] = c.getDeclaredFields();
		Method methods[] = c.getDeclaredMethods();
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
//				Object value;
//				try {
//					m.setAccessible(true);
//					value=m.invoke(o,null);
//				} catch (Exception e) {
//					value="Could not get value: "+ClassUtils.nameOf(e)+": "+e.getMessage();
//				}
//				result +=": ["+value+"]\n";
				result.append("\n");
			}
			result.append("}");
		}
		result.append("\n");
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

}
