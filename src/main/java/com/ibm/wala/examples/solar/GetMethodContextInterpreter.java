package com.ibm.wala.examples.solar;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.generics.MethodTypeSignature;
import com.ibm.wala.util.collections.ArraySetMultiMap;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.MultiMap;
import com.ibm.wala.util.intset.OrdinalSet;

import static com.ibm.wala.examples.util.MyUtil.*;

import java.util.Iterator;

public class GetMethodContextInterpreter implements SSAContextInterpreter {

    @Override
    public IR getIR(CGNode node) {
        Context c = node.getContext();
        PointerKey def = getValue(c.get(Def.instance));
        PointerKey r = getValue(c.get(ContextKey.RECEIVER));
        PointerKey use = getValue(c.get(MethodName.instance));
        boolean isGetMethods = getValue(c.get(GetMethods.instance));

        if (def != null) {
            OrdinalSet<InstanceKey> pts = Solar.get().builder
                    .getPointerAnalysis().getPointsToSet(r);
            for (InstanceKey i : pts) {
                IClass klass = null;
                MethodSig sig = null;

                if (i instanceof ConstantKey) {
                    if (((ConstantKey<?>) i).getValue() instanceof IClass) {
                        klass = (IClass) ((ConstantKey<?>) i).getValue();
                    }
                } else {
                    assert i instanceof MetaClass;
                    MetaClass mc = (MetaClass) i;
                    klass = mc.type;
                }

                if (isGetMethods) {
                    Solar.get().addToPts(def, new MetaMethod(klass, null));
                    continue;
                }

                OrdinalSet<InstanceKey> ptStr = Solar.get().builder.getPointerAnalysis().getPointsToSet(use);

                if(ptStr.isEmpty()) {
                    Solar.get().builder.getSystem().newConstraint(def, new MetaMethod(klass, null));
                    continue;
                }

                for (InstanceKey j : ptStr) {
                    String s = null;
                    if (j instanceof ConstantKey) {
                        ConstantKey<?> sc = (ConstantKey<?>) j;
                        s = (String) sc.getValue();
                    }

                    if (s != null) {
                        sig = new MethodSig(null, null, s);
                    }

                    MetaMethod method = new MetaMethod(klass, sig);
                    Solar.get().addToPts(def, method);

                }
            }
        }
        return null;
    }

    @Override
    public IRView getIRView(CGNode node) {
        return null;
    }

    @Override
    public DefUse getDU(CGNode node) {
        return null;
    }

    @Override
    public int getNumberOfStatements(CGNode node) {
        return 0;
    }

    @Override
    public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n) {
        return null;
    }

    @Override
    public boolean understands(CGNode node) {
        return node.getContext().isA(GetMethodContext.class);
    }

    @Override
    public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
        return EmptyIterator.instance();
    }

    @Override
    public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
        return EmptyIterator.instance();
    }

    @Override
    public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
        return EmptyIterator.instance();
    }

    @Override
    public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
        return EmptyIterator.instance();
    }

    @Override
    public boolean recordFactoryType(CGNode node, IClass klass) {
        return false;
    }
}
