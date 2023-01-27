import com.ibm.wala.examples.drivers.ScopeFileCallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class TestSolar {

    String path = "src/test/resources/ref-test/";
    String scopeFile = "src/test/resources/scopefile";
    @Test
    public void testAll() throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
        Path p = Paths.get(path);
        Files.list(p)
                .filter(i -> i.toString().endsWith(".java"))
                .forEach(i -> {
                    String str = i.getFileName().toString();
                    try {
                        String name = str.split(Pattern.quote("."))[0];
                        System.out.println("-----------------" + name + "----------------");
                        ScopeFileCallGraph.main(new String[] { "-scopeFile", scopeFile, "-mainClass",
                                "L" + name});
                        System.out.println("------------------------------------------");
                    } catch (IOException | ClassHierarchyException | CallGraphBuilderCancelException e) {
                        throw new RuntimeException(e);
                    }
                });

    }
}
