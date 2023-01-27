package com.ibm.wala.examples.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.examples.solar.MethodSig;
import com.ibm.wala.examples.solar.Solar;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
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

	@SuppressWarnings("unchecked")
	public static <T> T getValue(ContextItem t) {
		return ((ContextItem.Value<T>) t).getValue();
	}

	public static Set<IClass> searchContainingClass(IClassHierarchy cha, MethodSig methodSig) {
		Set<IClass> s = new HashSet<>();

		for (IClass i : cha) {
			boolean is = i.getAllMethods()
					.stream()
					.map(MethodSig::getMethodSig)
					.anyMatch(sig -> sig.equals(methodSig));
			if (is) {
				s.add(i);
			}
		}

		return s;
	}

	public static<T> List<T> cons(List<T> list, T t) {
		ArrayList<T> result = new ArrayList<T>(list);
		result.add(0, t);
		return result;
	}

	public static TypeReference getType(CGNode node, int i) {
		TypeInference ti = TypeInference.make(node.getIR(), true);
		return ti.getType(i).getTypeReference();
	}

	public static<T> List<T> toList(Iterator<T> iterator) {
		List<T> list = new ArrayList<>();
		iterator.forEachRemaining(list::add);
		return list;
	}

	public static Set<TypeReference> getUppers(IClass c) {
		return getUpperClasses(c)
				.stream()
				.map(IClass::getReference)
				.collect(Collectors.toSet());
	}

	public static Set<IClass> getUpperClasses(IClass c) {
		Set<IClass> res = new HashSet<>();
		Queue<IClass> workList = new LinkedList<>();
		workList.add(c);
		while (! workList.isEmpty()) {
			IClass now = workList.poll();
			if (! res.contains(now)) {
				res.add(now);
				if (now.getSuperclass() != null) {
					workList.add(now.getSuperclass());
				}
				workList.addAll(now.getDirectInterfaces());
			}
		}
		return res;
	}
}
