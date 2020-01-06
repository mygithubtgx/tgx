package controller.test1;

import java.lang.reflect.Method;

public class MethodInfo {
    private String className;
    private Method method;

    public MethodInfo(String className, Method method) {
        this.className = className;
        this.method = method;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "className='" + className + '\'' +
                ", method=" + method +
                '}';
    }
}
