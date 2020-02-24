package nl.nn.adapterframework.doc.doclet;

import com.sun.javadoc.*;
import nl.nn.adapterframework.doc.doclet.domain.Folder;
import nl.nn.adapterframework.doc.doclet.domain.Method;
import nl.nn.adapterframework.doc.doclet.domain.Class;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
public class ListClass {

    private static String json;
    private static String currentFolder = "defaultFolderName";

    /**
     * Hello world
     * @order 1
     * @default hoi
     * @param root the ruut
     * @return something
     * @throws JSONException an exception
     */
    public static boolean start(RootDoc root) throws JSONException {
        JSONStringer writer = new JSONStringer();

        // Make a new array of classes for the current folder
        ArrayList<Class> classesInThisFolder = new ArrayList<Class>();

        writer.array();
        ClassDoc[] classes = root.classes();

        // Check all classes
        for (int i = 0; i < classes.length; ++i) {

            // If we switched folders we have to notice
            if (!classes[i].toString().contains(currentFolder)) {

                // Add all methods of the previous folder to the previous folder object
                Folder folder = new Folder(currentFolder, classesInThisFolder);

                // If its not an empty folder, add it to the json
                if (!classesInThisFolder.isEmpty()) {
                    folder.appendToJsonWriter(writer);
                }
                classesInThisFolder = new ArrayList<Class>();

                // Get the new folder name
                String fullPackage = classes[i].containingPackage().toString();
                currentFolder = fullPackage.substring(fullPackage.lastIndexOf(".") + 1);
            }

            // Create a class object and json object of that class
            MethodDoc[] methods = classes[i].methods();
            Class clazz = new Class(classes[i].name(), classes[i].commentText(), getMethods(methods));

            // Add the class to the array for this folder
            classesInThisFolder.add(clazz);
        }
        writer.endArray();
        json = writer.toString();
        return true;
    }

    public static ArrayList<Method> getMethods(MethodDoc[] methods) throws JSONException {
        ArrayList<Method> methoden = new ArrayList<Method>();

        // For every method
        for (int j = 0; j < methods.length; j++) {

            // If it has a description
            if (methods[j].tags("description").length != 0) {

                // Get the value of the description and default value
                Tag descriptionTag = methods[j].tags("description")[0];
                Tag defaultTag = methods[j].tags("default")[0];
                
                // Some javadoc doesnt have an order tag
                if (methods[j].tags("order").length != 0) {
                    Tag order = methods[j].tags("order")[0];
                    Method method = new Method(methods[j].name(), Integer.parseInt(order.text()), descriptionTag.text(), defaultTag.text());
                    methoden.add(method);
                } else {
                	
                    // Create a new method object and add that to the method array
                    Method method = new Method(methods[j].name(), -1, descriptionTag.text(), defaultTag.text());
                    methoden.add(method);
                }
            }
        }
        return methoden;
    }

    public static String getJson() {
        return json;
    }
}