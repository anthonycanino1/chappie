package chappie.online;

import chappie.Chaperone;

import java.util.TimerTask;

public class OnlineTester implements Runnable {

    private Chaperone chappie;
    private int frequency;

    @Override
    public void run() {
        while(true) {
            int start = chappie.get_current_epoch();
            try {
                Thread.sleep(frequency);
            } catch(Exception exc) {
                exc.printStackTrace();
            }
            int end = chappie.get_current_epoch();
            Attribution.get_all_thread_attrib(start, end-1);
        }
    }

    public Chaperone getChappie() {
        return chappie;
    }

    public void setChappie(Chaperone chappie) {
        this.chappie = chappie;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
