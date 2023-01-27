package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

public class InvokeContext implements Context {

    final CallSiteReference callSite;

    final CGNode caller;

    public InvokeContext(CallSiteReference callSite, CGNode caller) {
        this.callSite = callSite;
        this.caller = caller;
    }

    @Override
    public ContextItem get(ContextKey name) {
        if (name == ContextKey.CALLSITE) {
            return callSite;
        } else if (name == ContextKey.CALLER) {
            return caller;
        }
        return null;
    }
}
