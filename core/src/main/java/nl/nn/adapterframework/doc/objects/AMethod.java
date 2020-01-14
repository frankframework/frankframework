package nl.nn.adapterframework.doc.objects;

import java.util.ArrayList;

public class AMethod {

    private String name;
    private String className;
    private String originalClassName;
    private String folderName;
    private String description;
    private String defaultValue;
    private int order;
    private boolean deprecated;


    public AMethod(String name, String className, String originalClassName, String folderName, String description, String defaultValue, int order, boolean deprecated) {
        this.name = name;
        this.className = className;
        this.originalClassName = originalClassName;
        this.folderName = folderName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.order = order;
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

    public int getOrder() {
        return order;
    }

    public boolean isDeprecated() {
        return deprecated;
    }
}