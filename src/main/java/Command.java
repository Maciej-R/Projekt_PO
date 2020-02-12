import java.util.HashMap;

/**
 * Keeps name of command and
 * map of name-value of parameters
 */
public class Command {

    public HashMap<String, String> parameters;
    public String command;

    Command(){

        parameters = new HashMap<>(10);
        command = "";

    }

}