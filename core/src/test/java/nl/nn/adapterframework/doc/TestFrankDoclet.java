package nl.nn.adapterframework.doc;

import org.junit.Test;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

public class TestFrankDoclet {

	@Test
	public void test() throws Exception {
		EasyDoclet doclet = new EasyDoclet();
		RootDoc doc = doclet.getRootDoc();
		ClassDoc[] classes = doc.classes();
		if(classes.length > 1) {
			System.out.println(classes[0]);
			System.out.println(classes[0].commentText());
		}
		else {
			System.out.println("no classes");
		}
	}
}
