import java.util.List;
import java.util.Optional;

public class Program {
    private final String program;
    private final String[] args;
    private final String writeTo;
    private final boolean isAppend;

    public Program(List<String> args, String writeTo, boolean isAppend) {
        this.program = args.remove(0);
        this.args = args.toArray(new String[0]);
        this.writeTo = writeTo;
        this.isAppend = isAppend;
    }

    public String getProgram() { return this.program; }
    public String[] getArgs() { return this.args; }
    public Optional<String> getWriteTo() { return Optional.ofNullable(writeTo); }
    public boolean getIsAppend() { return this.isAppend; }
    public String[] getProgramAndArgs() {
        String[] programAndArgs = new String[args.length + 1];
        programAndArgs[0] = program;
        System.arraycopy(args, 0, programAndArgs, 1, programAndArgs.length - 1);
        return programAndArgs;
    }
}
