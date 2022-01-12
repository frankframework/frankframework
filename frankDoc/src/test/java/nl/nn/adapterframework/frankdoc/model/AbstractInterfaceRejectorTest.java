package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

@RunWith(Parameterized.class)
public class AbstractInterfaceRejectorTest {
	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"nl.nn.adapterframework.frankdoc.testtarget.reject.simple.", "ISuperseeded", new String[] {"rejectedAttribute"}},
			{"nl.nn.adapterframework.frankdoc.testtarget.reject.simple2.", "IIgnored", new String[] {"attributeIIgnored"}}
		});
	}

	@Parameter(0)
	public String thePackage;

	@Parameter(1)
	public String excludedInterface;

	@Parameter(2)
	public String[] expectedAttributes;

	private FrankClassRepository classRepository;

	@Test
	public void testAttributeRejectionOnChild() throws Exception {
		doAttributeRejectionTest("Child");
	}

	@Test
	public void testAttributeRejectionOnGrandChild() throws Exception {
		doAttributeRejectionTest("GrandChild");
	}

	private void doAttributeRejectionTest(String inputClass) throws FrankDocException {
		classRepository = TestUtil.getFrankClassRepositoryDoclet(thePackage);
		FrankClass clazz = classRepository.findClass(thePackage + inputClass);
		String excludedInterfaceFullName = thePackage + excludedInterface;
		AttributesFromInterfaceRejector instance = new AttributesFromInterfaceRejector(excludedInterfaceFullName);
		List<String> actualAttributes = new ArrayList<>(instance.getRejects(clazz));
		Collections.sort(actualAttributes);
		assertArrayEquals(expectedAttributes, actualAttributes.toArray(new String[] {}));
	}

	@Test
	public void testTypeIgnoreOnChild() throws Exception {
		doTypeIngoreTest("Child");
	}

	@Test
	public void testTypeIgnoreOnGrandChild() throws Exception {
		doTypeIngoreTest("GrandChild");
	}

	private void doTypeIngoreTest(String inputClass) throws FrankDocException {
		classRepository = TestUtil.getFrankClassRepositoryDoclet(thePackage);
		FrankClass clazz = classRepository.findClass(thePackage + inputClass);
		String excludedInterfaceFullName = thePackage + excludedInterface;
		GroupFromInterfaceRejector instance = new GroupFromInterfaceRejector(new HashSet<>(Arrays.asList(excludedInterfaceFullName)));
		List<String> actualGroupRejections = new ArrayList<>(instance.getRejects(clazz));
		assertEquals(1, actualGroupRejections.size());
		assertEquals(excludedInterfaceFullName, actualGroupRejections.get(0));
	}
}
