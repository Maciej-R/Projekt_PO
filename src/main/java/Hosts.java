import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.*;
import java.util.function.Predicate;

public class Hosts implements Runnable {

    private HashMap<String, String> parameters;

    Hosts(HashMap<String, String> param){
        parameters = param;
    }

    /**
     * All possible numbers in given address space with given parameters
     * @param l List for data
     * @param len Address space in bits
     * @param where Current location of recurrent calls (offset starting from right - least significant bit is 1)
     * @param value Starting number
     */
    void buildHostAddresses(List<Integer> l, int len, int where, int value){

        //Self explaining
        if(where == len) return;
        value = value | 0x01;
        l.add(value);
        for(int i = 0;; ++i){

            if(i == len - where){
                //Broadcast
                if(((value | 0x01) ^ (int)(Math.pow(2, len) - 1)) == 0) break; else l.add(value | 0x01);
                break;
            }
            if (value != 1 && i != 0) buildHostAddresses(l, len, where + i, value);
            value = value << 1;
            l.add(value);

        }

    }

    /**
     * All possible numbers in given address space
     * @param l List where results will be saved (not sorted)
     * @param len Address space in bits
     */
    void buildHostAddresses(List<Integer> l, int len){

        buildHostAddresses(l, len, 1, 0);

    }

    @Override
    public void run() {

        try {

            //Command output
            StringBuilder builder = new StringBuilder(100);
            InetAddress addr;
            //No address specified
            if(parameters.get("h") == null) {

                builder.append("By default using Java's getLocalHost() address\nNetwork:\n");

                addr = InetAddress.getLocalHost();

            }//Address from user
            else{

                builder.append("Hosts in network:\n");

                    String param_addr = parameters.get("h");
                    addr = InetAddress.getByName(param_addr);

            }

            //Interface info is needed to get prefix length
            NetworkInterface inter = NetworkInterface.getByInetAddress(addr);
            //All addresses available
            List<InterfaceAddress> iaddr = inter.getInterfaceAddresses();
            //Look for proper address
            for (InterfaceAddress ia : iaddr) {

                InetAddress a = ia.getAddress();
                //Only IPv4
                if (!(a instanceof Inet4Address)) continue;

                int mask_len = ia.getNetworkPrefixLength();
                byte baddr[] = a.getAddress();
                //Bit representation
                int mask = (int) Math.pow(2, mask_len) - 1;
                //Move to most significant byte in little endian notation
                mask = mask << (32 - mask_len);
                int haddr = Algorithm.btoi(baddr) & mask;
                builder.append("\t" + InetAddress.getByAddress(Algorithm.itob(haddr)).toString() + "\n");

                //Host addresses
                List<Integer> host_addrs = new LinkedList<>();
                buildHostAddresses(host_addrs, 32 - mask_len);
                Collections.sort(host_addrs);
                //One more - <,) range
                host_addrs.add(5);

                String param_val = parameters.get("n");
                int nthreads = param_val == null ? 1 : Integer.parseInt(param_val);

                //Timeout
                int tout = parameters.get("t") == null ? 500 : Integer.parseInt(parameters.get("t"));

                int nlines = Algorithm.count_if(builder.toString(), c -> c == '\n');

                LinkedList args = new LinkedList();
                args.add(host_addrs);
                args.add(haddr);
                args.add(tout);
                ParallelTask ptask = new ParallelTask(nthreads, 0, host_addrs.size(), builder, runPing.class.getDeclaredConstructors()[0], args);
                        //runPing.class.getDeclaredConstructor(int.class, int.class, StringBuilder.class, LinkedList.class));
                Thread t = new Thread(new ParallelTaskExecutioner(ptask));
                t.start();
                t.join();

                //Send output
                int nhosts = Algorithm.count_if(builder.toString(), c -> c == '\n');
                if(nhosts > nlines) nhosts -= nlines;
                else nhosts = 0;
                Presenter.show(builder.toString() + "\n Number of hosts: " + nhosts);

            }

        }catch (Exception e){Presenter.show(e.toString());}


    }

}
