import java.util.*;

public class Parser {
    private List<Program> programs = new ArrayList<>();
    private char singleQuote = '\'';
    private char doubleQuote = '"';
    private char backslash = '\\';
    private char[] charactersToEscapeInsideDoubleQuotes = { doubleQuote, backslash, '$', '`'};

    public Parser(Scanner scanner) {
        parse(scanner);
    }

    private void parse(Scanner scanner) {
        boolean isDone = false;
        boolean insideQuote = false;
        boolean escape = false;
        char quoteValue = 0;
        boolean overrideAppender = false;
        String writeTo = null;
        boolean isAppend = false;

        List<String> args = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        while(!isDone) {
            if(insideQuote) System.out.print("> ");
            String line = scanner.nextLine();
            for(int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
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
                } else if(overrideAppender) {
                    if(c == ' ') {
                        if(sb.isEmpty()) continue;
                        else {
                            overrideAppender = false;
                            writeTo = sb.toString();
                            sb.delete(0, sb.length());
                        }
                    } else if(c == '>') {
                    } else if(c == '|') {
                    } else {
                        sb.append(c);
                    }
                } else if(insideQuote) {
                    if(c == quoteValue) {
                        if(i == line.length()-1) {
                            args.add(sb.toString());
                            sb.delete(0, sb.length());
                        }
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
                            if(!s.isEmpty() && !s.equals("1")) {
                                args.add(s);
                            }
                            sb.delete(0, sb.length());
                        }
                        overrideAppender = true;
                    } else if(c == backslash) {
                        escape = true;
                    } else {
                        sb.append(c);
                    }
                }
            }

            if(overrideAppender && sb.isEmpty()) {
                throw new IllegalArgumentException("syntax error near unexpected token `newline'");
            } else if(!insideQuote) {
                isDone = true;
                if(overrideAppender) {
                    writeTo = sb.toString();
                    sb.delete(0, sb.length());
                }
                if(!sb.isEmpty()) args.add(sb.toString());
                if(!args.isEmpty()) {
                    programs.add(new Program(args, writeTo, isAppend));
                }
            }
        }

    }

    public List<Program> getPrograms() {
        return this.programs;
    }
}
