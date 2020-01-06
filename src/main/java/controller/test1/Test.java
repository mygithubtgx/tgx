package controller.test1;

public class Test {
    public static void main(String[] args) {
        int i=5, j=10; do {
            if(i>j) {
                break;
            } j--; i++;
        }while(j!=i);
        System.out.println( i + "," + j); }
}
