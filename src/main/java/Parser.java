import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {
    private Terminal terminal;
    private LineReader lineReader;
    private char singleQuote = '\'';
    private char doubleQuote = '"';
    private char backslash = '\\';
    private char[] charactersToEscapeInsideDoubleQuotes = { doubleQuote, backslash, '$', '`'};

    public Parser(Terminal terminal) {
        List<String> builtIns = new ArrayList<>();
        Arrays.stream(Main.BuiltIns.values()).forEach(b -> builtIns.add(b.toString()));
        StringsCompleter stringsCompleter = new StringsCompleter(builtIns);
        LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
                .completer(stringsCompleter)
                .terminal(terminal);
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
            for(char c: lineReader.readLine(prompt).toCharArray()) {
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
