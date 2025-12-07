import java.util.*;

public class Parser {
    private List<String> args = new ArrayList<>();
    private char singleQuote = '\'';
    private char doubleQuote = '"';
    private char backslash = '\\';
    private char[] charactersToEscapeInsideDoubleQuotes = { '"', '\\', '$', '`'};
    public Parser(Scanner scanner) {
        parse(scanner);
    }
    private void parse(Scanner scanner) {
        boolean isDone = false;
        boolean insideQuote = false;
        boolean escape = false;
        char quoteValue = 0;
        StringBuilder sb = new StringBuilder();
        while(!isDone) {
            if(insideQuote) System.out.print("> ");
            String line = scanner.nextLine();
            for(int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if(escape) {
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
                    escape = false;
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
                    } else if(c == backslash) {
                        escape = true;
                    } else {
                        sb.append(c);
                    }
                }
            }
            if(!insideQuote) {
                isDone = true;
                if(!sb.isEmpty()) args.add(sb.toString());
            }
        }

    }
    public String getProgram() {
        if(!args.isEmpty()) {
            return args.get(0);
        }
        return "";
    }
    public String[] getArgs() {
        if(args.size() > 1) {
            return args.subList(1, args.size()).toArray(new String[0]);
        }
        return new String[]{};
    }
}
