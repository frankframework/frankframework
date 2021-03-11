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
package nl.nn.adapterframework.cmdline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import nl.nn.adapterframework.doc.DocWriterNew;
import nl.nn.adapterframework.doc.XsdVersion;
import nl.nn.adapterframework.doc.model.FrankDocModel;

public class GenerateFrankDoc {

	public static void main(String[] args) throws IOException {//arg1 = XsdVersion, arg2 = outputDirectory
		String coreModuleDirectory = System.getProperty("maven.multiModuleProjectDirectory");
		File directory = new File(coreModuleDirectory);
		if(!coreModuleDirectory.endsWith("core") || !directory.isDirectory()) {
			throw new IllegalStateException("Invalid module directory ["+coreModuleDirectory+"]");
		}

		File logDir = new File(directory, "/target/logs/frankdoc");
		log("Using log.dir ["+logDir.getAbsolutePath()+"]");
		System.setProperty("log.dir", logDir.getAbsolutePath());

		String xsdVersions = XsdVersion.STRICT.name();
		File outputDirectory = new File(directory, "/target/classes/xml/xsd");
		if(args.length == 1) {
			xsdVersions = args[0];
		}
		else if(args.length == 2) {
			xsdVersions = args[0];

			outputDirectory = new File(args[1]);
		}
		outputDirectory.mkdirs(); //Try and create the directory if it doesn't exist.
		if(!outputDirectory.isDirectory()) {
			throw new IllegalStateException("Invalid outputDirectory ["+outputDirectory+"]");
		}

		for(String version : xsdVersions.split(",")) {
			try {
				XsdVersion xsdVersion = XsdVersion.valueOf(version);
				generateFrankDoc(xsdVersion, outputDirectory);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void generateFrankDoc(XsdVersion version, File outputDirectory) throws IOException {
		long startTime = System.currentTimeMillis();
		log("Generating Frank!Doc - xsdVersion ["+version.name()+"] outputDirectory ["+outputDirectory+"]");

		FrankDocModel model = FrankDocModel.populate();
		DocWriterNew docWriter = new DocWriterNew(model);
		docWriter.init(version);

		File output = new File(outputDirectory, "FrankConfig-" + docWriter.getOutputFileName());
		try(Writer writer = new BufferedWriter(new FileWriter(output))) {
			writer.append(docWriter.getSchema());
		}

		long duration = System.currentTimeMillis() - startTime;
		log("Generated ["+version.name()+"] Frank!Doc in ["+duration+"] seconds");
	}

	private static void log(String log) {
		System.out.println("[INFO] "+log);
	}
}
