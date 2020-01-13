package nl.nn.adapterframework.doc;

import nl.nn.adapterframework.doc.objects.AClass;
import nl.nn.adapterframework.doc.objects.AFolder;
import nl.nn.adapterframework.doc.objects.AMethod;
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
                        methodObject.put("deprecated", method.isDeprecated());
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

    /**
     * Adds a folder to the folder array
     *
     * @param folder - The folder to be added
     */
    public void addFolder(AFolder folder) {
        folders.add(folder);
    }
}
