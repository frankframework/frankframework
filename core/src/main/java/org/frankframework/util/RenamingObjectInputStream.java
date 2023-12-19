package org.frankframework.util;

import static org.frankframework.util.ClassUtils.loadClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RenamingObjectInputStream extends ObjectInputStream {

	public RenamingObjectInputStream(InputStream in) throws IOException {
		super(in);
	}

	// Support old package names, already serialized in the database
	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

		String className = resultClassDescriptor.getName();
		if (className.startsWith("nl.nn.adapterframework")) {
			String newClassName = className.replace("nl.nn.adapterframework", "org.frankframework");
			Class<?> newClass = null;
			try {
				newClass = loadClass(newClassName);
				return ObjectStreamClass.lookup(newClass);
			} catch (ClassNotFoundException e) {
				log.warn("Found old class [{}] in ObjectInputStream but cannot load class with new name [{}]", className, newClassName, e);
			}
		}
		return resultClassDescriptor;
	}
}
