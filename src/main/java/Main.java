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
                    String program = cmdAndArgs[1].trim();
                    try {
                        BuiltIns.valueOf(program);
                        System.out.println(program + " is a shell builtin");
                    } catch (IllegalArgumentException iae) {
                        Optional<File> executableFile= checkForExecutableFileInPath(program);
                        if(executableFile.isEmpty()) {
                            System.out.println(program + ": not found");
                        } else {
                            System.out.println(program + " is " + executableFile.get().getAbsolutePath());
                        }
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

    private static Optional<File> checkForExecutableFileInPath(String program) {
        String path = System.getenv("PATH");
        String[] dirs = path.split(File.pathSeparator);
        for(String dir: dirs) {
            File file = new File(dir);
            if(file.isFile() && file.getName().equals(program) && file.canExecute()) {
                return Optional.of(file);
            } else if(file.isDirectory()) {
                for(File internalFile: Objects.requireNonNull(file.listFiles())) {
                    if(internalFile.isFile() && internalFile.canExecute() && internalFile.getName().equals(program)) {
                        return Optional.of(internalFile);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public enum BuiltIns {
        exit,
        echo,
        type;
    }
}
