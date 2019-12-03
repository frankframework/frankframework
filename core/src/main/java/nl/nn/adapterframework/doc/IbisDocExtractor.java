package nl.nn.adapterframework.doc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class IbisDocExtractor {

    private ArrayList<AFolder> folders = new ArrayList<AFolder>();
    private String json = "";

    /**
     * Writes the folders object containing all information to a Json.
     */
    public void writeToJsonUrl() {
        JSONArray newFolders = new JSONArray();
        JSONArray newClasses;
        JSONArray newMethods;

        try {
            for (AFolder folder : folders) {
                JSONObject folderObject = new JSONObject();
                folderObject.put("name", folder.getName());

                newClasses = new JSONArray();
                for (AClass aClass : folder.getClasses()) {
                    JSONObject classObject = new JSONObject();
                    classObject.put("name", aClass.getName());
                    classObject.put("packageName", aClass.getPackageName());

                    newMethods = new JSONArray();
                    for (AMethod method : aClass.getMethods()) {
                        JSONObject methodObject = new JSONObject();
                        methodObject.put("name", method.getName());
                        methodObject.put("originalClassName", method.getOriginalClassName());
                        methodObject.put("description", method.getDescription());
                        methodObject.put("defaultValue", method.getDefaultValue());
                        methodObject.put("javadocLink", method.getJavadocLink());
                        methodObject.put("order", method.getOrder());
                        methodObject.put("superClasses", method.getSuperClasses());
                        newMethods.put(methodObject);
                    }
                    classObject.put("methods", newMethods);
                    newClasses.put(classObject);
                }
                folderObject.put("classes", newClasses);
                newFolders.put(folderObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        json = newFolders.toString();
    }

    /**
     * Get the Json in String format.
     *
     * @return The Json String
     */
    public String getJsonString() {
        return this.json;
    }

//    /**
//     * Add an All folder to the folders containing all classes
//     */
//    public void addAllFolder() {
//        AFolder allFolder = new AFolder("All");
//        for (AFolder folder : folders) {
//            for (AClass clazz : folder.getClasses()) {
//                allFolder.addClass(clazz);
//            }
//        }
//        folders.add(allFolder);
//    }

    /**
     * Adds a folder to the folder array
     *
     * @param folder - The folder to be added
     */
    public void addFolder(AFolder folder) {
        folders.add(folder);
    }
}

class AMethod {

    private String name;
    private String className;
    private String originalClassName;
    private String folderName;
    private String description;
    private String defaultValue;
    private String javadocLink;
    private int order;
    private ArrayList<String> superClasses;


    public AMethod(String name, String className, String originalClassName, String folderName, String description, String defaultValue, String javadocLink, int order, ArrayList<String> superClasses) {
        this.name = name;
        this.className = className;
        this.originalClassName = originalClassName;
        this.folderName = folderName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.javadocLink = javadocLink;
        this.order = order;
        this.superClasses = superClasses;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getOriginalClassName() {
        return originalClassName;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getJavadocLink() {
        return javadocLink;
    }

    public int getOrder() {
        return order;
    }

    public ArrayList<String> getSuperClasses() {
        return superClasses;
    }

}

class AClass {

    private String name;
    private String packageName;
    private ArrayList<AMethod> methods;

    public AClass(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
        this.methods = new ArrayList<AMethod>();
    }

    public void addMethod(AMethod method) {
        this.methods.add(method);
    }

    public String getName() {
        return this.name;
    }

    public String getPackageName() {
        return packageName;
    }

    public ArrayList<AMethod> getMethods() {
        return this.methods;
    }
}

class AFolder {

    private String name;
    private ArrayList<AClass> classes;

    public AFolder(String name) {
        this.name = name;
        this.classes = new ArrayList<AClass>();
    }

    public void addClass(AClass clazz) {
        this.classes.add(clazz);
    }

    public String getName() {
        return name;
    }

    public ArrayList<AClass> getClasses() {
        return this.classes;
    }
}
