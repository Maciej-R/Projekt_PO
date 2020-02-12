import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestPscan {

    private HashMap<String, String> parameters;

    @Test
    public void scan(){

        (new Pscan(parameters)).run();

        try {
            System.in.read();
        }catch (Exception e){}

    }

    @BeforeAll
    void co(){

        parameters = new HashMap<String, String>();
        //parameters.put("ta", "212.77.98.9");
        //parameters.put("sa", "192.168.0.45");
        parameters.put("ta", "192.168.11.1");
        parameters.put("sa", "10.0.0.15");
        parameters.put("n", "10");

    }

}
