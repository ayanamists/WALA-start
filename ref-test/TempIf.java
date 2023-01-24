class TempIf {
    public static void main(String [] s) {
        Object b;
        if (unknown()) {
            b = new String("aaa");
        } else {
            b = new String("nnn");
        }
        doNothing(b);
    }

    public static void doNothing(Object s) {

    }

    public static boolean unknown() {
        return unknown();
    }
}