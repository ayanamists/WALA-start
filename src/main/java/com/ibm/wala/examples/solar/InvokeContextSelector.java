package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

public class InvokeContextSelector implements ContextSelector {
    public static final MethodReference METHOD_INVOKE =
            MethodReference.findOrCreate(
                    TypeReference.JavaLangReflectMethod,
                    "invoke",
                    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    @Override
    public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
        if (callee.getReference().equals(METHOD_INVOKE)) {
            return new InvokeContext(site, caller);
        }
        return null;
    }

    @Override
    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
        return EmptyIntSet.instance;
    }
}
