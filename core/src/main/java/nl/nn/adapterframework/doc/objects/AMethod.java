package nl.nn.adapterframework.doc.objects;

public class AMethod {

    private String name;
    private String originalClassName; // The name of the class the method was declared in
    private String description;
    private String defaultValue;
    private int order;
    private boolean deprecated;


    public AMethod(String name, String originalClassName, String description, String defaultValue, int order, boolean deprecated) {
        this.name = name;
        this.originalClassName = originalClassName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.order = order;
        this.deprecated = deprecated;
    }

    public String getName() {
        return name;
    }

    public String getOriginalClassName() {
        return originalClassName;
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