/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.examples.solar;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.analysis.reflection.JavaTypeContext;
import static com.ibm.wala.examples.util.MyUtil.*;

/**
 * A {@link ContextSelector} to intercept calls to reflective class factories (e.g. Class.forName())
 * when the parameter is a string constant
 */
public class ForNameContextSelector implements ContextSelector {

    public static final Atom forNameAtom = Atom.findOrCreateUnicodeAtom("forName");

    private static final Descriptor forNameDescriptor =
            Descriptor.findOrCreateUTF8("(Ljava/lang/String;)Ljava/lang/Class;");

    public static final MethodReference FOR_NAME_REF =
            MethodReference.findOrCreate(TypeReference.JavaLangClass, forNameAtom, forNameDescriptor);

    public static final Atom loadClassAtom = Atom.findOrCreateUnicodeAtom("loadClass");

    private static final Descriptor loadClassDescriptor =
            Descriptor.findOrCreateUTF8("(Ljava/lang/String;)Ljava/lang/Class;");

    private static final TypeReference CLASSLOADER =
            TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ClassLoader");

    public static final MethodReference LOAD_CLASS_REF =
            MethodReference.findOrCreate(CLASSLOADER, loadClassAtom, loadClassDescriptor);

    public static boolean isClassFactory(MethodReference m) {
        return m.equals(FOR_NAME_REF);
    }

    public int getUseOfStringParameter(SSAAbstractInvokeInstruction call) {
        if (call.isStatic()) {
            return call.getUse(0);
        } else {
            return call.getUse(1);
        }
    }

    @Override
    public Context getCalleeTarget(
            CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
        if (isClassFactory(callee.getReference())) {
            IR ir = caller.getIR();
            SymbolTable symbolTable = ir.getSymbolTable();
            SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
            if (invokeInstructions.length != 1) {
                return null;
            }
            PointerKey def = null;
            if (invokeInstructions[0].hasDef()) {
                int d = invokeInstructions[0].getDef();
                def = getPointerKey(caller, d);
            }
            int use = getUseOfStringParameter(invokeInstructions[0]);

            if (! caller.getIR().getSymbolTable().isConstant(use)) {
                return new ForNameContext(null, def);
            }

            String className =
                    StringStuff.deployment2CanonicalTypeString(symbolTable.getStringValue(use));
            TypeReference t =
                    TypeReference.findOrCreate(
                            caller.getMethod().getDeclaringClass().getClassLoader().getReference(), className);
            IClass klass = caller.getClassHierarchy().lookupClass(t);

            if (klass != null) {
                return new ForNameContext(klass, def);
            } else {
                return new ForNameContext(null, def);
            }
        }
        return null;
    }

    private static final IntSet thisParameter = IntSetUtil.make(new int[] {0});

    private static final IntSet firstParameter = IntSetUtil.make(new int[] {0, 1});

    @Override
    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
        IMethod resolved =
                caller.getMethod().getClassHierarchy().resolveMethod(site.getDeclaredTarget());
        if (isClassFactory(resolved != null ? resolved.getReference() : site.getDeclaredTarget())) {
            SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
            if (invokeInstructions.length >= 1) {
                if (invokeInstructions[0].isStatic()) {
                    return thisParameter;
                } else {
                    return firstParameter;
                }
            } else {
                return EmptyIntSet.instance;
            }
        } else {
            return EmptyIntSet.instance;
        }
    }
}
