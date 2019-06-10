package nl.nn.adapterframework.doc;

import com.google.gson.Gson;
import java.io.*;
import java.util.*;

public class ResultsTesting {

    BufferedWriter writer;
    private ArrayList<AFolder> folders = new ArrayList<AFolder>();
    private ArrayList<AClass> classes = new ArrayList<AClass>();
    private String json = "";

    public static void main(String[] args) {
        ResultsTesting rt = new ResultsTesting();
        rt.writeToJsonFile();
    }

    public void writeToJsonFile() {
        Gson gson = new Gson();
        json = gson.toJson(folders);
    }

    public String getJsonString() {
        return this.json;
    }

    public void addMethods(String currentFolder, String currentClass, String methodName, String description, String defaultValue, String originalClassName, String descriptionClass) {

        // Check if the folder already exists (there is only one of each)
        boolean folderExists = false;
        for (AFolder folder : folders) {
            if (currentFolder.equals(folder.getName())) {
                folderExists = true;
                break;
            }
        }

        if (!folderExists) {
            folders.add(new AFolder(currentFolder));
        }

        // Check if the class already exists (We assume there is only one of each)
        boolean classExists = false;
        for (AClass aClass : classes) {
            if (currentClass.equals(aClass.getName())) {
                classExists = true;
                break;
            }
        }

        if (!classExists) {
            for (AFolder folder : folders) {
                if (currentFolder.equals(folder.getName())) {
                    folder.addClass(new AClass(currentClass));
                    classes.add(new AClass(currentClass));
                }
            }
        }

        for (AFolder folder : folders) {
            if (currentFolder.equals(folder.getName())) {
                for (AClass aClass : folder.getClasses()) {
                    if (currentClass.equals(aClass.getName())) {
                        aClass.addMethod(new AMethod(currentFolder, currentClass, methodName, description, defaultValue, originalClassName, descriptionClass));
                    }
                }
            }
        }

    }

    class AMethod {

        private String name;
        private String description;
        private String defaultValue;
        private String className;
        private String superClassName;
        private String folderName;
        private String originalClassName;
        private String descriptionClass;

        public AMethod(String folderName, String className, String name, String description, String defaultValue, String originalClassName, String descriptionClass) {
            this.folderName = folderName;
            this.className = className;
            this.name = name;
            this.description = description;
            this.defaultValue = defaultValue;
            this.originalClassName = originalClassName;
            this.descriptionClass = descriptionClass;
        }
    }

    class AClass {
        private String name;
        private ArrayList<AMethod> methods;

        public AClass(String name) {
            this.name = name;
            this.methods = new ArrayList<AMethod>();
        }

        public String getName() {
            return this.name;
        }

        public void addMethod(AMethod method) {
            this.methods.add(method);
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

        public String getName() {
            return name;
        }

        public void addClass(AClass clazz) {
            this.classes.add(clazz);
        }

        public ArrayList<AClass> getClasses() {
            return this.classes;
        }
    }
}
