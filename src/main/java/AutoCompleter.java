import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;

import javax.sound.sampled.Line;
import java.util.ArrayList;
import java.util.List;

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
//        if(canBeCompletedPartially(reader, line.line(), possibleCompletionsList)) return;
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

    private boolean canBeCompletedPartially(LineReader reader, String line, List<String> possibleCompletionsList) {
        int minLength = minLength(possibleCompletionsList);
        StringBuilder commonCharacters = new StringBuilder();
        for(int i = line.length(); i < minLength; i++) {
            boolean isCommon = true;
            char c = possibleCompletionsList.get(0).charAt(i);
            for(int j = 1; j < possibleCompletionsList.size(); j++) {
                if(possibleCompletionsList.get(j).charAt(i) != c) {
                    isCommon = false;
                }
            }
            if(isCommon) {
                commonCharacters.append(c);
            }
        }
        if(commonCharacters.isEmpty()) {
            return false;
        }
        reader.getBuffer().write(commonCharacters.toString());
        return true;
    }
    private int minLength(List<String> list) {
        if(list.isEmpty()) return 0;
        int minLength = list.get(0).length();
        for(String s: list) {
            if(s.length() < minLength) minLength = s.length();
        }
        return minLength;
   }

    private List<String> possibleCompletionList(String prefix, List<Candidate> candidates) {
        List<String> completionList = new ArrayList<>();
        for(Candidate candidate: candidates) {
            if(candidate.value().startsWith(prefix)) {
                completionList.add(candidate.value());
            }
        }
        return completionList;
    }
}
