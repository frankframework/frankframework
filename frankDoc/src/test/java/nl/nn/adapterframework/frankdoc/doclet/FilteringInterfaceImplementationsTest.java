package nl.nn.adapterframework.frankdoc.doclet;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javadoc.ClassDoc;

@RunWith(Parameterized.class)
public class FilteringInterfaceImplementationsTest {
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

	@Parameters(name = "{0}, {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"First package no exclude", asList(FIRST_PACKAGE), asList(NO_EXCLUDES), new String[] {FIRST_IMPL}},
			{"First package with exclude", asList(FIRST_PACKAGE), asList(THIRD_IMPL_EXCLUDED), new String[] {FIRST_IMPL}},
			{"Both packages no exclude", asList(BOTH_PACKAGES), asList(NO_EXCLUDES), new String[] {CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE, FIRST_IMPL, SECOND_IMPL, THIRD_IMPL}},
			{"Both packages with exclude", asList(BOTH_PACKAGES), asList(THIRD_IMPL_EXCLUDED), new String[] {CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE, FIRST_IMPL,SECOND_IMPL}},
			{"Both packages derived class of implementation excluded", asList(BOTH_PACKAGES), asList(CHILD_OF_FIRST_IMPL_IN_SECOND_PACKAGE_EXCLUDED), new String[] {FIRST_IMPL, SECOND_IMPL, THIRD_IMPL}}			
		});
	}

	@Parameter(0)
	public String caseName;

	@Parameter(1)
	public List<String> includes;

	@Parameter(2)
	public List<String> excludes;

	@Parameter(3)
	public String[] expectedImplementations;

	@Test
	public void test() throws FrankDocException {
		ClassDoc[] classDocs = TestUtil.getClassDocs(BOTH_PACKAGES);
		FrankClassRepository repository = FrankClassRepository.getDocletInstance(classDocs, new HashSet<>(includes), new HashSet<>(excludes), new HashSet<>());
		FrankClass clazz = repository.findClass(FIRST_PACKAGE + "MyInterface");
		List<FrankClass> implementations = clazz.getInterfaceImplementations();
		List<String> actualSimpleNames = implementations.stream()
				.map(FrankClass::getSimpleName)
				.sorted()
				.collect(Collectors.toList());
		assertArrayEquals(expectedImplementations, actualSimpleNames.toArray(new String[] {}));
	}
}
