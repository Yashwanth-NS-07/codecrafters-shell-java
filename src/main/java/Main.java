import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("$ ");
        String input = sc.nextLine().trim();
        System.out.println(input + ": command not found");
        sc.close();
    }
}
