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
        boolean outputRedirect = false;
        boolean isAppend = false;
        String writeTo = null;
        boolean errorRedirect = false;
        String writeErrorTo = null;
        boolean isErrorAppend = false;

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
                            if(!s.isEmpty()) {
                                if(s.equals("2")) {
                                    isErrorAppend = false;
                                    errorRedirect = true;
                                } else if(s.equals("1")) {
                                    isAppend = false;
                                    outputRedirect = true;
                                } else {
                                    sb.append(s);
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

    }

    public List<Program> getPrograms() {
        return this.programs;
    }
}
