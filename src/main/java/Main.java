import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
            System.err.flush();
            System.out.print("$ ");
            Parser parser = new Parser(scanner);
            List<Program> programs = parser.getPrograms();
            if(programs.isEmpty()) continue;
            boolean hasBuiltins = checkForBuiltins(programs);
            try {
                if(hasBuiltins) {
                    runProcessSequentially(programs);
                } else {
                    runProcessParallelly(programs);
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void runProcessSequentially(List<Program> programs) throws IOException, InterruptedException {
        BuiltInsOutput output = null;
        boolean isLast = false;
        for(int i = 0; i < programs.size(); i++) {
            if(i == programs.size() - 1) isLast = true;
            Program program = programs.get(i);
            boolean isBuiltin = checkForBuiltins(List.of(programs.get(i)));
            if(isBuiltin) {
                output = BuiltIns.valueOf(program.getProgram()).doTask(program.getArgs());
                if(program.getWriteTo().isPresent()) {
                    try(PrintWriter writer = new PrintWriter(
                            new BufferedWriter(
                                new FileWriter(program.getWriteTo().get(), program.getIsAppend())))) {
                            writer.println(output.output);
                    }
                } else if(isLast) {
                    if(output.output != null && !output.output.isEmpty()) {
                        System.out.println(output.output);
                    }
                }
                if(program.getWriteErrorTo().isPresent()) {
                    try(PrintWriter writer = new PrintWriter(
                            new BufferedWriter(
                                    new FileWriter(program.getWriteErrorTo().get(), program.getIsErrorAppend())))) {
                        writer.print(output.errorOutput);
                    }
                }else if(isLast) {
                    if(output.errorOutput != null && !output.errorOutput.isEmpty()) {
                        System.err.println(output.errorOutput);
                    }
                }
            } else {
                if(checkForExecutableFileInPath(program.getProgram()).isEmpty()) {
                    System.out.println(program.getProgram() + ": command not found");
                    break;
                }
                ProcessBuilder processBuilder = new ProcessBuilder(program.getProgramAndArgs());
                processBuilder.inheritIO();
                if(program.getWriteTo().isPresent()) {
                    processBuilder.redirectOutput(maybeCreateFile(program.getWriteTo().get(), program.getIsAppend()));
                } else if(isLast) {
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                }
                if(program.getWriteErrorTo().isPresent()) {
                    processBuilder.redirectError(maybeCreateFile(program.getWriteErrorTo().get(), program.getIsErrorAppend()));
                } else if(isLast) {
                    processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
                }
                Process process = processBuilder.start();
                if(output != null && output.output != null && !output.output.isEmpty()) {
                    process.getOutputStream().write(output.output.getBytes(StandardCharsets.UTF_8));
                }
                process.waitFor();
                if(isLast) {
                    if(program.getWriteTo().isEmpty()) {
                        System.out.println(new String(process.getInputStream().readAllBytes()));
                    }
                    if(program.getWriteErrorTo().isEmpty()) {
                        System.err.println(new String(process.getErrorStream().readAllBytes()));
                    }
                }

            }
        }
    }

    private static void runProcessParallelly(List<Program> programs) throws IOException, InterruptedException {
        List<ProcessBuilder> processBuilders = new ArrayList<>();
        for(int i = 0; i < programs.size(); i++) {
            Program program =  programs.get(i);
            if(checkForExecutableFileInPath(program.getProgram()).isEmpty()) {
                System.out.println(program.getProgram() + ": command not found");
                break;
            }
            ProcessBuilder pb = new ProcessBuilder()
                    .command(List.of(program.getProgramAndArgs()))
                    .inheritIO();
            Optional<String> writeTo = program.getWriteTo();
            Optional<String> writeErrorTo = program.getWriteErrorTo();
            if(writeTo.isPresent()) {
                if(program.getIsAppend()) {
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(writeTo.get())));
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.to(new File(writeTo.get())));
                }
            } else if(i == programs.size()-1) {
                pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            }
            if(writeErrorTo.isPresent()) {
                if(program.getIsErrorAppend()) {
                    pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(writeErrorTo.get())));
                } else {
                    pb.redirectError(ProcessBuilder.Redirect.to(new File(writeErrorTo.get())));
                }
            } else {
                pb.redirectError(ProcessBuilder.Redirect.PIPE);
            }
            processBuilders.add(pb);
        }
        List<Process> processes = ProcessBuilder.startPipeline(processBuilders);
        for(int i = 0; i < processes.size(); i++) {
            if(i == processes.size()-1) {
                Process process = processes.get(i);
//                String output = new String(processes.get(i).getInputStream().readAllBytes());
//                if(!output.isEmpty()) System.out.print(output);
//                String error = new String(processes.get(i).getErrorStream().readAllBytes());
//                if(!error.isEmpty()) System.out.print(error);
                byte[] bytes = new byte[1024];
                int count;
                while((count = process.getInputStream().read(bytes)) != -1) {
                    System.out.print(new String(bytes, 0, count));
                }
                while((count = process.getErrorStream().read(bytes)) != -1) {
                    System.err.print(new String(bytes, 0, count));
                }
            }
        }
    }

    private static File maybeCreateFile(String fileName, boolean isAppend) throws IOException {
        File file = new File(fileName).getCanonicalFile();
        if(file.exists()) {
            if(isAppend) return file;
            else {
                if(!file.delete()) {
                    throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
                }
                try {
                    if(!file.createNewFile())
                        throw new RuntimeException("Failed to create file: " + file.getAbsolutePath());
                    return file;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if(!file.createNewFile()) {
                throw new RuntimeException("Failed to create file: " + file.getAbsolutePath());
            }
        }
        return file;
    }

    private static boolean checkForBuiltins(List<Program> programs) {
        for(Program program: programs) {
            try {
                BuiltIns.valueOf(program.getProgram());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
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
            public BuiltInsOutput doTask(String... args) {
                StringBuilder errorSb = new StringBuilder();
                if(args.length > 0 && args[args.length-1].endsWith(">")) {
                    errorSb.append("syntax error near unexpected token `newline`");
                    return new BuiltInsOutput("", errorSb.toString(), 1);
                } else {
                    isRunning = false;
                }
                return new BuiltInsOutput("", "", 1);
            }
        },
        echo {
            public BuiltInsOutput doTask(String... args) {
                return new BuiltInsOutput(String.join(" ", args), "", 0);
            }
        },
        type {
            public BuiltInsOutput doTask(String... args) {
                StringBuilder output = new StringBuilder();
                String error = "";
                int errorCode = 0;
                for(String program: args) {
                    try {
                        BuiltIns.valueOf(program);
                        output.append(program).
                                append(" is a shell builtin");
                    } catch (IllegalArgumentException iae) {
                        Optional<File> executableFile = checkForExecutableFileInPath(program);
                        if (executableFile.isEmpty()) {
                            output.append(program).
                                    append(": not found");
                            errorCode = 1;
                        } else {
                            output.append(program).
                                    append(" is ").
                                    append(executableFile.get().getAbsolutePath());
                            errorCode = 0;
                        }
                    }
                }
                return new BuiltInsOutput(output.toString(), error, errorCode);
            }
        },
        pwd {
            public BuiltInsOutput doTask(String... args) {
                return new BuiltInsOutput(System.getProperty("user.dir"), "", 0);
            }
        },
        cd {
            public BuiltInsOutput doTask(String... args) {
                if(args.length > 1) {
                    return new BuiltInsOutput("cd: too many arguments", "", 1);
                }
                boolean setSuccessfull = true;
                String path = args.length == 0? "~": args[0];
                File dir = new File(path);
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
                        System.err.println(e.getMessage());
                    }
                }
                if(!setSuccessfull) {
                    return new BuiltInsOutput("", "cd: " + path + ": No such file or directory", 1);
                }
                return new BuiltInsOutput("", "", 0);
            }
        };

        public abstract BuiltInsOutput doTask(String... args);
    }

    public record BuiltInsOutput(String output, String errorOutput, int errorCode) {
    }
}
