import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

public class ParserTest {
    @Test
    public void testParse() {
        String[] inputs = {
                " java -jar 'a' 'a'",
                "\"java\" \"-jar\" \"'a ' 'a'\" '\"a a a\"'"
        };
        for(String input: inputs) {
            Parser parser = new Parser(new Scanner(new ByteArrayInputStream(input.getBytes())));
            System.out.println("Program: " + parser.getProgram());
            int k = 1;
            for (String arg : parser.getArgs()) {
                System.out.println("Argument " + k++ + ": " + arg);
            }
        }
    }
}
