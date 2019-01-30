package chappie.online;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


import chappie.Chaperone;


/**
 *  API for client to call attribution functions.
 */
public class Attribution {


    private static Chaperone chappie;

    private static void init_attribution(Chaperone chapp) {
        chappie =  chapp;
    }


    /**
     *
     * @param epochs Number of epochs to include in activity report backward from the current report
     *
     */
    public static AppOSActivityReport get_os_activeness(int epochs) {
        AppOSActivityReport osReport = new AppOSActivityReport();
        //Data will be fetched starting from current_epoch -1 to avoid synchronization
        int current_epoch = chappie.get_current_epoch();
        List<String> sys_jiffies = chappie.get_sys_jiffies(current_epoch,epochs);
        List<List<Object>> app_jiffies = chappie.application_jiffies(current_epoch,epochs);

        /**
         * Logic will be performed here is the same as the first part of data_processing.py.
         * This is already implemented in chappie_atrribution repository
         * 
         */



        return osReport;
    }

    public static Map<Integer, List<ThreadEnergyAttribution>> get_thread_energy_reports(AppOSActivityReport os_report) {
        HashMap<Integer, List<ThreadEnergyAttribution>> energy_reports = new HashMap<>();

        /**
         * Logic will be performed here is the same as the second part of data_processing.py.
         * This will be implemented as part of the chappie_attribtuon project
         */
        return energy_reports;
    }





}