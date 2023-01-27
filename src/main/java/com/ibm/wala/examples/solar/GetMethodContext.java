package com.ibm.wala.examples.solar;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

class GetMethods implements ContextKey {
    public static GetMethods instance = new GetMethods();
}

class Def implements ContextKey {
    public static Def instance = new Def();
}

class MethodName implements ContextKey {
    public static MethodName instance = new MethodName();
}

public class GetMethodContext implements Context {

    final PointerKey r;

    final PointerKey def;

    final PointerKey use;

    final boolean all;

    public GetMethodContext(PointerKey r, PointerKey def, PointerKey use, boolean all) {
        this.r = r;
        this.def = def;
        this.use = use;
        this.all = all;
    }

    @Override
    public ContextItem get(ContextKey name) {
        if (name == ContextKey.RECEIVER) {
            return Value.make(r);
        } else if (name == Def.instance) {
            return Value.make(def);
        } else if (name == MethodName.instance) {
            return Value.make(this.use);
        } else if (name == GetMethods.instance) {
            return Value.make(all);
        } else {
            return null;
        }
    }

}
