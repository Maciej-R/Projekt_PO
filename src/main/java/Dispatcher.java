public class Dispatcher {

    private static Presenter presenter = Presenter.get();
    private static Dispatcher dispatcher = new Dispatcher();

    private Dispatcher(){}

    public static Dispatcher get(){ return dispatcher; }

    public void go(){

        (new Thread(new Job())).run();

    }

    private class Job implements Runnable{

        @Override
        public void run() {

            String cmdS;

            while(true){

                if((cmdS = presenter.getCommand()) == null) {

                    Thread.yield();
                    continue;

                }
                if(cmdS.equals("q")) {

                    presenter.close();
                    break;

                }

                Command cmd = Parser.parse(cmdS);

                if(cmd == null) continue;

                switch (cmd.command){

                    case("hosts"):
                    {
                        (new Thread(new Hosts(cmd.parameters))).start();
                        break;
                    }
                    case("ports"):
                    {
                        (new Thread(new Ports())).start();
                        break;
                    }
                    case("pscan"):
                    {
                        (new Thread(new Pscan(cmd.parameters))).start();
                        break;
                    }
                    case("help"):
                    {
                        (new Thread(new Help())).start();
                        break;
                    }
                    case("hopcnt"):
                    {
                        (new Thread(new HopCount(cmd.parameters))).start();
                        break;
                    }
                    case("traceroute"):
                    {
                        (new Thread(new Traceroute(cmd.parameters))).start();
                        break;
                    }
                    default:
                    {
                     Presenter.show("Try \"help\" command");
                    }

                }

            }

            System.out.println("Bye");

        }

    }

}
