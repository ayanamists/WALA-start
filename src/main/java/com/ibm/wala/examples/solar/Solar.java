package com.ibm.wala.examples.solar;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

import java.util.ArrayList;
import java.util.Collection;

public class Solar {

    AnalysisScope scope;
    String mainClass;
    SSAPropagationCallGraphBuilder builder;

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
        System.out.println(cha.getNumberOfClasses() + " classes");
        System.out.println(Warnings.asString());
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
        builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA,
                options, cache, cha, solarSelector, solarInterpreter);
        // CallGraphBuilder<InstanceKey> builder = Util.makeNCFABuilder(1, options, cache, cha);
        System.out.println("building call graph...");

        PropagationSystem pSys = builder.getPropagationSystem();

        CallGraph cg = builder.makeCallGraph(options, null);

        long end = System.currentTimeMillis();
        System.out.println("done");
        System.out.println("took " + (end-start) + "ms");
        System.out.println(CallGraphStats.getStats(cg));
        return cg;
    }

    static Solar solar;

    public static Solar get() {
        return solar;
    }

    public static void make(AnalysisScope scope, String mainClass) {
        solar = new Solar(scope, mainClass);
    }
}
