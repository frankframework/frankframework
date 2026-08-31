package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.Description;

public class ControllerDescriptionAnnotationsTest {

	private static final String CONTROLLERS_PACKAGE = "org.frankframework.console.controllers";
	private static final String CONTROLLERS_PACKAGE_PATH = CONTROLLERS_PACKAGE.replace('.', '/');

	@Test
	public void allRequestMappedMethodsShouldHaveDescriptionAnnotation() throws Exception {
		List<String> missingDescriptions = new ArrayList<>();
		int productionRestControllerCount = 0;
		Enumeration<URL> controllerDirectories = Thread.currentThread().getContextClassLoader().getResources(CONTROLLERS_PACKAGE_PATH);
		while (controllerDirectories.hasMoreElements()) {
			URL directory = controllerDirectories.nextElement();
			if ("file".equals(directory.getProtocol())) {
				if (directory.getPath().contains("/test-classes/")) {
					continue;
				}
				productionRestControllerCount += scanFileDirectory(Path.of(directory.toURI()), missingDescriptions);
			} else if ("jar".equals(directory.getProtocol())) {
				productionRestControllerCount += scanJarDirectory(directory, missingDescriptions);
			}
		}

		assertTrue(productionRestControllerCount > 0, "No production rest controllers were discovered for annotation validation");
		missingDescriptions.sort(Comparator.naturalOrder());
		assertTrue(missingDescriptions.isEmpty(), () -> "Request-mapped methods missing @Description: " + missingDescriptions);
	}

	private int scanFileDirectory(Path directory, List<String> missingDescriptions) throws Exception {
		int productionControllerCount = 0;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.class")) {
			for (Path file : files) {
				String className = file.getFileName().toString().replace(".class", "");
				if (className.contains("$")) {
					continue;
				}
				productionControllerCount += inspectControllerClass(className, missingDescriptions);
			}
		}
		return productionControllerCount;
	}

	private int scanJarDirectory(URL directory, List<String> missingDescriptions) throws Exception {
		int productionControllerCount = 0;
		JarURLConnection connection = (JarURLConnection) directory.openConnection();
		String entryPrefix = connection.getEntryName() + "/";
		try (JarFile jarFile = connection.getJarFile()) {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (!entryName.startsWith(entryPrefix) || !entryName.endsWith(".class")) {
					continue;
				}
				String className = entryName.substring(entryPrefix.length()).replace(".class", "");
				if (className.contains("/") || className.contains("$")) {
					continue;
				}
				productionControllerCount += inspectControllerClass(className, missingDescriptions);
			}
		}
		return productionControllerCount;
	}

	private int inspectControllerClass(String className, List<String> missingDescriptions) throws ClassNotFoundException {
		Class<?> controllerClass = Class.forName(CONTROLLERS_PACKAGE + "." + className);
		if (!controllerClass.isAnnotationPresent(RestController.class)) {
			return 0;
		}

		boolean hasRequestMappedMethod = false;
		for (Method method : controllerClass.getDeclaredMethods()) {
			if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
				hasRequestMappedMethod = true;
			}
			if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) && !AnnotatedElementUtils.hasAnnotation(method, Description.class)) {
				missingDescriptions.add(controllerClass.getSimpleName() + "#" + method.toGenericString());
			}
		}
		return hasRequestMappedMethod ? 1 : 0;
	}
}
