import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedList;

public class RangeScan implements Runnable {

    private int rangeStart;
    private int rangeEnd;
    private StringBuilder builder;
    private InetAddress target;

    RangeScan(int rS, int rE, StringBuilder b, LinkedList l){

        target = (InetAddress)l.get(0);
        rangeEnd = rE;
        rangeStart = rS;
        builder = b;

    }

    @Override
    public void run() {

        int cnt = 0;
        Socket soc = null;
        for(int port = rangeStart; port < rangeEnd; ++port){

            try {

                //Connecting from unbound socket works
                soc = new Socket();

                //Find available port
                /*while (!soc.isBound()) {

                    try {
                        soc.bind(new InetSocketAddress(addr, lport));
                        //Exception - port in use
                    } catch (Exception e) {++lport;}
                    //System.out.println(soc.toString());
                }*/
                //soc.setTcpNoDelay(true);

                //Destination identifier
                SocketAddress dest = new InetSocketAddress(target, port);
                //Try to connect
                soc.connect(dest, 500);

                if(soc.isConnected()){

                    if(cnt % 10 == 0) builder.append("\n\t");
                    ++cnt;
                    builder.append(String.format("%d, ", port));

                }
                soc.close();

            }catch(Exception e){//System.out.println(e.toString());
                try {//???????
                    soc.close();
                }catch(Exception exc){}
            }

        }

        if(cnt != 0) {
            //Last space
            builder.deleteCharAt(builder.length() - 1);
            //Last comma
            builder.deleteCharAt(builder.length() - 1);
        }

    }

}
