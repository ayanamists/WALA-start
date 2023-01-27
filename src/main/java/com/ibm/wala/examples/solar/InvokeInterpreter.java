package com.ibm.wala.examples.solar;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.ArraySetMultiMap;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.MultiMap;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

import static com.ibm.wala.examples.util.MyUtil.*;

import java.util.*;
import java.util.stream.Collectors;

public class InvokeInterpreter implements SSAContextInterpreter {

    @Override
    public IR getIR(CGNode node) {
        CallSiteReference callSite = (CallSiteReference)
                node.getContext().get(ContextKey.CALLSITE);
        CGNode caller = (CGNode) node.getContext().get(ContextKey.CALLER);

        SSAAbstractInvokeInstruction[] instrs = caller.getIR().getCalls(callSite);
        if (instrs.length == 1) {
            SSAAbstractInvokeInstruction instr = instrs[0];

            int def = -1;
            if (instr.hasDef()) {
                def = instr.getDef();
            }

            int m = instr.getReceiver();
            PointerKey ptm = getPointerKey(caller, m);
            int obj = instr.getUse(1);
            int arr = instr.getUse(2);
            TypeReference A = instr.hasDef() ? getCastType(caller, def) : TypeReference.Void;
            List<MethodSig> sigs = getAllPossibleSig(caller, arr, A);
            Set<IClass> iInvTP = new HashSet<>();

            boolean needInvS2T = false;
            for (InstanceKey i : getPTS(caller, obj)) {
                if (! i.getConcreteType().getReference().equals(TypeReference.JavaLangObject)) {
                    iInvTP.add(i.getConcreteType());
                } else {
                    needInvS2T = true;
                }
            }

            Queue<MetaMethod> workList = new LinkedList<>();
            for (InstanceKey k : getPTS(caller, m)) {
                workList.add((MetaMethod) k);
            }

            while (! workList.isEmpty()) {
                MetaMethod now = workList.poll();
                if (now.getDeclType() == null) {
                    for (IClass k : iInvTP) {
                        MetaMethod next = new MetaMethod(now);
                        next.declType = k;
                        addPTS(ptm, next, workList);
                    }

                    if (needInvS2T) {
                        if (now.getSignature() != null && now.getSignature().methodName != null) {
                            for (MethodSig sig : sigs) {
                                MethodSig sig1 = new MethodSig(sig);
                                sig1.methodName = now.getSignature().methodName;
                                for (IClass k : searchContainingClass(Solar.get().getBuilder().cha, sig1)) {
                                    MetaMethod next = new MetaMethod(now);
                                    next.declType = k;
                                    addPTS(ptm, next, workList);
                                }
                            }
                        }
                    }
                }

                if (now.getSignature() == null) {
                    for (MethodSig sig : sigs) {
                        MetaMethod next = new MetaMethod(now);
                        next.signature = sig;
                        addPTS(ptm, next, workList);
                    }
                }
            }

            Set<IMethod> methods = selectMethod(caller, m, arr);
            Solar.get().logReflectionCall(caller, methods);
        }
        return null;
    }

    Set<IMethod> selectMethod(CGNode node, int mtd, int arr) {
        MultiMap<Integer, TypeReference> argTypes = getArgPtrTypes(node, arr);
        OrdinalSet<InstanceKey> ptm = getPTS(node, mtd);
        MultiMap<MethodSig, IMethod> map = new ArraySetMultiMap<>();

        for (InstanceKey k : ptm) {
            if (! (k instanceof MetaMethod)) {
                continue;
            }
            MetaMethod mm = (MetaMethod) k;

            if (mm.getSignature() == null || mm.getDeclType() == null) {
                continue;
            }

            for (IClass c : getUpperClasses(mm.getDeclType())) {
                for (IMethod m : c.getAllMethods()) {
                    MethodSig mCan = MethodSig.getMethodSig(m);
                    if (m.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) &&
                            ! m.getName().toString().equals("<init>") &&
                            ! m.isSynthetic() &&
                            length.contains(mCan.argTypes.length) &&
                            mm.getSignature().canMatch(mCan) &&
                            hasCandidate(mCan, argTypes)) {
                        map.put(mCan, m);
                    }
                }
            }
        }

