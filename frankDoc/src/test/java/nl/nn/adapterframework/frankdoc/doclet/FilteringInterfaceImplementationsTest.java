package nl.nn.adapterframework.frankdoc.doclet;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FilteringInterfaceImplementationsTest {
	private static final int NUM_PARAMETERS = 5;

	private static final String PREFIX = "nl.nn.adapterframework.frankdoc.testtarget.doclet.filtering.";
	private static final String FIRST = "first.";
	private static final String SECOND = "second.";
	private static final String FIRST_PACKAGE = PREFIX + FIRST;
	private static final String SECOND_PACKAGE = PREFIX + SECOND;
	private static final String[] BOTH_PACKAGES = new String[] {FIRST_PACKAGE, SECOND_PACKAGE};
	private static final String FIRST_IMPL = "FirstImpl";
	private static final String CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE = "ChildOfFirstImplInOtherPackage";
	private static final String SECOND_IMPL = "SecondImpl";
	private static final String THIRD_IMPL = "ThirdImpl";
	private static final String[] NO_EXCLUDES = new String[] {};
	private static final String[] THIRD_IMPL_EXCLUDED = new String[] {SECOND_PACKAGE + THIRD_IMPL};
	private static final String[] CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE_EXCLUDED = new String[] {SECOND_PACKAGE + CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE};

	private static Collection<Object[]> cases = Arrays.asList(new Object[][] {
		{"First package no exclude", asList(FIRST_PACKAGE), asList(NO_EXCLUDES), new String[] {FIRST_IMPL}},
		{"First package with exclude", asList(FIRST_PACKAGE), asList(THIRD_IMPL_EXCLUDED), new String[] {FIRST_IMPL}},
		{"Both packages no exclude", asList(BOTH_PACKAGES), asList(NO_EXCLUDES), new String[] {CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE, FIRST_IMPL, SECOND_IMPL, THIRD_IMPL}},
		{"Both packages with exclude", asList(BOTH_PACKAGES), asList(THIRD_IMPL_EXCLUDED), new String[] {CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE, FIRST_IMPL,SECOND_IMPL}},
		{"Both packages derived class of implementation excluded", asList(BOTH_PACKAGES), asList(CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE_EXCLUDED), new String[] {FIRST_IMPL, SECOND_IMPL, THIRD_IMPL}}});

	@Parameters(name = "{0}, {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> result = new ArrayList<>();
		for(Environment environment: Environment.values()) {
			for(Object[] c: cases) {
				Object[] row = new Object[NUM_PARAMETERS];
				row[0] = environment;
				for(int i = 0; i < 4; ++i) {
					row[i+1] = c[i];
				}
				result.add(row);
			}
		}
		return result;
	}

	@Parameter(0)
	public Environment environment;

	@Parameter(1)
	public String caseName;

	@Parameter(2)
	public List<String> includes;

	@Parameter(3)
	public List<String> excludes;

	@Parameter(4)
	public String[] expectedImplementations;

	@Test
	public void test() throws FrankDocException {
		FrankClassRepository repository = environment.getRepository(asList(BOTH_PACKAGES), includes, excludes, new ArrayList<>());
		FrankClass clazz = repository.findClass(FIRST_PACKAGE + "MyInterface");
		List<FrankClass> implementations = clazz.getInterfaceImplementations();
		List<String> actualSimpleNames = implementations.stream()
				.map(FrankClass::getSimpleName)
				.sorted()
				.collect(Collectors.toList());
		assertArrayEquals(expectedImplementations, actualSimpleNames.toArray(new String[] {}));
	}
}
