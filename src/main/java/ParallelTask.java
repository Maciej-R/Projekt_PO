import java.lang.reflect.Constructor;
import java.util.LinkedList;

public class ParallelTask {

    public int n;
    public int rangeStart;
    public int rangeEnd;
    public StringBuilder builder;
    Constructor<? extends Runnable> impl;
    public LinkedList<Object> args = null;

    /**
     * Used when no arguments for constructor of class implementing task are required
     * Using <rangeStart, rangeEnd)
     * @param n Threads num
     * @param rangeStart
     * @param rangeEnd
     * @param impl Constructor of class used to run parallel threads
     */
    public ParallelTask(int n, int rangeStart, int rangeEnd, StringBuilder builder, Constructor impl) {
        this.n = n;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.builder = builder;
        this.impl = impl;
    }

    /**
     * Task with arguments in constructor
     * Using <rangeStart, rangeEnd)
     * @param n Threads num
     * @param rangeStart
     * @param rangeEnd
     * @param builder Result string

     * @param args Raw type list of any objects used by impl class which must parse types on its own
     */
    public ParallelTask(int n, int rangeStart, int rangeEnd, StringBuilder builder, Constructor impl, LinkedList args) {
        this.n = n;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.builder = builder;
        this.impl = impl;
        this.args = args;
    }
}
