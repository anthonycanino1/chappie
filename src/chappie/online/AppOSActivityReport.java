package chappie.online;

public class AppOSActivityReport {


    public void setEpoch_start(int epoch_start) {
        this.epoch_start = epoch_start;
    }

    public void setEpoch_end(int epoch_end) {
        this.epoch_end = epoch_end;
    }

    public int getEpoch_start() {
        return epoch_start;
    }

    public int getEpoch_end() {
        return epoch_end;
    }

    private int epoch_start;
    private int epoch_end;

    public AppOSActivityReport() {

    }

    public AppOSActivityReport(int current_epoch, int number_of_epochs, double[][] reports) {
        this.epoch_activity = reports;
    }

    private double[][] epoch_activity;

    public double[][] getEpoch_activity() {
        return epoch_activity;
    }

    public void setEpoch_activity(double[][] epoch_activity) {
        this.epoch_activity = epoch_activity;
    }
}
