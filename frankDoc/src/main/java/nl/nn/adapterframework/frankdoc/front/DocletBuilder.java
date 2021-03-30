package nl.nn.adapterframework.frankdoc.front;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;

import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.util.LogUtil;

public class DocletBuilder extends com.sun.javadoc.Doclet {
	private static final Logger log = LogUtil.getLogger(DocletBuilder.class);

	private static final String OPT_OUTPUT_DIR = "-outputDir";
	private static String outputDir = null;

    public static boolean start(RootDoc root) {
    	printOptions(root);
    	ClassDoc classes[] = root.classes();
    	boolean result = true;
    	try {
    		new Doclet(classes, outputDir).run();
    	}
    	catch(RuntimeException e) {
    		e.printStackTrace();
    		result = false;
    	}
    	catch(FrankDocException e) {
    		log.error("FrankDocException occurred while running Frank!Doc Doclet", e);
    	}
    	return result;
    }

	private static void printOptions(RootDoc root) {
		log.info("Here are the options given to the Frank!Doc doclet");
    	String[][] options = root.options();
    	for(int i = 0; i < options.length; i++) {
    		String[] curOption = options[i];
    		String line = "";
    		for(int j = 0; j < curOption.length; j++) {
    			if(j != 0) {
    				line += " ";
    			}
    			line += curOption[j];
    		}
    		log.info(line);
    	}
    	log.info("End of options given to Frank!Doc doclet");
	}

    public static int optionLength(String option) {
        if(option.equals(OPT_OUTPUT_DIR)) {
            return 2;
        }
        return 0;
    }

    public static boolean validOptions(String options[][], DocErrorReporter reporter) {
        boolean foundOutputDirOption = false;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (opt[0].equals(OPT_OUTPUT_DIR)) {
                if (foundOutputDirOption) {
                    reporter.printError(String.format("Only one %s option allowed.", OPT_OUTPUT_DIR));
                    return false;
                } else { 
                    foundOutputDirOption = true;
                    outputDir = opt[1];
                }
            } 
        }
        if (!foundOutputDirOption) {
            reporter.printError(String.format("Option %s is missing. Please add it like "
            		+ "<additionalOptions> %s directory-to-store-frank-doc-in</additionalOptions>"
            		+ " in the pom.xml",
            		OPT_OUTPUT_DIR, OPT_OUTPUT_DIR));
        }
        return foundOutputDirOption;
    }
}