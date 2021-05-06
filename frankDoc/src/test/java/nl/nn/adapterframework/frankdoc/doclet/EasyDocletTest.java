package nl.nn.adapterframework.frankdoc.doclet;

import org.junit.Ignore;
import org.junit.Test;

import com.sun.javadoc.ClassDoc;

public class EasyDocletTest {
	@Ignore
	@Test
	public void test() throws Exception {
		for(ClassDoc c: TestUtil.getClassDocs("nl.nn.adapterframework.frankdoc.testtarget.doclet")) {
			System.out.println(c.name());
		}
	}
}
