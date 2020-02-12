import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class TestTraceroute {

    @Test
    void test(){

        HashMap<String, String> map = new HashMap<>();
        map.put("ta", "212.77.98.9");
        map.put("gw", "192.168.0.1");
        (new Thread(new Traceroute(map))).start();

        try {
            System.in.read();
        }catch (Exception e){}

    }

}
