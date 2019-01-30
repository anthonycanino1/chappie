package chappie.online;

public class ThreadEnergyAttribution {

    private int epoch_no;
    private int core_no;
    private double dram_energy;
    private double pkg_energy;
    private int tid;

    public int getEpoch_no() {
        return epoch_no;
    }

    public void setEpoch_no(int epoch_no) {
        this.epoch_no = epoch_no;
    }

    public int getCore_no() {
        return core_no;
    }

    public void setCore_no(int core_no) {
        this.core_no = core_no;
    }

    public double getDram_energy() {
        return dram_energy;
    }

    public void setDram_energy(double dram_energy) {
        this.dram_energy = dram_energy;
    }

    public double getPkg_energy() {
        return pkg_energy;
    }

    public void setPkg_energy(double pkg_energy) {
        this.pkg_energy = pkg_energy;
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }
}
