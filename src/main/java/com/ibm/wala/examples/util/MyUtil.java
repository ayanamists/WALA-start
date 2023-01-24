package com.ibm.wala.examples.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.ibm.wala.examples.solar.Solar;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.FileOfClasses;

public class MyUtil {

	// more aggressive exclusions to avoid library blowup
	  // in interprocedural tests
    private static final String EXCLUSIONS = "java\\/awt\\/.*\n" +
  		"javax\\/swing\\/.*\n" + 
  		"sun\\/awt\\/.*\n" + 
  		"sun\\/swing\\/.*\n" + 
  		"com\\/sun\\/.*\n" + 
  		"sun\\/.*\n" + 
  		"org\\/netbeans\\/.*\n" + 
  		"org\\/openide\\/.*\n" + 
  		"com\\/ibm\\/crypto\\/.*\n" + 
  		"com\\/ibm\\/security\\/.*\n" + 
  		"org\\/apache\\/xerces\\/.*\n" + 
  		"java\\/security\\/.*\n" + 
  		"";

    public static void addDefaultExclusions(AnalysisScope scope) throws UnsupportedEncodingException, IOException {
	    scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(MyUtil.EXCLUSIONS.getBytes("UTF-8"))));
    }

	public static boolean isApplication(CGNode node) {
		return node
				.getMethod()
				.getReference()
				.getDeclaringClass()
				.getClassLoader()
				.equals(ClassLoaderReference.Application);
	}

	public static PointerKey getPointerKey(CGNode node, int idx) {
		return Solar.get().getBuilder().getPointerKeyForLocal(node, idx);
	}
	  
}
