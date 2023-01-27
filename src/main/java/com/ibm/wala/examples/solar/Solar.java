package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.OrdinalSet;

import java.lang.invoke.CallSite;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Solar {

    AnalysisScope scope;
    String mainClass;
    SSAPropagationCallGraphBuilder builder;

    Map<CGNode, Set<IMethod>> reflectionCalls = new HashMap<>();

    public SSAPropagationCallGraphBuilder getBuilder() {
        return builder;
    }

    Solar (AnalysisScope scope, String mainClass) {
        this.scope = scope;
        this.mainClass = mainClass;
    }

    public CallGraph build() throws ClassHierarchyException, CallGraphBuilderCancelException {
        long start = System.currentTimeMillis();
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);
        Warnings.clear();
        AnalysisOptions options = new AnalysisOptions();
        Iterable<Entrypoint> entrypoints =
                com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, mainClass);
        options.setEntrypoints(entrypoints);
        // you can dial down reflection handling if you like
        options.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);
        AnalysisCache cache = new AnalysisCacheImpl();
        // other builders can be constructed with different Util methods

        ContextSelector solarSelector = SolarSelector.makeSolarSelector();
        SSAContextInterpreter solarInterpreter = SolarInterpreter.makeSolarInterpreter();
        options.setUseConstantSpecificKeys(true);
        builder = Util.makeZeroOneContainerCFABuilder(
                options, cache, cha, solarSelector, solarInterpreter);
        // CallGraphBuilder<InstanceKey> builder = Util.makeNCFABuilder(1, options, cache, cha);
        System.out.println("building call graph...");

        PropagationSystem pSys = builder.getPropagationSystem();

        CallGraph cg = builder.makeCallGraph(options, null);

        long end = System.currentTimeMillis();
        System.out.println("done");
        System.out.println("took " + (end-start) + "ms");
        System.out.println(CallGraphStats.getStats(cg));
        printReflectionCall();
        return cg;
    }

    public void addToPts(PointerKey ptr, InstanceKey obj) {
        OrdinalSet<InstanceKey> set = builder.getPointerAnalysis().getPointsToSet(ptr);
        if (! set.contains(obj)) {
            builder.getPropagationSystem().newConstraint(ptr, obj);
        }
    }

    public void logReflectionCall(CGNode site, Set<IMethod> methodSet) {
        this.reflectionCalls.put(site, methodSet);
    }

    public void printReflectionCall() {
        for (CGNode site: reflectionCalls.keySet()) {
            System.out.println(site.getMethod().getName() + "->");
            System.out.println(reflectionCalls.get(site));
        }
    }

    static Solar solar;

    public static Solar get() {
        return solar;
    }

    public static void make(AnalysisScope scope, String mainClass) {
        solar = new Solar(scope, mainClass);
    }
}
