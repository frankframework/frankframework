package nl.nn.adapterframework.doc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javadoc.JavadocTool;
import com.sun.tools.javadoc.ModifierFilter;

public class EasyDoclet {

	final private Logger log = Logger.getLogger(EasyDoclet.class.getName());

	final private File sourceDirectory;
	final private String[] packageNames;
	final private File[] fileNames;
	final private RootDoc rootDoc;

	public EasyDoclet(File sourceDirectory, String... packageNames) {
		this(sourceDirectory, packageNames, new File[0]);
	}

	public EasyDoclet(File sourceDirectory, File... fileNames) {
		this(sourceDirectory, new String[0], fileNames);
	}

	static File docDir = new File("C:\\Users\\Niels\\Git\\iaf-example\\core\\src\\test\\java\\nl\\nn\\adapterframework\\doc");
	static File testSrc = new File("C:\\Users\\Niels\\Git\\iaf-example\\core\\src\\test\\java\\nl\\nn\\adapterframework\\doc\\FrankDoclet.java");

	public EasyDoclet() {
		this(new File("C:\\Users\\Niels\\Git\\iaf-example\\core\\src\\test\\java\\nl\\nn\\adapterframework"), new String[0], new File[] {testSrc});
	}

	protected EasyDoclet(File sourceDirectory, String[] packageNames, File[] fileNames) {
		this.sourceDirectory = sourceDirectory;
		this.packageNames = packageNames;
		this.fileNames = fileNames;

		Context context = new Context();
		Options compOpts = Options.instance(context);

		if (getSourceDirectory().exists()) {
			log.info("Using source path: " + getSourceDirectory().getAbsolutePath());
			compOpts.put("-sourcepath", getSourceDirectory().getAbsolutePath());
		} else {
			log.info("Ignoring non-existant source path, check your source directory argument");
		}

		ListBuffer<String> javaNames = new ListBuffer<String>();
		for (File fileName : fileNames) {
//			log.info("Adding file to documentation path: " + fileName.getAbsolutePath());
//			javaNames.append(fileName.getPath());
//			javaNames.append(CmdTest.class.getCanonicalName());
//			javaNames.append("src/test/java/nl/nn/adapterframework/doc/FrankDoclet.java");
		}
		File fff = new File(docDir, "FrankDoclet.java");
		javaNames.append(fff.getPath());

		ListBuffer<String> subPackages = new ListBuffer<String>();
		for (String packageName : packageNames) {
			log.info("Adding sub-packages to documentation path: " + packageName);
			subPackages.append(packageName);
		}

		new PublicMessager(context, getApplicationName(), new PrintWriter(new LogWriter(Level.SEVERE), true),
				new PrintWriter(new LogWriter(Level.WARNING), true), new PrintWriter(new LogWriter(Level.INFO), true));

		JavadocTool javadocTool = JavadocTool.make0(context);
		

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fm = compiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("UTF-8"));

		try {
//			List<JavaClassObject> tralala = new ArrayList<>();
//			tralala.add(new JavaClassObject("nl.nn.adapterframework.doc.CmdTest", Kind.SOURCE));
			File sadfasfas = new File("C:\\Users\\Niels\\Git\\iaf-example\\core\\src\\test\\java\\nl\\nn\\adapterframework\\doc\\EasyDoclet.java");
			Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(sadfasfas);
//			System.out.println(files);
//			files = new ListBuffer<JavaFileObject>().toList();
			rootDoc = javadocTool.getRootDocImpl(Locale.ENGLISH.getDisplayCountry(), "UTF-8", new ModifierFilter(ModifierFilter.ALL_ACCESS),
					javaNames.toList(), new ListBuffer<String[]>().toList(), files, false, subPackages.toList(),
					new ListBuffer<String>().toList(), false, false, false);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

		if (log.isLoggable(Level.FINEST)) {
			for (ClassDoc classDoc : getRootDoc().classes()) {
				log.finest("Parsed Javadoc class source: " + classDoc.position() + " with inline tags: "
						+ classDoc.inlineTags().length);
			}
		}
	}


	public File getSourceDirectory() {
		return sourceDirectory;
	}

	public String[] getPackageNames() {
		return packageNames;
	}

	public File[] getFileNames() {
		return fileNames;
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
				System.out.println(s);
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