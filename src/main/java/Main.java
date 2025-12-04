import java.util.Scanner;

public class Main {
    private static boolean isRunning = true;
    private static Scanner scanner = null;

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
        }));
        scanner = new Scanner(System.in);
        startShell();
        scanner.close();
    }

    public static void startShell() {
        while(isRunning) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if(input.equals("exit")) break;
            System.out.println(input + ": command not found");
        }
    }
}
