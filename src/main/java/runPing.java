import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

/**
 * Parallel check of host reachability
 */
public class runPing implements Runnable{

    private StringBuilder builder;
    private List<Integer> range;
    private int address;
    private int tout;

    /**
     *
     * @param rangeStart
     * @param rangeEnd
     * @param b StringBuilder for saving results
     * @param lobj
     */
    runPing(int rangeStart, int rangeEnd, StringBuilder b, LinkedList lobj){

        range = ((LinkedList)lobj.get(0)).subList(rangeStart, rangeEnd);
        address = (int)lobj.get(1);
        builder = b;
        tout = (int)lobj.get(2);

    }

    @Override
    public void run() {

        try {

            for (int i = 0; i < range.size(); ++i) {

                //Host address
                int toPing = address | range.get(i);

                //Java representation of toPing
                InetAddress topaddr = InetAddress.getByAddress(Algorithm.itob(toPing));
                //Ping
                boolean is = topaddr.isReachable(tout);
                //If positive save result
                if(is) {
                    builder.append(String.format("%s up\n", topaddr.toString()));
                }

            }

        }catch (Exception e){}

    }

}