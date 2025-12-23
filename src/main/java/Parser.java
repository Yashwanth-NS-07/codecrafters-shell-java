import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Parser {
    private Terminal terminal;
    private LineReader lineReader;
    private final char singleQuote = '\'';
    private final char doubleQuote = '"';
    private final char backslash = '\\';
    private final char[] charactersToEscapeInsideDoubleQuotes = { doubleQuote, backslash, '$', '`'};

    public Parser(Terminal terminal, Map<String, File> executablesInPath) {
        List<String> builtIns = new ArrayList<>();
        Arrays.stream(Main.BuiltIns.values()).forEach(b -> builtIns.add(b.toString()));
        StringsCompleter builtInsCompleter = new StringsCompleter(builtIns);
        StringsCompleter executablesCompleter = new StringsCompleter(executablesInPath.keySet());
        DefaultParser defaultParser = new DefaultParser();
        defaultParser.setEscapeChars(new char[]{});
        Completer completer = new AggregateCompleter(
                builtInsCompleter,
                executablesCompleter
        );

        AutoCompleter customCompleter = new AutoCompleter(completer, terminal);
        LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
                .completer(customCompleter)
                .parser(defaultParser)
                .terminal(terminal)
//                .option(LineReader.Option.MENU_COMPLETE, false)
//                .option(LineReader.Option.AUTO_MENU_LIST, false)
                .variable(LineReader.COMPLETE_PREFIX, true)
                .option(LineReader.Option.AUTO_LIST, false)
                .option(LineReader.Option.LIST_AMBIGUOUS, false)
                .option(LineReader.Option.AUTO_MENU, false);

        this.terminal = terminal;
        this.lineReader = lineReaderBuilder.build();
    }

    public List<Program> takeInput() throws IOException {
        List<Program> programs = new ArrayList<>();
        boolean isDone = false;
        boolean insideQuote = false;
        boolean escape = false;
        char quoteValue = 0;
        boolean outputRedirect = false;
        boolean isAppend = false;
        String writeTo = null;
        boolean errorRedirect = false;
        String writeErrorTo = null;
        boolean isErrorAppend = false;

        String prompt = "$ ";
        List<String> args = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        while(!isDone) {
            if(insideQuote) {
                prompt = "> ";
            }
            String line = lineReader.readLine(prompt);
            for(char c: line.toCharArray()) {
                if(escape) {
                    escape = false;
                    if(insideQuote && quoteValue == doubleQuote) {
                        boolean isEscapable = false;
                        for (char cc : charactersToEscapeInsideDoubleQuotes) {
                            if (c == cc) {
                                isEscapable = true;
                                break;
                            }
                        }
                        if (isEscapable) sb.append(c);
                        else {
                            sb.append('\\');
                            sb.append(c);
                        }
                    } else {
                        sb.append(c);
                    }
                } else if(outputRedirect) {
                    if(c == ' ') {
                        if(sb.isEmpty()) continue;
                        else {
                            outputRedirect = false;
                            writeTo = sb.toString();
                            sb.delete(0, sb.length());
                        }
                    } else if(c == '>') {
                        if(sb.equals("1") || sb.isEmpty()) {
                            isAppend = true;
                        }
                    } else if(c == '|') {
                    } else {
                        sb.append(c);
                    }
                } else if(errorRedirect) {
                    if (c == ' ') {
                        if(sb.isEmpty()) continue;
                        else {
                            errorRedirect = false;
                            writeErrorTo = sb.toString();
                            sb.delete(0, sb.length());
                        }
                    } else if(c == '>') {
                        if(sb.isEmpty()) {
                            isErrorAppend = true;
                        }
                    } else if(c == '|') {

                    } else {
                        sb.append(c);
                    }
                } else if(insideQuote) {
                    if(c == quoteValue) {
                        insideQuote = false;
                    } else if(quoteValue == doubleQuote && c == backslash) {
                        escape = true;
                    } else {
                        sb.append(c);
                    }
                } else {
                    if(c == singleQuote || c == doubleQuote) {
                        quoteValue = c == singleQuote? singleQuote: doubleQuote;
                        insideQuote = true;
                    } else if(c == '|') {
                        programs.add(new Program(args, writeTo, isAppend, writeErrorTo, isErrorAppend));
                        args.clear();
                    } else if(c == ' ') {
                        if(!sb.isEmpty()) args.add(sb.toString());
                        sb.delete(0, sb.length());
                    } else if(c == '>') {
                        if(!sb.isEmpty()) {
                            String s = sb.toString().trim();
                            if(!s.isEmpty()) {
                                if(s.equals("2")) {
                                    isErrorAppend = false;
                                    errorRedirect = true;
                                } else if(s.equals("1")) {
                                    isAppend = false;
                                    outputRedirect = true;
                                } else {
                                    args.add(s);
                                    outputRedirect = true;
                                }
                            }
                            sb.delete(0, sb.length());
                        } else {
                            isAppend = false;
                            outputRedirect = true;
                        }
                    } else if(c == backslash) {
                        escape = true;
                    } else {
                        sb.append(c);
                    }
                }
            }

            if((errorRedirect || outputRedirect) && sb.isEmpty()) {
                throw new IllegalArgumentException("syntax error near unexpected token `newline'");
            } else if(!insideQuote) {
                isDone = true;
                if(outputRedirect) {
                    writeTo = sb.toString();
                    sb.delete(0, sb.length());
                } else if(errorRedirect) {
                    writeErrorTo = sb.toString();
                    sb.delete(0, sb.length());
                }

                if(!sb.isEmpty()) args.add(sb.toString());
                if(!args.isEmpty()) {
                    programs.add(new Program(args, writeTo, isAppend, writeErrorTo, isErrorAppend));
                }
            }
        }
        return programs;
    }
}
