import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParallelTaskExecutioner implements Runnable {

    ParallelTask task;

    ParallelTaskExecutioner(ParallelTask t){
        task = t;
    }

    @Override
    public void run(){

        try{

            //Prepare for parallelism
            List<Thread> thrds = new LinkedList<>();

            //Number of hosts to check per thread
            int rng_per_thrd = (int)Math.ceil((task.rangeEnd - task.rangeStart) / (double)task.n);
            //Where thread will be saving theirs results
            ArrayList<StringBuilder> res = new ArrayList<>(task.n);

            //Start jobs
            if(task.n == 1){

                res.add(0, new StringBuilder());
                if (task.args == null)
                    thrds.add(new Thread(task.impl.newInstance(task.rangeStart, task.rangeEnd, res.get(0))));
                else
                    thrds.add(new Thread((Runnable) task.impl.newInstance(task.rangeStart, task.rangeEnd, res.get(0), task.args)));
                thrds.get(0).start();

            }
            else

                for (int i = 0; i < task.n; ++i) {

                    res.add(i, new StringBuilder());
                    if (task.args == null)
                        thrds.add(new Thread(task.impl.newInstance(i * rng_per_thrd + task.rangeStart,
                                Math.min((i + 1) * rng_per_thrd + task.rangeStart, task.rangeEnd), res.get(i))));
                    else
                        thrds.add(new Thread((Runnable) task.impl.newInstance(i * rng_per_thrd + task.rangeStart,
                                Math.min((i + 1) * rng_per_thrd + task.rangeStart, task.rangeEnd), res.get(i), task.args)));

                    thrds.get(i).start();

                }

            //Wait for all
            for(Thread t : thrds) t.join();

            //Concat all results
            for(StringBuilder sb : res){

                task.builder.append(sb.toString());

            }

        }
        catch(Exception e){System.out.println(e.toString());}

    }

}
