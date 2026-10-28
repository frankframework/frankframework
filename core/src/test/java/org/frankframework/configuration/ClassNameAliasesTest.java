package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.filters.ClassNameAliases;

public class ClassNameAliasesTest {

	@Test
	public void mergesEveryFileAndKeepsTheFirstMappingOfAName() throws Exception {
		URL first = getClass().getResource("/ClassNameAliases/first/");
		URL second = getClass().getResource("/ClassNameAliases/second/");
		try (URLClassLoader loader = new URLClassLoader(new URL[] { first, second }, null)) {
			ClassNameAliases aliases = ClassNameAliases.load(loader);

			assertEquals(Optional.of("org.example.First"), aliases.target("org.example.Old"));
			assertEquals(Optional.of("org.example.A"), aliases.target("org.example.OnlyInFirst"));
			assertEquals(Optional.of("org.example.B"), aliases.target("org.example.OnlyInSecond"));
			assertEquals(Optional.empty(), aliases.target("org.example.Unknown"));
			assertFalse(aliases.isEmpty());
		}
	}

	@Test
	public void noFilesMeansNoAliases() throws Exception {
		try (URLClassLoader loader = new URLClassLoader(new URL[0], null)) {
			ClassNameAliases aliases = ClassNameAliases.load(loader);
			assertTrue(aliases.isEmpty());
			assertEquals(Optional.empty(), aliases.target("org.example.Old"));
		}
	}

	@Test
	public void theFrameworkInstanceReadsTheClassPath() {
		assertEquals(Optional.of("org.frankframework.pipes.EchoPipe"),
				ClassNameAliases.getInstance().target("org.frankframework.test.aliases.OldEchoPipe"));
	}
}
