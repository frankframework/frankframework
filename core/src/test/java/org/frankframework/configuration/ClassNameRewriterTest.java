package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.filters.ClassNameRewriter;
import org.frankframework.pipes.GetFromSessionPipe;
import org.frankframework.pipes.JsonWellFormedCheckerPipe;
import org.frankframework.pipes.PutInSessionPipe;
import org.frankframework.pipes.RemoveFromSessionPipe;
import org.frankframework.pipes.XmlWellFormedCheckerPipe;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

public class ClassNameRewriterTest {
	private static final String ELEMENT_NAME_TEMPLATE = "<pipe className=\"%s\" />";

	@ParameterizedTest
	@MethodSource
	public void testImplicitClassName(String originalClassName, Class<?> expectedClass) throws Exception {
		Configuration configuration = new TestConfiguration();
		String expectedClassName = expectedClass.getCanonicalName();

		XmlWriter writer = new XmlWriter();
		ClassNameRewriter handler = new ClassNameRewriter(writer, configuration);

		XmlUtils.parseXml(ELEMENT_NAME_TEMPLATE.formatted(originalClassName), handler);
		String result = writer.toString();
		MatchUtils.assertXmlEquals(ELEMENT_NAME_TEMPLATE.formatted(expectedClassName), result);

		String newClassName = originalClassName.replace(ClassNameRewriter.LEGACY_PACKAGE_NAME, ClassNameRewriter.ORG_FRANKFRAMEWORK_PACKAGE_NAME);
		String expected = "[%s] has been renamed to [%s]. Please use the new syntax or change the className attribute."
				.formatted(newClassName, expectedClassName);

		assertEquals(expected, configuration.getConfigurationWarnings().get(0));
	}

	public static Stream<Arguments> testImplicitClassName() {
		return Stream.of(
			Arguments.of("nl.nn.adapterframework.pipes.PutInSession", PutInSessionPipe.class),
			Arguments.of("org.frankframework.pipes.PutInSession", PutInSessionPipe.class),
			Arguments.of("org.frankframework.pipes.RemoveFromSession", RemoveFromSessionPipe.class),
			Arguments.of("org.frankframework.pipes.GetFromSession", GetFromSessionPipe.class),
			Arguments.of("org.frankframework.pipes.XmlWellFormedChecker", XmlWellFormedCheckerPipe.class),
			Arguments.of("org.frankframework.pipes.JsonWellFormedChecker", JsonWellFormedCheckerPipe.class)
		);
	}
}
