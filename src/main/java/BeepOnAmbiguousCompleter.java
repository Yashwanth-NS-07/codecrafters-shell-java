import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;

import java.util.List;

public class BeepOnAmbiguousCompleter implements Completer {
    private final Completer delegate;
    private final Terminal terminal;

    public BeepOnAmbiguousCompleter(Completer delegate, Terminal terminal) {
        this.delegate = delegate;
        this.terminal = terminal;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        delegate.complete(reader, line, candidates);

        // Beep when multiple matches exist
        if (candidates.size() > 1) {
            terminal.writer().write('\007');
            terminal.writer().flush();
        }
    }
}
