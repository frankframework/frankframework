package nl.nn.adapterframework.configuration;

import java.net.URL;

public class BasePathClassLoader extends ClassLoader {
	private String basePath;
	
	public BasePathClassLoader(ClassLoader parent, String basePath) {
		super(parent);
		this.basePath = basePath;
	}

	@Override
	public URL getResource(String name) {
		URL url = getParent().getResource(basePath + name);
		if (url != null) {
			return url;
		} else {
			return getParent().getResource(name);
		}
	}

}
