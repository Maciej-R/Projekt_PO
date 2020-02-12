import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestHosts {

    private HashMap<String, String> parameters;

    @org.junit.jupiter.api.Test
    void printInfo(){

        Hosts h = new Hosts(parameters);

        h.run();

        try {
            System.in.read();
        }catch (Exception e){}

    }

    @BeforeAll
    void co(){

        parameters = new HashMap<String, String>();
        parameters.put("h", "10.0.0.15");
        //parameters.put("h", "192.168.0.45");
        parameters.put("n", "10");
        parameters.put("t", "1000");

    }

}
