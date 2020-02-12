import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.channels.AsynchronousFileChannel;
import java.util.*;
import java.util.concurrent.Semaphore;

import static java.lang.Math.ceil;

/**
 * This class accepts messages to be displayed in console
 * and has interface to transfer read data from input
 */
public class Presenter {

    private static Presenter presenter = new Presenter();
    private static Queue<String> queue = new LinkedList<String>();
    private static Queue<String> input = new LinkedList<String>();
    private static String prompt = "$ ";
    private static boolean exit = false;

    // Modification of output queue
    private static Semaphore mutex = new Semaphore(1);
    //Modification of input queue - console read
    private static Semaphore imutex = new Semaphore(1);
    //Lets userInput function read input without interruption form getCommand
    private static Semaphore readmutex = new Semaphore(1);


    //Only one to have synchronized output
    /**
     * On class import creates threads to read and print
     */
    private Presenter(){

        new Thread(new Printer()).start();
        new Thread(new Listener()).start();

    }

    /**
     * @return Only instance of Presenter
     */
    public static Presenter get(){

        return presenter;

    }

    /**
     * Adds given String to queue of messages to display
     * @param txt - message to display
     */
    public static void show(String txt){

        try {
            //In case it's being modified by Printer
            mutex.acquire();
            queue.add(txt);

        }catch (Exception e) {
        }finally {
            mutex.release();
        }
    }

    /**
     * Prints out messages in blocks of ten lines
     * press enter to continue if longer
     */
    private class Printer implements Runnable{

        @Override
        public void run() {

            System.out.print(prompt);
            System.out.flush();

            while(!exit) {

                String txt = "";
                //Checking queue
                try {
                    mutex.acquire();
                    while ((txt = queue.poll()) == null) {
                        try {
                            mutex.release();
                            Thread.sleep(250);
                            mutex.acquire();
                        } catch (Exception e) {
                        }
                    }
                }catch(Exception e){}
                finally {
                    mutex.release();
                }

                //Number of lines in text
                int lines = Algorithm.count_if(txt, (Character c) -> {
                    return c == '\n';
                });


                System.out.print("\n");
                //Short message at once
                if (lines < 10) {

                    System.out.print(txt);
                    System.out.flush();

                }//Longer in blocks, wait for enter pressed to continue
                else {

                    String lns[] = txt.split("\n");
                    for (int j = 0; j < ceil(lns.length / 10.0); ++j) {

                        for (int i = 0; i < 10; ++i) {

                            int idx = i + j * 10;
                            if (idx >= lns.length) break;
                            System.out.println(lns[idx]);

                        }

                        System.out.flush();

                        System.out.println("...\n[Enter]");

                        try {
                            System.in.read();
                        } catch (Exception e) {
                        }

                        System.out.print("\033[1A"); // Move up
                        System.out.print("\033[2K"); // Erase line content

                    }
                }

                System.out.println("");
                System.out.print(prompt);
                System.out.flush();

            }

        }
    }

    /**
     * Scans input, adds results to input queue
     */
    private class Listener implements Runnable{

        @Override
        public void run() {

            Scanner in = new Scanner(System.in);
            String tmp = null;
            while(!exit){
                try {
                    //Blocking call - must be before mutex to prevent deadlock
                    tmp = in.nextLine();
                    imutex.acquire();
                    input.add(tmp);
                }catch (Exception e){}
                finally {
                    imutex.release();
                    show("");
                }
            }

        }
    }

    /**
     * @return First command from input queues, null if nothing available
     */
    public static String getCommand(){

        String res = null;

        try {

            if(readmutex.availablePermits() == 0) return null;
            else readmutex.acquire();

            res = null;
            try{
                imutex.acquire();
                res = input.poll();
            }catch (Exception e){}
            finally {
                imutex.release();
            }

        }catch(Exception e){ readmutex.release(); }

        readmutex.release();


        return res;

    }

    /**
     * Acquire readmutex before use
     * @return First command from input queues, null if nothing available
     */
    private static String getCmd(){

        String res = null;
        try{
            imutex.acquire();
            res = input.poll();
        }catch (Exception e){}
        finally {
            imutex.release();
        }

        return res;

    }

    /**
     * Changes prompt
     * @param str - new prompt
     */
    public void setPrompt(String str){

        prompt = str;

    }

    /**
     * Notify threads to end operations
     */
    public void close(){

        exit = true;

    }

    /**
     * Lets prompt user for input in uninterrupted way
     * Blocks printing out any other messages then function argument until user input is read
     * @param txt Text to display before reading user input
     * @return User input
     */
    public static String userInput(String txt){

        String inS;

        try {

            //Proprietary input reading
            readmutex.acquire();
            //No other text will be displayed
            mutex.acquire();

            Scanner in = new Scanner(System.in);
            //Ignore if anything was present
            in.nextLine();
            //Show prompt
            System.out.print(txt);
            //Get answer
            inS = in.nextLine();

        }catch (Exception e){
            return null;
        }finally {
            readmutex.release();
            mutex.release();
        }

        return inS;

    }

    /**
     * Lets prompt user for input in uninterrupted way
     * Blocks printing out any other messages then function argument until user input is read
     * @param obj Object on with function will be called
     * @param mtd Method invoked before reading input, should show prompt
     * @param params mtd call arguments
     * @return User input
     */
    public static String userInput(Object obj, Method mtd, Object params){

        String inS = null;

        try {

            //Proprietary input reading
            readmutex.acquire();
            //No other text will be displayed
            mutex.acquire();

            InputStreamReader in = new InputStreamReader(System.in);

            //Show prompt
            mtd.invoke(obj, params);
            //Get answer
            while(inS == null || inS.equals("")) {

                inS = getCmd();

            }

        }catch (Exception e){System.out.println(e.toString());
            return null;
        }finally {
            readmutex.release();
            mutex.release();
        }

        return inS;

    }

    @Test
    void manualOutputTest(){

        Presenter.show("1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12");
        /*try {
            System.in.read();
        }catch (Exception e){}*/
    }

}
