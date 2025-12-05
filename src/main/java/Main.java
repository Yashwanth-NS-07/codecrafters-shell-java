import java.io.File;
import java.util.Objects;
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
                builtIn = BuiltIns.valueOf(cmdAndArgs[0]);
            } catch (IllegalArgumentException iae) {
                System.out.println(cmdAndArgs[0] + ": command not found");
                continue;
            }

            switch (builtIn) {
                case exit -> {
                    isRunning = false;
                }
                case echo -> {
                    System.out.println(cmdAndArgs[1]);
                }
                case type -> {
                    cmdAndArgs[1] = cmdAndArgs[1].trim();
                    try {
                        BuiltIns.valueOf(cmdAndArgs[1]);
                        System.out.println(cmdAndArgs[1] + " is a shell builtin");
                    } catch (IllegalArgumentException iae) {
                        String output = checkForExecutableInPath(cmdAndArgs[1]);
                        System.out.println(output);
                    }
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

    private static String checkForExecutableInPath(String program) {
        String path = System.getenv("PATH");
        String[] dirs = path.split(File.pathSeparator);
        for(String dir: dirs) {
            File file = new File(dir);
            if(file.isFile() && file.getName().equals(program)) {
                return program + " is " + file.getAbsolutePath();
            } else if(file.isDirectory()) {
                for(File file1: Objects.requireNonNull(file.listFiles())) {
                    if(file1.isFile() && file1.getName().equals(program)) {
                        return program + " is " + file1.getAbsolutePath();
                    }
                }
            }
        }
        return program + ": not found";
    }

    public enum BuiltIns {
        exit,
        echo,
        type;
    }
}
