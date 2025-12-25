import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import javax.sound.sampled.Line;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    private static boolean isRunning = true;
    private static final String PATH = "PATH";
    private static Parser parser;
    private static final Map<String, File> executablesInPath = new Hashtable<>();

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
        }));
        try {
            init();
            parser = new Parser(setupTerminal(),  executablesInPath);
            startShell();
        } catch (IOException e) {
            System.err.println("Failed to start shell: " + e.getMessage());
        }
    }
    private static void init() {
        // adding executables
        executablesInPath.clear();
        String path = System.getenv(PATH);
        String[] dirs = path.split(File.pathSeparator);
        for(String dir: dirs) {
            File file = new File(dir);
            if(isExecutableFile(file)) {
                executablesInPath.put(file.getName(), file);
            } else if(file.isDirectory()) {
                for(File internalFile: Objects.requireNonNull(file.listFiles())) {
                    if(isExecutableFile(internalFile)) {
                        executablesInPath.put(internalFile.getName(), internalFile);
                    }
                }
            }
        }
    }

    private static Terminal setupTerminal() throws IOException {
        TerminalBuilder terminalBuilder = TerminalBuilder.builder();
        return terminalBuilder.build();
    }

    public static void startShell() throws IOException {
        while(isRunning) {
            System.err.flush();
            List<Program> programs = parser.takeInput();
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
                if(isLast) {
                    processBuilder.inheritIO();
                    processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
                }
                else {
                    processBuilder.redirectError(ProcessBuilder.Redirect.PIPE)
                            .redirectOutput(ProcessBuilder.Redirect.PIPE)
                            .redirectInput(ProcessBuilder.Redirect.PIPE);
                }
                if(program.getWriteTo().isPresent()) {
                    if(program.getIsAppend()) {
                        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(program.getWriteTo().get())));
                    } else {
                        processBuilder.redirectOutput(ProcessBuilder.Redirect.to(new File(program.getWriteTo().get())));
                    }

                }
                if(program.getWriteErrorTo().isPresent()) {
                    if(program.getIsErrorAppend()) {
                        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(new File(program.getWriteErrorTo().get())));
                    } else {
                        processBuilder.redirectError(ProcessBuilder.Redirect.to(new File(program.getWriteErrorTo().get())));
                    }
                }
                Process process = processBuilder.start();
                if(output != null && output.output != null && !output.output.isEmpty()) {
                    process.getOutputStream().write(output.output.getBytes(StandardCharsets.UTF_8));
                    process.getOutputStream().close();
                }
                process.waitFor();
            }
        }
    }

    private static void runProcessParallelly(List<Program> programs) throws IOException, InterruptedException {
        List<ProcessBuilder> processBuilders = new ArrayList<>();
        for (int i = 0; i < programs.size(); i++) {
            Program program = programs.get(i);
            boolean isLast = i == programs.size() - 1 ? true: false;
            if (checkForExecutableFileInPath(program.getProgram()).isEmpty()) {
                System.out.println(program.getProgram() + ": command not found");
                break;
            }
            ProcessBuilder pb = new ProcessBuilder()
                    .command(List.of(program.getProgramAndArgs()))
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectInput(ProcessBuilder.Redirect.PIPE);
            if(isLast) {
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT);
            }
            Optional<String> writeTo = program.getWriteTo();
            Optional<String> writeErrorTo = program.getWriteErrorTo();
            if (writeTo.isPresent()) {
                if (program.getIsAppend()) {
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(writeTo.get())));
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.to(new File(writeTo.get())));
                }
            }
            if (writeErrorTo.isPresent()) {
                if (program.getIsErrorAppend()) {
                    pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(writeErrorTo.get())));
                } else {
                    pb.redirectError(ProcessBuilder.Redirect.to(new File(writeErrorTo.get())));
                }
            }
            processBuilders.add(pb);
        }
        List<Process> processes = ProcessBuilder.startPipeline(processBuilders);
        for(int i = 0; i < processes.size(); i++) {
            if(i == processes.size()-1) {
                processes.get(i).waitFor();
            }
        }
    }

    private static boolean checkForBuiltins(List<Program> programs) {
        for(Program program: programs) {
            try {
                BuiltIns.valueOf(program.getProgram());
                return true;
            } catch (IllegalArgumentException ignored) {
            }
        }
        return false;
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
        history {
            public BuiltInsOutput doTask(String... args) {
                History history = parser.lineReader.getHistory();
                StringBuilder errorOutput = new StringBuilder();
                StringBuilder output = new StringBuilder();
                if(args.length > 0) {
                    try {
                        int count = Integer.parseInt(args[0]);
                        if(args.length > 1) {
                            errorOutput.append("history: too many arguments");
                        } else {
                            if(count < 0) {
                                errorOutput.append("history: ")
                                        .append(count)
                                        .append(" invalid option");
                            } else {
                                output.append(printableFormat(history, count));
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        if(args[0].equals("-r")) {
                            if(args.length == 1) {
                                errorOutput.append("history: no file input");
                            } else {
                                try {
                                    addToHistory(history, new File(args[1]));
                                } catch (IOException e) {
                                    errorOutput.append("history: ")
                                            .append(e.getMessage());
                                }
                            }
                        } else if(args[0].equals("-w")) {
                            if(args.length == 1) {
                                errorOutput.append("history: no file output");
                            } else {

                            }
                        } else {
                            errorOutput.append("history: unknown argument ")
                                    .append(args[0]);
                        }
                    }
                } else {
                    output.append(printableFormat(history, history.size()));
                }
                return new BuiltInsOutput(output.toString(), errorOutput.toString(), 0);
            }
            private void addToHistory(History history, File file) throws IOException {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while((line = reader.readLine()) != null && !line.isEmpty()) {
                    history.add(line);
                }
            }
            private String printableFormat(History history, int count) {
                List<String> historyList = new ArrayList<>();
                for(int i = history.size() - count; i < history.size(); i++) {
                    history.moveTo(i);
                    historyList.add(history.index()+1 + "  " + history.current());
                }
                return String.join("\n", historyList);
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
