import java.io.BufferedReader;
import java.io.FileReader;

public class Help implements Runnable {

    @Override
    public void run() {

        try (BufferedReader br = new BufferedReader(new FileReader("help.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }catch(Exception e){

            Presenter.show("Cannot read help file");

        }

    }
}
