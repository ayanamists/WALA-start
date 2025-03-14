package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;

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
            boolean isMethods = callee.getReference().equals(GET_METHODS);
            IR ir = caller.getIR();
            SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
            if (invokeInstructions.length != 1) {
                return null;
            }
            int r = invokeInstructions[0].getReceiver();
            int use = isMethods ? 0 : invokeInstructions[0].getUse(1);
            GetMethodContext c = new GetMethodContext(
                    getPointerKey(caller, r),
                    invokeInstructions[0].hasDef() ?
                            getDef(caller, invokeInstructions[0].getDef(), isMethods) : null,
                    callee.getReference().equals(GET_METHODS) ? null : getPointerKey(caller, use),
                    callee.getReference().equals(GET_METHODS));
            return c;
        }
        return null;
    }

    PointerKey getDef(CGNode caller, int def, boolean isGetMethods) {
        if (! isGetMethods) {
            return getPointerKey(caller, def);
        } else {
            PointerKey arr = getPointerKey(caller, def);
            OrdinalSet<InstanceKey> set = Solar.get().builder.getPointerAnalysis().getPointsToSet(arr);
            if (set.isEmpty()) {
                InstanceKey k = new AllocationSite(caller.getMethod(),
                        null, Solar.get().builder.cha.lookupClass(
                        TypeReference.findOrCreateArrayOf(TypeReference.JavaLangObject)));
                Solar.get().builder.getSystem().newConstraint(arr, k);
                set = Solar.get().builder.getPointerAnalysis().getPointsToSet(arr);
            }
            PointerKey content = Solar.get().builder
                    .getPointerKeyForArrayContents(set.iterator().next());
            return content;
        }
    }

    @Override
    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
        return EmptyIntSet.instance;
    }
}
