import java.util.Optional;
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
            String[] cmdAndArgs = parse(scanner.nextLine().trim());
            assert cmdAndArgs.length == 2;

            BuiltIns builtIn;
            try {
                builtIn = BuiltIns.valueOf(cmdAndArgs[0].toUpperCase());
            } catch (IllegalArgumentException iae) {
                System.out.println(cmdAndArgs[0] + ": command not found");
                continue;
            }

            switch (builtIn) {
                case EXIT -> {
                    isRunning = false;
                }
                case ECHO -> {
                    System.out.println(cmdAndArgs[1]);
                }
                case TYPE -> {
                    cmdAndArgs[1] = cmdAndArgs[1].trim();
                    try {
                        BuiltIns.valueOf(cmdAndArgs[1].toUpperCase());
                        System.out.println(cmdAndArgs[1] + " is a shell builtin");
                    } catch (IllegalArgumentException iae) {
                        System.out.println(cmdAndArgs[1] + ": not found");
                    }

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

    public enum BuiltIns {
        EXIT,
        ECHO,
        TYPE;
    }
}
