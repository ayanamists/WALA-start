package com.ibm.wala.examples.solar;

import com.ibm.wala.analysis.reflection.ClassFactoryContextSelector;
import com.ibm.wala.analysis.reflection.GetClassContextSelector;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

public class SolarSelector {
    public static ContextSelector makeSolarSelector() {

        ContextSelector result =
                new ContextSelector() {
                    @Override
                    public Context getCalleeTarget(
                            CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
                        return null;
                    }

                    @Override
                    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
                        return EmptyIntSet.instance;
                    }
                };

        result = new DelegatingContextSelector(new InvokeContextSelector(),
                new DelegatingContextSelector(new GetMethodContextSelector(),
                new DelegatingContextSelector(new ForNameContextSelector(), result)));
        return result;
    }

}
