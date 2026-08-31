package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

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
		int productionControllerCount = 0;
		Enumeration<URL> controllerDirectories = Thread.currentThread().getContextClassLoader().getResources(CONTROLLERS_PACKAGE_PATH);
		while (controllerDirectories.hasMoreElements()) {
			URL directory = controllerDirectories.nextElement();
			if (!"file".equals(directory.getProtocol())) {
				continue;
			}
			if (directory.getPath().contains("/test-classes/")) {
				continue;
			}

			try (DirectoryStream<Path> files = Files.newDirectoryStream(Path.of(directory.toURI()), "*.class")) {
				for (Path file : files) {
					String className = file.getFileName().toString().replace(".class", "");
					if (className.contains("$")) {
						continue;
					}
					Class<?> controllerClass = Class.forName(CONTROLLERS_PACKAGE + "." + className);
					if (!controllerClass.isAnnotationPresent(RestController.class)) {
						continue;
					}
					productionControllerCount++;

					for (Method method : controllerClass.getDeclaredMethods()) {
						if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) && !method.isAnnotationPresent(Description.class)) {
							missingDescriptions.add(controllerClass.getSimpleName() + "#" + method.getName());
						}
					}
				}
			}
		}

		assertTrue(productionControllerCount > 0, "No production controllers were discovered for annotation validation");
		missingDescriptions.sort(Comparator.naturalOrder());
		assertTrue(missingDescriptions.isEmpty(), () -> "Request-mapped methods missing @Description: " + missingDescriptions);
	}
}
