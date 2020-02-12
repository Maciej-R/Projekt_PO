import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Stream;

public class HopCount implements Runnable {

    private HashMap<String, String> parameters;

    public HopCount(HashMap<String, String> p ) { parameters = p; }

    @Override
    public void run() {

        String taddrS = parameters.get("ta");

        if(taddrS == null){

            Presenter.show("Hopcount requires target addresss (-ta)");
            return;

        }

        InetAddress addr = null;

        try{

            addr = InetAddress.getByName(taddrS);

        }catch(Exception e) { Presenter.show("Wrong target address"); }

        StringBuilder builder = new StringBuilder(100);
        builder.append(String.format("Hop count to %s\n", addr.toString()));

        int maxttl = parameters.get("mttl") == null ? 255 : Integer.parseInt(parameters.get("mttl"));

        NetworkInterface netint = null;
        if(parameters.get("netint") != null)
            try{
                netint = NetworkInterface.getByName(parameters.get("netint"));
            }catch (Exception e) {builder.append("Wrong interface name, using any available\n");}

        if(parameters.get("sa") != null) {
            try {
                netint = NetworkInterface.getByInetAddress(InetAddress.getByName(parameters.get("sa")));
            }catch(Exception e) { Presenter.show("Network interface error"); return; }
        }

        if(netint == null) {

            try {
                Stream<NetworkInterface> ifS = NetworkInterface.networkInterfaces();
                Iterator<NetworkInterface> it = ifS.iterator();
                netint = it.next();
            }catch(SocketException e) { Presenter.show("Network interface error"); return; }

        }

        int tout = parameters.get("t") == null ? 3000 : Integer.parseInt(parameters.get("t"));
        byte ttl;
        boolean is = false;
        for(ttl = 1; ttl < maxttl; ++ttl){

            try {
                is = addr.isReachable(netint, ttl, tout);
                if(is) break;
            }catch (Exception e) {Presenter.show("Error sending");}

        }

        if(is) builder.append(String.format("Result: %d", ttl));
        else builder.append("Host unreachable");

        Presenter.show(builder.toString());

    }

}
