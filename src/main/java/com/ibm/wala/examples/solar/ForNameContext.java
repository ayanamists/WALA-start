package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

public class ForNameContext implements Context {

    final IClass klass;

    final PointerKey def;

    public ForNameContext(IClass klass, PointerKey def) {
        this.klass = klass;
        this.def = def;
    }

    @Override
    public ContextItem get(ContextKey name) {
        if (name.equals(ContextKey.RECEIVER)) {
            return Value.make(klass);
        } else if (name.equals(ContextKey.TARGET)) {
            return Value.make(def);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "<ForNameCtx: " + klass + " -> " + def + " >";
    }
}
