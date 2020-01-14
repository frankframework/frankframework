package nl.nn.adapterframework.doc.objects;

public class IbisMethod {
    private String methodName; // E.g. registerAdapter
    private String parameterName; // E.g. adapter
    int maxOccurs = -1;

    IbisMethod(String methodName, String parameterName) {
        this.methodName = methodName;
        this.parameterName = parameterName;
        if (methodName.startsWith("set")) {
            maxOccurs = 1;
        } else if (!(methodName.startsWith("add") || methodName.startsWith("register"))) {
            throw new RuntimeException("Unknow verb in method name: " + methodName);
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public String toString() {
        return methodName +  "(" + parameterName + ")";
    }
}