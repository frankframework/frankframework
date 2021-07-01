package nl.nn.adapterframework.frankdoc.doclet.classdocs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javadoc.JavadocTool;
import com.sun.tools.javadoc.ModifierFilter;

import nl.nn.adapterframework.util.LogUtil;

public class EasyDoclet {
	private static Logger log = LogUtil.getLogger(EasyDoclet.class);

	final private File sourceDirectory;
	final private String[] packageNames;
	final private RootDoc rootDoc;

	public EasyDoclet(File sourceDirectory, String[] packageNames) {
		this.sourceDirectory = sourceDirectory;
		this.packageNames = packageNames;

		Context context = new Context();
		Options compOpts = Options.instance(context);

		if (getSourceDirectory().exists()) {
			log.info("Using source path: " + getSourceDirectory().getAbsolutePath());
			compOpts.put("-sourcepath", getSourceDirectory().getAbsolutePath());
		} else {
			log.info("Ignoring non-existant source path, check your source directory argument");
		}

		ListBuffer<String> javaNames = new ListBuffer<String>();

		ListBuffer<String> subPackages = new ListBuffer<String>();
		for (String packageName : packageNames) {
			log.info("Adding sub-packages to documentation path: " + packageName);
			subPackages.append(packageName);
		}

		new PublicMessager(context, getApplicationName(), new PrintWriter(new LogWriter(Level.ERROR), true),
				new PrintWriter(new LogWriter(Level.WARN), true), new PrintWriter(new LogWriter(Level.INFO), true));

		JavadocTool javadocTool = JavadocTool.make0(context);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			throw new RuntimeException("No Java compiler available, maybe you are using a JRE instead of a JDK");
		}
		try {
			Iterable<? extends JavaFileObject> files = new ArrayList<>();
			rootDoc = javadocTool.getRootDocImpl(Locale.ENGLISH.getDisplayCountry(), "UTF-8", new ModifierFilter(Modifier.PUBLIC),
					javaNames.toList(), new ListBuffer<String[]>().toList(), files, false, subPackages.toList(), new ListBuffer<String>().toList(),
					false, false, false);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

		if (log.isDebugEnabled()) {
			for (ClassDoc classDoc : getRootDoc().classes()) {
				log.debug("Parsed Javadoc class source: " + classDoc.position() + " with inline tags: " + classDoc.inlineTags().length);
			}
		}
	}

	public File getSourceDirectory() {
		return sourceDirectory;
	}

	public String[] getPackageNames() {
		return packageNames;
	}

	public RootDoc getRootDoc() {
		return rootDoc;
	}

	protected class LogWriter extends Writer {

		Level level;

		public LogWriter(Level level) {
			this.level = level;
		}

		public void write(char[] chars, int offset, int length) throws IOException {
			String s = new String(Arrays.copyOf(chars, length));
			if (!s.equals("\n"))
				log.info(s);
		}

		public void flush() throws IOException {
		}

		public void close() throws IOException {
		}
	}

	protected String getApplicationName() {
		return getClass().getSimpleName() + " Application";
	}

}