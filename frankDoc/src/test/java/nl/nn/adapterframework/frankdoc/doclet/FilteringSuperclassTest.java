package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FilteringSuperclassTest {
	private static final String CHILD_PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.filtering.second.";
	private static final String PARENT_PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.filtering.first.";
	private static final String CHILD_CLASS = "ChildDerivedFromOtherPackageParent";
	
	private static final String PROP_EXPECTED_SUPERCLASS = "expectedSuperClass";
	private static final String PROP_EXPECTED_METHODS = "expectedMethods";
	private static final Set<String> RELEVANT_METHODS = new HashSet<>(Arrays.asList("setChild", "setParent"));

	private static Map<Boolean, Properties> testSpecifications = new HashMap<>();
	static {
		testSpecifications.put(false, getTestSpecificationsKeepSuperclasses());
		testSpecifications.put(true, getTestSpecificationsOmitSuperclasses());
	}

	private static Properties getTestSpecificationsKeepSuperclasses() {
		Properties p = new Properties();
		p.setProperty(PROP_EXPECTED_SUPERCLASS, "Parent");
		p.setProperty(PROP_EXPECTED_METHODS, RELEVANT_METHODS.stream().sorted().collect(Collectors.joining(",")));
		return p;
	}

	private static Properties getTestSpecificationsOmitSuperclasses() {
		Properties p = new Properties();
		p.setProperty(PROP_EXPECTED_SUPERCLASS, "null");
		p.setProperty(PROP_EXPECTED_METHODS, "setChild");
		return p;
	}

	@Parameters(name = "Environment {0}, all superclasses excluded {1}")
	public static Collection<Object[]> data() {
		final List<Object[]> result = new ArrayList<>();
		for(Environment env: Environment.values()) {
			result.add(new Object[] {env, false});
			result.add(new Object[] {env, true});
		}
		return result;
	}

	@Parameter(0)
	public Environment environment;

	@Parameter(1)
	public boolean omitAllAsSuperclasses;

	private FrankClassRepository repository;
	private FrankClass childClass;

	@Before
	public void setUp() throws FrankDocException {
		List<String> packages = Arrays.asList(CHILD_PACKAGE, PARENT_PACKAGE);
		if(omitAllAsSuperclasses) {
			repository = environment.getRepository(packages, packages, new ArrayList<>(), Arrays.asList(PARENT_PACKAGE));
		} else {
			repository = environment.getRepository(packages, packages, new ArrayList<>(), Arrays.asList(CHILD_PACKAGE));
		}
		childClass = repository.findClass(CHILD_PACKAGE + CHILD_CLASS);
		assertNotNull(childClass);
	}

	@Test
	public void onlyWhenSuperclassNotExcludedThenSuperclassFound() {
		Optional<FrankClass> actualSuperclass = Optional.ofNullable(childClass.getSuperclass());
		String actualSuperclassName = actualSuperclass.map(FrankClass::getSimpleName).orElse("null");
		String expectedSuperclassName = testSpecifications.get(omitAllAsSuperclasses).getProperty(PROP_EXPECTED_SUPERCLASS);
		assertEquals(expectedSuperclassName, actualSuperclassName);
	}

	@Test
	public void onlyWhenSuperclassNotExcludedThenMethodInheritedFromSuperclassFound() {
		// There is no need to filter superclasses when filtering declared and inherited method.
		// Therefore we omit this case from these tests.
		assumeFalse(environment.equals(Environment.REFLECTION) && omitAllAsSuperclasses);
		FrankMethod[] actualMethods = childClass.getDeclaredAndInheritedMethods();
		List<String> actualMethodNames = Arrays.asList(actualMethods).stream()
				.map(FrankMethod::getName)
				.filter(name -> RELEVANT_METHODS.contains(name))
				.sorted()
				.collect(Collectors.toList());
		String[] expectedMethodNames = testSpecifications.get(omitAllAsSuperclasses).getProperty(PROP_EXPECTED_METHODS).split(",");
		assertArrayEquals(expectedMethodNames, actualMethodNames.toArray(new String[] {}));
	}
}
