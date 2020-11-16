package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class FrankDocModelPopulateTest {
	private FrankDocModel instance;
	private Set<String> actualTypeSimpleNames;
	private Set<String> actualElementSimpleNames;

	@Before
	public void setUp() {
		instance = FrankDocModel.populate();
		actualTypeSimpleNames = instance.getAllTypes().values().stream()
				.map(ElementType::getSimpleName).collect(Collectors.toSet());
		actualElementSimpleNames = instance.getAllElements().values().stream()
				.map(FrankElement::getSimpleName).collect(Collectors.toSet());		
	}

	@Test
	public void testTypeIAdapterCreated() {
		assertTrue(actualTypeSimpleNames.contains("IAdapter"));
	}

	@Test
	public void testElementGenericReceiverCreated() {
		assertTrue(actualElementSimpleNames.contains("Receiver"));
	}
}
