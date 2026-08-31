package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.Description;

public class ControllerDescriptionAnnotationsTest {

	private static final String CONTROLLERS_PACKAGE = "org.frankframework.console.controllers";

	@Test
	public void allRequestMappedMethodsShouldHaveDescriptionAnnotation() throws Exception {
		Path controllersDirectory = Path.of("src/main/java/org/frankframework/console/controllers");
		List<String> missingDescriptions = new ArrayList<>();
		try (Stream<Path> files = Files.list(controllersDirectory)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String className = file.getFileName().toString().replace(".java", "");
				Class<?> controllerClass = Class.forName(CONTROLLERS_PACKAGE + "." + className);
				if (!controllerClass.isAnnotationPresent(RestController.class)) {
					continue;
				}

				for (Method method : controllerClass.getDeclaredMethods()) {
					if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) && !method.isAnnotationPresent(Description.class)) {
						missingDescriptions.add(controllerClass.getSimpleName() + "#" + method.getName());
					}
				}
			}
		}

		missingDescriptions.sort(Comparator.naturalOrder());
		assertTrue(missingDescriptions.isEmpty(), () -> "Request-mapped methods missing @Description: " + missingDescriptions);
	}
}
