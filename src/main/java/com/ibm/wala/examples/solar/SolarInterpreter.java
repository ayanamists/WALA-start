package com.ibm.wala.examples.solar;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.FieldReference;

import java.util.Iterator;

public class SolarInterpreter {

    public static SSAContextInterpreter makeSolarInterpreter() {
        SSAContextInterpreter result =
                new SSAContextInterpreter() {
                    @Override
                    public boolean understands(CGNode node) {
                        return false;
                    }

                    @Override
                    public boolean recordFactoryType(CGNode node, IClass klass) {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public int getNumberOfStatements(CGNode node) {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public IR getIR(CGNode node) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public IRView getIRView(CGNode node) {
                        return getIR(node);
                    }

                    @Override
                    public DefUse getDU(CGNode node) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n) {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };

        result = new DelegatingSSAContextInterpreter(new InvokeInterpreter(),
                new DelegatingSSAContextInterpreter(new GetMethodContextInterpreter(),
                    new DelegatingSSAContextInterpreter(new ForNameInterpreter(), result)));
        return result;
    }

}
