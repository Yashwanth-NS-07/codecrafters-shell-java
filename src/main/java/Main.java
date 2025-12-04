import java.util.Scanner;

public class Main {
    private static boolean isRunning = true;
    private static Scanner scanner = null;

    private static final String exit = "exit";
    private static final String echo = "echo";

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
            String[] cmdAndArgs = parse(scanner.nextLine().trim());
            assert cmdAndArgs.length == 2;

            switch (cmdAndArgs[0]) {
                case exit -> {
                    isRunning = false;
                }
                case echo -> {
                    System.out.println(cmdAndArgs[1]);
                }
                default -> {
                    System.out.println(cmdAndArgs[0] + ": command not found");
                }
            }
        }
    }

    private static String[] parse(String input) {
        String[] cmdAndArgs = new String[2];
        int programAndArgSeparatorIndex = input.indexOf(" ");
        if(programAndArgSeparatorIndex == -1) return new String[] {input, ""};

        cmdAndArgs[0] = input.substring(0, programAndArgSeparatorIndex);
        cmdAndArgs[1] = input.substring(programAndArgSeparatorIndex+1);
        return cmdAndArgs;
    }
}
