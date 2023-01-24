/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.examples.drivers;

import com.ibm.wala.analysis.reflection.ReflectiveInvocationInterpreter;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.core.util.warnings.Warnings;
import static com.ibm.wala.examples.util.MyUtil.*;

import com.ibm.wala.examples.solar.Solar;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.io.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * Driver that constructs a call graph for an application specified via a scope file.  
 * Useful for getting some code to copy-paste.    
 */
public class ScopeFileCallGraph {

  /**
   * Usage: ScopeFileCallGraph -scopeFile file_path [-entryClass class_name |
   * -mainClass class_name]
   * 
   * If given -mainClass, uses main() method of class_name as entrypoint. If
   * given -entryClass, uses all public methods of class_name.
   * 
   * @throws IOException
   * @throws ClassHierarchyException
   * @throws CallGraphBuilderCancelException
   * @throws IllegalArgumentException
   */
  public static void main(String[] args) throws IOException, ClassHierarchyException, IllegalArgumentException,
      CallGraphBuilderCancelException {

    Properties p = CommandLine.parse(args);
    String scopeFile = p.getProperty("scopeFile");

    String mainClass = p.getProperty("mainClass");

    AnalysisScope scope =
            AnalysisScopeReader.instance.readJavaScope(scopeFile, null, ScopeFileCallGraph.class.getClassLoader());
    // set exclusions.  we use these exclusions as standard for handling JDK 8
    addDefaultExclusions(scope);


    // System.out.println(cg);
    Solar.make(scope, mainClass);
    Solar s = Solar.get();
    CallGraph cg = s.build();
    SSAPropagationCallGraphBuilder builder = s.getBuilder();

    PointerAnalysis<?> pa = builder.getPointerAnalysis();
    HeapModel hm = pa.getHeapModel();

    for (CGNode i : cg) {
      if (i.getMethod().getName().toString().equals("forName") && isApplication(i)) {
        System.out.println(i.getMethod().getDescriptor());
        System.out.println(i.getIR());
        OrdinalSet<? extends InstanceKey> pts =
                pa.getPointsToSet(hm.getPointerKeyForLocal(i, 11));
        System.out.println(pts);
      }
    }
  }


}
