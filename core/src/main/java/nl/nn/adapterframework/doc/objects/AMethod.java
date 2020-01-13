package nl.nn.adapterframework.doc.objects;

import java.util.ArrayList;

public class AMethod {

    private String name;
    private String className;
    private String originalClassName;
    private String folderName;
    private String description;
    private String defaultValue;
    private String javadocLink;
    private int order;
    private ArrayList<String> superClasses;
    private boolean deprecated;


    public AMethod(String name, String className, String originalClassName, String folderName, String description, String defaultValue, String javadocLink, int order, ArrayList<String> superClasses, boolean deprecated) {
        this.name = name;
        this.className = className;
        this.originalClassName = originalClassName;
        this.folderName = folderName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.javadocLink = javadocLink;
        this.order = order;
        this.superClasses = superClasses;
        this.deprecated = deprecated;
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

    public boolean isDeprecated() {
        return deprecated;
    }
}