package com.ibm.wala.examples.solar;

import com.ibm.wala.types.TypeReference;

public class MethodSig {
    final TypeReference returnType;

    final TypeReference[] argTypes;

    final String methodName;

    public MethodSig(TypeReference returnType, TypeReference[] argTypes, String typeName) {
        this.returnType = returnType;
        this.argTypes = argTypes;
        this.methodName = typeName;
    }

    @Override
    public String toString() {
        if (methodName == null) {
            return "<>";
        } else {
            StringBuilder sb = new StringBuilder("<" + returnType + " "  + methodName +  "(");
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
    }
}
