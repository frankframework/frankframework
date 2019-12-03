package nl.nn.adapterframework.doc.objects;

public class SpringBean implements Comparable<Object> {
    private String name;
    protected Class<?> clazz;

    public SpringBean(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String getName() {
        return name;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public int compareTo(Object o) {
        return name.compareTo(((SpringBean)o).name);
    }

    @Override
    public String toString() {
        return name +  "[" + clazz.getName() + "]";
    }
}