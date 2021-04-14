package nl.nn.adapterframework.frankdoc.doclet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

class TestBase {
	static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.";

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		final List<Object[]> result = new ArrayList<>();
		Arrays.asList(Environment.values()).forEach(v -> result.add(new Object[] {v}));
		return result;
	}

	@Parameter
	public Environment environment;

	FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = environment.getRepository(PACKAGE);
	}
}
