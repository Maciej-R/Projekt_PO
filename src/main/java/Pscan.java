import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Scan opened ports
 */
public class Pscan implements Runnable {

    private HashMap<String, String> parameters;

    /**
     * @param p Parameters' HashMap
     */
    public Pscan(HashMap<String, String> p) {

        parameters = p;

    }

    @Override
    public void run() {

        //No target given
        if (parameters.get("ta") == null) {
            Presenter.show("Target host address required (-ta)");
            return;
        }

        //Local address to use
        InetAddress addr = null;

        try {

            //No source address specified on parameter
            if (parameters.get("sa") == null) addr = InetAddress.getLocalHost();
            else addr = InetAddress.getByName(parameters.get("sa"));

        } catch (Exception e) { Presenter.show("Wrong source address"); }

        //Remote address
        InetAddress target = null;

        try {
            target = InetAddress.getByName(parameters.get("ta"));
        }catch(Exception e){ Presenter.show("Error in port scanning\n" + e.getMessage()); }

        StringBuilder builder = new StringBuilder(100);
        builder.append(String.format("Ports opened at host %s", target.toString()));
        int initLen = builder.length();

        String str = parameters.get("n");
        int nthreads = 1;
        if(str != null) nthreads = Integer.parseInt(str);

        int rangeStart = parameters.get("rs") == null ? 0 : Integer.parseInt(parameters.get("rs"));
        int rangeEnd = parameters.get("re") == null ? 65536 : Integer.parseInt(parameters.get("re")) + 1;

        LinkedList trgt = new LinkedList();
        trgt.add(target);
        //RangeEnd - one behind real end due to <rangeStart, rangeEnd)
        ParallelTask ptask = new ParallelTask(nthreads, rangeStart, rangeEnd, builder, RangeScan.class.getDeclaredConstructors()[0], trgt);
        Thread t = new Thread(new ParallelTaskExecutioner(ptask));
        t.start();

        try{
            t.join();
        }catch (Exception e){}

       if (builder.length() == initLen){
            Presenter.show("No ports opened at " + target);
            return;
       }

        Presenter.show(builder.toString());

    }

}
