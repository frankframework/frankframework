package nl.nn.adapterframework.frankdoc.doclet.classdocs;

import java.io.PrintWriter;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javadoc.Messager;

public class PublicMessager extends Messager {
	public PublicMessager(Context context, String s) {
		super(context, s);
	}

	public PublicMessager(Context context, String s, PrintWriter printWriter, PrintWriter printWriter1, PrintWriter printWriter2) {
		super(context, s, printWriter, printWriter1, printWriter2);
	}
}
