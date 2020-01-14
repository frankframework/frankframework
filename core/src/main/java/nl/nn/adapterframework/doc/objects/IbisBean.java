package nl.nn.adapterframework.doc.objects;

public class IbisBean implements Comparable<IbisBean> {
    private String name;
    private Class<?> clazz;

    public IbisBean(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String getName() {
        return name;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public int compareTo(IbisBean ibisBean) {
        return name.compareTo((ibisBean).name);
    }

    @Override
    public String toString() {
        return name +  "[" + clazz.getName() + "]";
    }
}