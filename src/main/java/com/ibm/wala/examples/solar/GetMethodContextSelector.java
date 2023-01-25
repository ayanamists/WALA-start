package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ibm.wala.examples.util.MyUtil.*;

/**
 * Produces {@link com.ibm.wala.analysis.reflection.GetMethodContext} if appropriate.
 *
 * @author Michael Heilmann
 * @see com.ibm.wala.analysis.reflection.GetMethodContext
 * @see com.ibm.wala.analysis.reflection.GetMethodContextInterpreter
 */
public class GetMethodContextSelector implements ContextSelector {
    public static final MethodReference GET_METHOD =
            MethodReference.findOrCreate(
                    TypeReference.JavaLangClass,
                    "getMethod",
                    "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

    public static final MethodReference GET_METHODS =
            MethodReference.findOrCreate(
                    TypeReference.JavaLangClass, "getMethods", "()[Ljava/lang/reflect/Method;");

    Set<MethodReference> set = new HashSet<>(List.of(GET_METHOD, GET_METHODS));
    @Override
    public Context getCalleeTarget(
            CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
        if (set.contains(callee.getReference())) {
            IR ir = caller.getIR();
            SymbolTable symbolTable = ir.getSymbolTable();
            SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
            if (invokeInstructions.length != 1) {
                return null;
            }
            int r = invokeInstructions[0].getReceiver();
            int use = invokeInstructions[0].getUse(1);
            String sym = null;
            if (symbolTable.isStringConstant(invokeInstructions[0].getUse(1))) {
                sym = symbolTable.getStringValue(use);
            }
            GetMethodContext c = new GetMethodContext(
                    getPointerKey(caller, r),
                    invokeInstructions[0].hasDef() ? getPointerKey(caller, invokeInstructions[0].getDef()) : null,
                    sym,
                    callee.getReference().equals(GET_METHODS));
            return c;
        }
        return null;
    }

    @Override
    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
        return EmptyIntSet.instance;
    }
}
