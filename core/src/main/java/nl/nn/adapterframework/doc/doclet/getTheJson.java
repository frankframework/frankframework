package nl.nn.adapterframework.doc.doclet;

public class getTheJson {

    public String getJsoon() {
        String[] arguments = new String[]{
                "-private",             // To run private
                "-quiet",               // To not have extra output
                "-subpackages",         // Also do the subpackages of rootdoclet
                "nl.nn.adapterframework",
                "-exclude",             // Exclude the domain package
                "nl.nn.adapterframework.doc.doclet.domain",
                "-doclet",              // Give the doclet
                "nl.nn.adapterframework.doc.doclet.ListClass",
                "-sourcepath",          // Give the sourcepath to the upper package
                "C:/Users/chakir/Documents/iaf/core/src/main/java"
        };
        com.sun.tools.javadoc.Main.execute(arguments);
        return ListClass.getJson();
    }
}

