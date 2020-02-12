import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

/**
 * Class implements listing opened ports in its run() method
 */
public class Ports implements Runnable{

    @Override
    public void run() {

        StringBuilder builder = new StringBuilder(100);
        builder.append("Ports in use:");
        for(int port = 0, cnt = 0; port < 65536; ++port){

            ServerSocket soc;

            //Exception if port is already in use
            try{

                soc = new ServerSocket(port);
                soc.close();

            }catch (Exception e){

                if(cnt % 10 == 0) builder.append("\n\t");
                ++cnt;
                builder.append(String.format("%5d, ", port));

            }

        }
        //Last space
        builder.deleteCharAt(builder.length() - 1);
        //Last comma
        builder.deleteCharAt(builder.length() - 1);

        Presenter.get().show(builder.toString());

    }

    @Test
    public void check(){

        (new Thread(new Ports())).start();
        try{
            System.in.read();
        }catch (Exception e){}

    }

}
