package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.PhantomClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Pair;

import java.util.Iterator;

public class MetaClass implements InstanceKey {

    public IClass type;

    private final IClassHierarchy cha;

    public MetaClass(IClassHierarchy cha, IClass type) {
        this.cha = cha;
        this.type = type;
    }

    public boolean isUnknown() {
        return type == null;
    }

    @Override
    public IClass getConcreteType() {
        return cha.lookupClass(TypeReference.JavaLangClass);
    }

    @Override
    public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
        return EmptyIterator.instance();
    }

    @Override
    public String toString() {
        if (isUnknown()) {
            return "C{ u }";
        } else {
            return "C{ " + type + " }";
        }
    }
}
