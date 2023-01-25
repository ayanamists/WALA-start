package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Pair;

import java.util.Iterator;

public class MetaMethod implements InstanceKey {

    public IClass getDeclType() {
        return declType;
    }

    public MethodSig getSignature() {
        return signature;
    }

    final IClass declType;

    final MethodSig signature;

    public MetaMethod(IClass declType, MethodSig signature) {
        this.declType = declType;
        this.signature = signature;
    }

    @Override
    public IClass getConcreteType() {
        TypeReference mtd = TypeReference.JavaLangReflectMethod;
        return Solar.get().builder.getClassHierarchy().lookupClass(mtd);
    }

    @Override
    public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
        return EmptyIterator.instance();
    }

    @Override
    public String toString() {
        return "M{ " + declType + ", " + signature + " }";
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MetaMethod) {
            return o.toString().equals(toString());
        }
        return false;
    }
}
