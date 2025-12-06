import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

public class ParserTest {
    @Test
    public void testParse() {
        Parser parser = new Parser(new Scanner(new ByteArrayInputStream(" java -jar 'a' 'a'".getBytes())));
        System.out.println("Program: " + parser.getProgram());
        int k = 1;
        for(String arg: parser.getArgs()) {
            System.out.println("Argument " + k++ + ": " + arg);
        }
    }
}
