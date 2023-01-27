package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;

import java.util.Objects;

public class MethodSig {
    TypeReference returnType;

    TypeReference[] argTypes;

    String methodName;

    public MethodSig(TypeReference returnType, TypeReference[] argTypes, String methodName) {
        this.returnType = returnType;
        this.argTypes = argTypes;
        this.methodName = methodName;
    }

    public MethodSig(MethodSig sig) {
        this.methodName = sig.methodName;
        this.argTypes = sig.argTypes;
        this.returnType = sig.returnType;
    }

    @Override
    public String toString() {

            StringBuilder sb = new StringBuilder("<" + returnType + " "  +
                    (methodName == null ? "?" : methodName)  +  "(");
            if (argTypes != null) {
                for (TypeReference i : argTypes) {
                    sb.append(i);
                    sb.append(",");
                }
            } else {
                sb.append("null");
            }
            sb.append(")>");
            return sb.toString();
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof MethodSig)) {
            return false;
        } else {
            return hashCode() == obj.hashCode();
        }
    }

    public static MethodSig getMethodSig(IMethod method) {

        boolean isStatic = method.isStatic();

        TypeReference[] argTypes = new TypeReference[ isStatic ?
                method.getNumberOfParameters() :
                method.getNumberOfParameters() - 1];

        for (int i = 0; i < argTypes.length; ++i) {
            argTypes[i] = method.getParameterType(  isStatic ? i : i + 1 );
        }

        String name = method.getName().toString();

        return new MethodSig(method.getReturnType(), argTypes, name);
    }

    public boolean canMatch(MethodSig sig1) {
        return equalsOrNull(this.returnType, sig1.returnType) &&
                equalsOrNull(this.methodName, sig1.methodName) &&
                equalsOrNull(this.argTypes, sig1.argTypes);
    }

    public boolean typeEquals(TypeReference t1, TypeReference t2) {
        if (t1.equals(TypeReference.JavaLangObject)) {
            return true;
        } else {
            return t1.equals(t2);
        }
    }

    public boolean equalsOrNull(Object o1, Object o2) {
        if (o1 == null) {
            return true;
        } else {
            if (o1 instanceof TypeReference && o2 instanceof TypeReference) {
                return typeEquals((TypeReference) o1, (TypeReference) o2);
            }
            if (o1 instanceof Object[] && o2 instanceof Object[]) {
                Object[] a1 = (Object[]) o1;
                Object[] a2 = (Object[]) o2;
                if (a1.length != a2.length) {
                    return false;
                } else {
                    for (int i = 0; i < a1.length; ++i) {
                        if (! equalsOrNull(a1[i], a2[i])) {
                            return false;
                        }
                    }
                    return true;
                }
            } else {
                return o1.equals(o2);
            }
        }
    }
}
