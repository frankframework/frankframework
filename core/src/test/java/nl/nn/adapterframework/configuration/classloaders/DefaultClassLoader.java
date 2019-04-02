package nl.nn.adapterframework.configuration.classloaders;

/**
 * Depending on the JKD this grabs the default ClassLoader
 * Does not implement the IClassLoader interface
 * 
 * @author Niels Meijer
 *
 */
public class DefaultClassLoader extends ClassLoader {

	public DefaultClassLoader() {
		super();
	}

	public DefaultClassLoader(ClassLoader parent) {
		super(parent);
	}
}
