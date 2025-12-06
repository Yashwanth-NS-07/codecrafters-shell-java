import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Main {
    private static boolean isRunning = true;
    private static String PATH = "PATH";
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

            try {
                BuiltIns builtIn = BuiltIns.valueOf(cmdAndArgs[0]);
                if(builtIn == BuiltIns.exit) {
                    isRunning = false;
                    continue;
                } else {
                    builtIn.doTask(cmdAndArgs[1]);
                }
            } catch (IllegalArgumentException iae) {
                Optional<File> file = checkForExecutableFileInPath(cmdAndArgs[0]);
                if(file.isEmpty()) {
                    System.out.println(cmdAndArgs[0] + ": command not found");
                } else {
                    try {
                        runProcess(file.get(), cmdAndArgs[1]);
                    } catch (IOException | ExecutionException | InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                }
                continue;
            }
        }
    }

    private static void runProcess(File file, String args) throws IOException, ExecutionException, InterruptedException {
        List<String> programWithArgs = new ArrayList<>();
        programWithArgs.add(file.getName());
        programWithArgs.addAll(Arrays.asList(args.split(" ")));
        ProcessBuilder processBuilder = new ProcessBuilder(programWithArgs);
        Process process = processBuilder.start();
        byte[] outputArray = process.getInputStream().readAllBytes();
        System.out.print(new String(outputArray));
        byte[] errorArray = process.getErrorStream().readAllBytes();
        System.out.print(new String(errorArray));
        process.onExit().get();
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
        String path = System.getenv(PATH);
        String[] dirs = path.split(File.pathSeparator);
        for(String dir: dirs) {
            File file = new File(dir);
            if(isExecutableFile(file) && file.getName().equals(program)) {
                return Optional.of(file);
            } else if(file.isDirectory()) {
                for(File internalFile: Objects.requireNonNull(file.listFiles())) {
                    if(isExecutableFile(internalFile) && internalFile.getName().equals(program)) {
                        return Optional.of(internalFile);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isExecutableFile(File file) {
        return file.isFile() && file.canExecute();
    }

    public enum BuiltIns {
        exit {
            public void doTask(String args) {}
        },
        echo {
            public void doTask(String args) {
                System.out.println(args);
            }
        },
        type {
            public void doTask(String args) {
                String program = args.trim();
                try {
                    BuiltIns.valueOf(program);
                    System.out.println(program + " is a shell builtin");
                } catch (IllegalArgumentException iae) {
                    Optional<File> executableFile = checkForExecutableFileInPath(program);
                    if(executableFile.isEmpty()) {
                        System.out.println(program + ": not found");
                    } else {
                        System.out.println(program + " is " + executableFile.get().getAbsolutePath());
                    }
                }
            }
        },
        pwd {
            public void doTask(String args) {
                System.out.println(System.getProperty("user.dir"));
            }
        },
        cd {
            public void doTask(String args) {
                boolean setSuccessfull = true;
                File dir = new File(args);
                if(dir.getPath().equals("~")) {
                    System.setProperty("user.dir", System.getenv("HOME"));
                } else if(dir.isAbsolute()) {
                    if(dir.isDirectory()) System.setProperty("user.dir", dir.getAbsolutePath());
                    else setSuccessfull = false;
                } else {
                    dir = new File(System.getProperty("user.dir"), dir.getPath());
                    try {
                        if(dir.isDirectory()) System.setProperty("user.dir", dir.getCanonicalPath());
                        else setSuccessfull = false;
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
                if(!setSuccessfull) System.out.println(args + ": No such file or directory");
            }
        };

        public abstract void doTask(String args);
    }
}
