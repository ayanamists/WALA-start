package com.ibm.wala.examples.solar;

import com.ibm.wala.analysis.reflection.ClassFactoryContextSelector;
import com.ibm.wala.analysis.reflection.JavaTypeContext;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.collections.EmptyIterator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ForNameInterpreter implements SSAContextInterpreter {

    Set<CGNode> knownNodes;

    public ForNameInterpreter() {
        knownNodes = new HashSet<>();
    }

    public IR getIR(CGNode node) {
        if (knownNodes.contains(node)) {
            return null;
        }
        knownNodes.add(node);
        Context c = node.getContext();

        PointerKey def = (PointerKey)
                ((ContextItem.Value<?>) c.get(ContextKey.TARGET)).getValue();
        IClass type = (IClass)
                ((ContextItem.Value<?>) c.get(ContextKey.RECEIVER)).getValue();

        MetaClass mc = new MetaClass(Solar.get().getBuilder().cha, type);
        Solar.get().getBuilder().getSystem().newConstraint(def, mc);
        return null;
    }

    public IRView getIRView(CGNode node) {
        return this.getIR(node);
    }

    public int getNumberOfStatements(CGNode node) {
        assert this.understands(node);

        return 0;
    }

    public boolean understands(CGNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        } else {
            return node.getContext().isA(ForNameContext.class);
        }
    }

    public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
        return EmptyIterator.instance();
    }

    public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
        assert this.understands(node);

        return EmptyIterator.instance();
    }


    public boolean recordFactoryType(CGNode node, IClass klass) {
        return false;
    }

    public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
        return EmptyIterator.instance();
    }

    public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
        return EmptyIterator.instance();
    }

    public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
        return this.getIR(N).getControlFlowGraph();
    }

    public DefUse getDU(CGNode node) {
        return new DefUse(this.getIR(node));
    }
}
