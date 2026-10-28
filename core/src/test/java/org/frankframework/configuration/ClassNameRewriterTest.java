package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.filters.ClassNameAliases;
import org.frankframework.configuration.filters.ClassNameRewriter;
import org.frankframework.pipes.EchoPipe;
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

		assertEquals(expected, configuration.getConfigurationWarnings().getWarnings().getFirst());
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

	@Test
	public void aliasIsRewrittenWithADeprecationWarning() throws Exception {
		Configuration configuration = new TestConfiguration();
		String result = rewrite(new ClassNameRewriter(new XmlWriter(), configuration), "org.frankframework.test.aliases.OldEchoPipe");

		MatchUtils.assertXmlEquals(ELEMENT_NAME_TEMPLATE.formatted(EchoPipe.class.getCanonicalName()), result);
		assertEquals("[org.frankframework.test.aliases.OldEchoPipe] has been renamed to [org.frankframework.pipes.EchoPipe]. Please change the className attribute.",
				configuration.getConfigurationWarnings().getWarnings().getFirst());
	}

	@Test
	public void aliasIsAppliedWhenLegacyRewritingIsSwitchedOff() throws Exception {
		Configuration configuration = new TestConfiguration();
		ClassNameRewriter handler = new ClassNameRewriter(new XmlWriter(), configuration, false, ClassNameAliases.getInstance());

		MatchUtils.assertXmlEquals(ELEMENT_NAME_TEMPLATE.formatted(EchoPipe.class.getCanonicalName()),
				rewrite(handler, "org.frankframework.test.aliases.OldEchoPipe"));
		// ...while the legacy package rewrite is left alone
		MatchUtils.assertXmlEquals(ELEMENT_NAME_TEMPLATE.formatted("nl.nn.adapterframework.pipes.EchoPipe"),
				rewrite(new ClassNameRewriter(new XmlWriter(), configuration, false, ClassNameAliases.getInstance()), "nl.nn.adapterframework.pipes.EchoPipe"));
	}

	@Test
	public void aliasToAClassThatCannotBeLoadedKeepsTheOriginalName() throws Exception {
		Configuration configuration = new TestConfiguration();
		String result = rewrite(new ClassNameRewriter(new XmlWriter(), configuration), "org.frankframework.test.aliases.OldMissingPipe");

		MatchUtils.assertXmlEquals(ELEMENT_NAME_TEMPLATE.formatted("org.frankframework.test.aliases.OldMissingPipe"), result);
		String warning = configuration.getConfigurationWarnings().getWarnings().getFirst();
		assertTrue(warning.contains("org.frankframework.test.aliases.DoesNotExist"), warning);
	}

	@Test
	public void nameWithoutAliasIsUntouched() throws Exception {
		Configuration configuration = new TestConfiguration();
		String result = rewrite(new ClassNameRewriter(new XmlWriter(), configuration), EchoPipe.class.getCanonicalName());

		MatchUtils.assertXmlEquals(ELEMENT_NAME_TEMPLATE.formatted(EchoPipe.class.getCanonicalName()), result);
		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty());
	}

	private static String rewrite(ClassNameRewriter handler, String className) throws Exception {
		XmlWriter writer = (XmlWriter) handler.getContentHandler();
		XmlUtils.parseXml(ELEMENT_NAME_TEMPLATE.formatted(className), handler);
		return writer.toString();
	}
}
