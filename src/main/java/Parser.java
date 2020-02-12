import java.text.ParseException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Parser {

    //Only static usage
    private Parser(){}

    /**
     *
     * @param arg - input from command line
     * @return input parsed to Command class
     */
    static Command parse(String arg) {

        Command cmd = new Command();
        //Command name
        Pattern expr = Pattern.compile("(^[a-z0-9]+\\s*)");
        Matcher mtch = expr.matcher(arg);
        //No command name found
        if(!mtch.lookingAt()) return null;
        String command = mtch.group(0);
        int len = command.length();
        command = command.trim();
        cmd.command = command;
        //Parameters search
        Pattern single_param = Pattern.compile("(-[a-z]{1,5}\\s[a-z0-9A-Z_.]*\\s*)");
        Matcher pmtch = single_param.matcher(arg.substring(len));
        //No match - just command
        if(!pmtch.lookingAt()) return cmd;
        //Go through params list
        for(int to_test = arg.length() - command.length() - 1;;){

            //Separate option name and val
            String opt_val[] = pmtch.group(0).split("\\s");
            //Save result
            cmd.parameters.put(opt_val[0].substring(1, opt_val[0].length()).trim(), opt_val[1].trim());
            if(pmtch.end() >= to_test) break;
            //Jumps tested region
            pmtch.region(pmtch.end(), to_test);
            pmtch.lookingAt();

        }

        return cmd;

    }

    @Test
    void parseTest(){

        Command res;

        res = Parser.parse("cmdname -s 56 -p jdfk");
        //System.out.println("!" + res.command + "!");
        assertTrue(res.command.equals("cmdname"));
        assertTrue(res.parameters.get("s").equals("56"));
        assertTrue(res.parameters.get("p").equals("jdfk"));

        res = Parser.parse("cmdname");
        assertTrue(res.command.equals("cmdname"));
        assertTrue(res.parameters.size() == 0);

        res = Parser.parse("cmdname -n 254");
        assertTrue(res.parameters.get("n").equals("254"));

    }

}
