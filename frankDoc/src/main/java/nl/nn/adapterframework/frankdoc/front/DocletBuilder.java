/* 
Copyright 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/

package nl.nn.adapterframework.frankdoc.front;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.util.LogUtil;

public class DocletBuilder extends com.sun.javadoc.Doclet {
	private static final Logger log = LogUtil.getLogger(DocletBuilder.class);

    public static boolean start(RootDoc root) {
    	printOptions(root);
    	ClassDoc classes[] = root.classes();
    	boolean result = true;
    	try {
        	FrankDocletOptions options = FrankDocletOptions.getInstance(root.options());
    		new Doclet(classes, options).run();
    	}
    	catch(RuntimeException e) {
    		e.printStackTrace();
    		result = false;
    	}
    	catch(FrankDocException e) {
    		log.error("FrankDocException occurred while running Frank!Doc Doclet", e);
    		result = false;
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
    	return FrankDocletOptions.optionLength(option);
    }

    public static boolean validOptions(String options[][], DocErrorReporter reporter) {
    	try {
    		FrankDocletOptions.validateOptions(options);
    		return true;
    	} catch(InvalidDocletOptionsException e) {
    		reporter.printError(e.getMessage());
    		return false;
    	}
    }

    public static LanguageVersion languageVersion() {
    	log.trace("Method languageVersion() called");
    	return LanguageVersion.JAVA_1_5;
    }
}