import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AutoCompleter implements Completer {
    private final Completer delegate;
    private final Terminal terminal;
    private int tabCount = 0;
    private String parseLine = null;

    public AutoCompleter(Completer delegate, Terminal terminal) {
        this.delegate = delegate;
        this.terminal = terminal;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        delegate.complete(reader, line, candidates);

        List<String> possibleCompletionsList = possibleCompletionList(line.line(), candidates);

        // Beep when multiple matches exist
        if (possibleCompletionsList.size() > 1) {
            terminal.writer().write('\007');
            terminal.writer().flush();
            if(tabCount == 0) {
                tabCount++;
                parseLine = line.line();
            }
            else if(tabCount == 1 && line.line().equals(parseLine)) {
                terminal.writer().write('\n');
                terminal.writer().write(String.join("  ", possibleCompletionsList));
                terminal.writer().write('\n');
                terminal.writer().write("$ " + parseLine);
                terminal.writer().flush();
                tabCount = 0;
            }
        }
    }

    private List<String> possibleCompletionList(String prefix, List<Candidate> candidates) {
        Set<String> completionList = new TreeSet<>();
        for(Candidate candidate: candidates) {
            if(candidate.value().startsWith(prefix)) {
                completionList.add(candidate.value());
            }
        }
        return completionList.stream().toList();
    }
}
