/*
   Copyright 2019, 2020 Integration Partners

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
package nl.nn.adapterframework.doc;

import nl.nn.adapterframework.doc.objects.AClass;
import nl.nn.adapterframework.doc.objects.AFolder;
import nl.nn.adapterframework.doc.objects.AMethod;
import nl.nn.adapterframework.doc.objects.IbisBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Extracts the IbisDoc data from the data objects and turns it into a JSON object
 * 
 * @author Chakir el Moussaoui
 */
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
                    classObject.put("name", aClass.getClazz().getSimpleName());
                    classObject.put("packageName", aClass.getClazz().getName());
                    classObject.put("javadocLink", aClass.getJavadocLink());
                    classObject.put("superClasses", aClass.getSuperClasses());

                    newMethods = new JSONArray();
                    for (AMethod method : aClass.getMethods()) {
                        JSONObject methodObject = new JSONObject();
                        methodObject.put("name", method.getName());
                        methodObject.put("originalClassName", method.getOriginalClassName());
                        methodObject.put("description", method.getDescription());
                        methodObject.put("defaultValue", method.getDefaultValue());
                        methodObject.put("order", method.getOrder());
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
    public String getJson() {
        Map<String, TreeSet<IbisBean>> groups = IbisDocPipe.getGroups();
        addFolders(groups);
        writeToJsonUrl();
        return this.json;
    }

    /**
     * Add folders to the Json.
     *
     * @param groups    - Contains all information
     */
    public void addFolders(Map<String, TreeSet<IbisBean>> groups) {
        AFolder allFolder = new AFolder("All");
        for (String folder : groups.keySet()) {
            AFolder newFolder = new AFolder(folder);
            newFolder.setClasses(groups, newFolder);
            folders.add(newFolder);
        }
        folders.add(allFolder);
    }
}
