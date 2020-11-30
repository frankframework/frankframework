package nl.nn.adapterframework.doc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import nl.nn.adapterframework.doc.objects.ChildIbisBeanMapping;

public class TestDigesterRulesParser {

	@Test
	public void testParser() throws Exception {
		List<ChildIbisBeanMapping> beanList = InfoBuilderSource.getChildIbisBeanMappings();
		assertTrue(beanList.size() >= 30); //Expect to find at least 33 pattern mappings

		ChildIbisBeanMapping mapping = beanList.get(0);
		assertNotNull(mapping.getMethodName()); //Should never be null
		assertNotNull(mapping.getChildIbisBeanName()); //Should never be null
	}
}