        Set<IMethod> set = new HashSet<>();
        for (MethodSig sig : map.keySet()) {
            if (map.get(sig).size() == 1) {
                set.add(map.get(sig).iterator().next());
            } else {
                Map<IClass, IMethod> current = new HashMap<>();
                for (IMethod method : map.get(sig)) {
                    boolean needAdd = true;
                    for (IClass k : current.keySet()) {
                        if (Solar.get().builder.cha
                                .isSubclassOf(method.getDeclaringClass(), k)) {
                            current.remove(k);
                        } else if (Solar.get().builder.cha
                                .isSubclassOf(k, method.getDeclaringClass())) {
                            needAdd = false;
                            break;
                        }
                    }

                    if (needAdd) {
                        current.put(method.getDeclaringClass(), method);
                    }
                }
                set.addAll(current.values());
            }
        }

        return set;
    }

    boolean hasCandidate(MethodSig sig, MultiMap<Integer, TypeReference> argTypes) {
        TypeReference[] sigArgs = sig.argTypes;
        for (int i = 0; i < sigArgs.length; ++i) {
            if (argTypes.get(i).size() == 0) {
                return false;
            }
            if (sigArgs[i] == null) {
                continue;
            }
            IClass sigT = getIClass(sigArgs[i]);
            boolean subMatch = argTypes.get(i)
                    .stream()
                    .map(this::getIClass)
                    .anyMatch(t -> Solar.get().builder.cha.isSubclassOf(t, sigT));
            if (! subMatch) {
                return false;
            }
        }
        return true;
    }

    IClass getIClass(TypeReference t) {
        return Solar.get().builder.cha.lookupClass(t);
    }

    OrdinalSet<InstanceKey> getPTS(CGNode node, int idx) {
        return getPTS(getPointerKey(node, idx));
    }

    OrdinalSet<InstanceKey> getPTS(PointerKey key) {
        return Solar.get().builder.getPointerAnalysis().getPointsToSet(key);
    }

    void addPTS(PointerKey k, MetaMethod mtd, Queue<MetaMethod> workList) {
        OrdinalSet<InstanceKey> pts = getPTS(k);
        if (! pts.contains(mtd)) {
            workList.add(mtd);
            Solar.get().builder.getPropagationSystem().newConstraint(k, mtd);
        }
    }

    public TypeReference getCastType(CGNode node, int def) {
        DefUse du = node.getDU();
        Iterator<SSAInstruction> uses = du.getUses(def);
        while (uses.hasNext()) {
            SSAInstruction i = uses.next();
            if (i instanceof SSACheckCastInstruction) {
                SSACheckCastInstruction cast = (SSACheckCastInstruction) i;
                TypeReference[] t = cast.getDeclaredResultTypes();
                return t[0];
            }
        }
        return TypeReference.JavaLangObject;
    }

    public MultiMap<Integer, TypeReference> getArgPtrTypes(CGNode node, int arr) {
        MultiMap<Integer, TypeReference> allType = new ArraySetMultiMap<>();

        if (local) {
            SSAInstruction[] instructions = node.getIR().getInstructions();
            for (SSAInstruction instr : instructions) {
                if (instr instanceof SSAArrayStoreInstruction) {
                    SSAArrayStoreInstruction as = (SSAArrayStoreInstruction) instr;
                    if (as.getArrayRef() == arr) {
                        int idxVar = as.getUse(1);
                        if (node.getIR().getSymbolTable().isConstant(idxVar)) {
                            int idx = node.getIR().getSymbolTable().getIntValue(idxVar);
                            for (InstanceKey obj : getPTS(node, as.getValue())) {
                                IClass t = obj.getConcreteType();
                                for (TypeReference type : getUppers(t)) {
                                    allType.put(idx, type);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Set<TypeReference> types = new HashSet<>();
            for (InstanceKey obj : getPTS(node, arr)) {
                PointerKey pk = Solar.get().builder.getPointerKeyForArrayContents(obj);
                for (InstanceKey o : getPTS(pk)) {
                    types.add(o.getConcreteType().getReference());
                }
            }

            for (int i = 0; i < Collections.max(length); ++i) {
                for (TypeReference type : types) {
                    allType.put(i, type);
                }
            }
        }
        return allType;
    }

    Set<Integer> getArrNewLength(CGNode node, Iterator<InstanceKey> k) {
        Set<Integer> length = new HashSet<>();
        while (k.hasNext()) {
            InstanceKey arrObj = k.next();
            Iterator<Pair<CGNode, NewSiteReference>> newSites =
                    arrObj.getCreationSites(Solar.get().builder.getCallGraph());

            while (newSites.hasNext()) {
                Pair<CGNode, NewSiteReference> newSite = newSites.next();
                SSANewInstruction newInstr = newSite.fst.getIR().getNew(newSite.snd);
                IR ir = newSite.fst.getIR();
                if (newSite.fst != node) {
                    local = false;
                }
                if (ir != null &&
                        ir.getSymbolTable().isConstant(newInstr.getUse(0))) {
                    length.add(ir.getSymbolTable().getIntValue(newInstr.getUse(0)));
                }
            }
        }

        return length;
    }

    boolean local = true;

    Set<Integer> length;

    public List<MethodSig> getAllPossibleSig(CGNode node, int arr, TypeReference A) {
        IClassHierarchy cha = Solar.get().getBuilder().cha;
        Collection<IClass> rets = A.equals(TypeReference.JavaLangObject)
                ? List.of(cha.lookupClass(A))
                : cha.computeSubClasses(A);
        List<MethodSig> res = new ArrayList<>();

        OrdinalSet<InstanceKey> k = getPTS(node, arr);
        PointerAnalysis<InstanceKey> pa = Solar.get().builder.getPointerAnalysis();
        List<List<TypeReference>> argTypes = new ArrayList<>();
        if (k.isEmpty()) {
            argTypes = List.of(List.of());
        } else {
            length = getArrNewLength(node, k.iterator());
            MultiMap<Integer, TypeReference> allType = getArgPtrTypes(node, arr);
            int maxLength = Collections.max(length);

            List<List<List<TypeReference>>> allTypeN = new ArrayList<>();
            for (int i = 0; i < maxLength; ++i) {
                List<List<TypeReference>> t = new ArrayList<>();
                allType.get(i).forEach(type -> t.add(List.of(type)));
                allTypeN.add(t.size() == 0 ? List.of(List.of()) : t);
            }
            List<List<TypeReference>> pAllType = allTypeN
                    .stream()
                    .reduce(this::product)
                    .orElse(null);
            for (int i : length) {
                if (i == 0) {
                    argTypes.add(List.of());
                } else {
                    for (List<TypeReference> l : pAllType) {
                        argTypes.add(l.stream().limit(i).collect(Collectors.toList()));
                    }
                }
            }
        }

        for (IClass r : rets) {
            for (List<TypeReference> a : argTypes) {
                res.add(new MethodSig(r.getReference(), a.toArray(new TypeReference[0]), null));
            }
        }

        return res;
    }

    <T> List<List<T>> product(List<List<T>> l1, List<List<T>> l2) {
        List<List<T>> res = new ArrayList<>();
        for (List<T> i : l1) {
            for (List<T> j : l2) {
                List<T> now = new ArrayList<>(i);
                now.addAll(j);
                res.add(now);
            }
        }
        return res;
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
        return node.getContext().isA(InvokeContext.class) &&
                isApplication((CGNode) node.getContext().get(ContextKey.CALLER));
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
