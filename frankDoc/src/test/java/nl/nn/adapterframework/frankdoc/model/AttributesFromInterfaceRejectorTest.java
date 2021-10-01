package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
public class AttributesFromInterfaceRejectorTest {
	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"nl.nn.adapterframework.frankdoc.testtarget.reject.simple.", "ISuperseeded", new String[] {"rejectedAttribute"}},
			{"nl.nn.adapterframework.frankdoc.testtarget.reject.simple2.", "IIgnored", new String[] {"attributeIIgnored"}},
			{"nl.nn.adapterframework.frankdoc.testtarget.reject.complex.", "ISuperseded", new String[] {"superseded3"}},
			{"nl.nn.adapterframework.frankdoc.testtarget.reject.complex2.", "ISuperseded", new String[] {"superseded3"}}
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
		FrankClass excludedInterface = classRepository.findClass(excludedInterfaceFullName);
		AttributesFromInterfaceRejector instance = new AttributesFromInterfaceRejector(excludedInterface);
		List<String> actualAttributes = new ArrayList<>(instance.getRejects(clazz));
		Collections.sort(actualAttributes);
		assertArrayEquals(expectedAttributes, actualAttributes.toArray(new String[] {}));
	}
}
