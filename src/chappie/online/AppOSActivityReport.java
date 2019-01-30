package chappie.online;

public class AppOSActivityReport {

    public AppOSActivityReport() {

    }

    public AppOSActivityReport(int current_epoch, int number_of_epochs, double[][] reports) {
        this.current_epoch = current_epoch;
        this.number_of_epochs = number_of_epochs;
        this.epoch_activity = reports;
    }

    private int current_epoch;
    private int number_of_epochs;
    private double[][] epoch_activity;

    public int getCurrent_epoch() {


        return current_epoch;
    }

    public void setCurrent_epoch(int current_epoch) {
        this.current_epoch = current_epoch;
    }

    public int getNumber_of_epochs() {
        return number_of_epochs;
    }

    public void setNumber_of_epochs(int number_of_epochs) {
        this.number_of_epochs = number_of_epochs;
    }

    public double[][] getEpoch_activity() {
        return epoch_activity;
    }

    public void setEpoch_activity(double[][] epoch_activity) {
        this.epoch_activity = epoch_activity;
    }
}
