package chappie.online;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *  API for client to call attribution functions.
 */
public class Attribution {


    /**
     *
     * @param epochs Number of epochs to include in activity report backward from the current report
     *
     */
    public static AppOSActivityReport get_os_activeness(int epochs) {
        AppOSActivityReport osReport = new AppOSActivityReport();
        //Same logic will copied fromthe offline attribution version ...
        return osReport;
    }

    public static Map<Integer, List<ThreadEnergyAttribution>> get_thread_energy_reports(AppOSActivityReport os_report) {
        HashMap<Integer, List<ThreadEnergyAttribution>> energy_reports = new HashMap<>();
        return energy_reports;
    }

}