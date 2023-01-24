import java.lang.reflect.Method;

public class ArgsRefine {

    public static void main(String[] args) throws Exception {
        invokePrint(new Class[]{Object.class, Object.class});
    }

    static void invokePrint(Class<?>[] paramTypes) throws Exception {
        Method print = B.class.getMethod("print", paramTypes);
        B b = new B();
        Object[] args = new Object[]{"hello", "hello"};
        print.invoke(b, args); // <B: void print(Object,Object)>
    }
}

class B {

    public void print(Object o) {
        System.out.println("B.print(Object)");
    }

    public void print(Object o1, Object o2) {
        System.out.println("B.print(Object,Object)");
    }

    public void print(Object o, B b) {
        System.out.println("B.print(Object,B)");
    }

    public void print(Object o1, Object o2, Object o3) {
        System.out.println("B.print(Object,Object,Object)");
    }
}
