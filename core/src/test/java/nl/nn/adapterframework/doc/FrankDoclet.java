package nl.nn.adapterframework.doc;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

/**
 * class doc
 * @author Niels
 *
 */
public class FrankDoclet {

	/**
	 * field doc
	 * @param root
	 * @return
	 */
	public static boolean start(RootDoc root) {
		System.out.println("I'm Frank!Doc");
		ClassDoc[] classes = root.classes();
		for (int i = 0; i < classes.length; ++i) {
			System.out.println(classes[i]);
		}
		return true;
	}
}
