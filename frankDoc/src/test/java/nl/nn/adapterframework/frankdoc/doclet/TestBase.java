package nl.nn.adapterframework.frankdoc.doclet;

import org.junit.Before;

class TestBase {
	static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.";

	FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
	}
}
