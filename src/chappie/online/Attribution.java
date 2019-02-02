package chappie.online;


import java.util.ArrayList;
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


    public static int get_curret_epoch() {
        //The previous epoch is always gauranteed to be in the list
        return chappie.get_current_epoch()-1;
    }

    /**
     * @param start Start epoch to fetch
     * @param end Last epoch to fetch inclusive
     */
    public double[][] get_os_activeness(int start, int end) {
        AppOSActivityReport osReport = new AppOSActivityReport();
        List<String> raw_sys_jiffies = chappie.get_sys_jiffies(start,end);
        List<List<Object>> raw_app_jiffies = chappie.application_jiffies(start,end);

        //First Step :: Calulate minimum jiffies per core across the entire /proc/stat file
        long[][] core_mins = new long[Chaperone.VIRTUAL_CORES][Chaperone.JIFF_LEN];

        for(int core_index=0;core_index<Chaperone.VIRTUAL_CORES;core_index++) {
            for(int jiff_index=0; jiff_index<Chaperone.JIFF_LEN;jiff_index++) {
                core_mins[core_index][jiff_index] = Long.MAX_VALUE;
            }
        }

        int epochs_no = end-start+1;
        int epoch_no_staggered = (int)Math.ceil(epochs_no/(double)Chaperone.EPOCH_RATE);
        long[][][] sys_jiffies = new long[epochs_no][Chaperone.VIRTUAL_CORES][Chaperone.JIFF_LEN];
        long[][]   jiff_group_epoch_socket = new long[epochs_no][Chaperone.NUMER_OF_SOCKETS];
        long[][]   jiff_group_epoch_socket_staggered = new long[epoch_no_staggered][Chaperone.NUMER_OF_SOCKETS];
        long[][]   app_jiff_group_epoch_socket =  new long[sys_jiffies.length][Chaperone.NUMER_OF_SOCKETS];
        long[][]   app_jiff_group_epoch_socket_staggered =  new long[epoch_no_staggered][Chaperone.NUMER_OF_SOCKETS];
        double os_state[][] = new double[epoch_no_staggered][Chaperone.NUMER_OF_SOCKETS];

        //Parsing /proc/stat entries and substracting min(core) from each core entry
        int epoch=0;
        for(String raw_reading : raw_sys_jiffies) {
            String[] lines = raw_reading.split(System.getProperty("line.separator"));
            int core_index=0;
            for(String line : lines) {
                if(!raw_reading.startsWith("cpu")) continue;
                raw_reading = raw_reading.replace("  "," ");
                String[] raw_core_reading = raw_reading.split(" ");
                if(raw_core_reading[0].equalsIgnoreCase("cpu")) continue;
                    for (int jiff_index = 1; jiff_index < raw_core_reading.length; jiff_index++) {
                        long jiff_reading = Long.parseLong(raw_core_reading[jiff_index]);
                        sys_jiffies[epoch][core_index][jiff_index - 1] = jiff_reading;
                        if (jiff_reading < core_mins[core_index][jiff_index - 1])
                            core_mins[core_index][jiff_index - 1] = jiff_reading;
                    }

                    core_index++;
            }
            epoch++;
        }

        epoch=0;


        //Performing Grouping Per Socket and Epoch
        long previous_socket_0_total=0;
        long previous_socket_1_total=0;
        for(long[][] reading : sys_jiffies) {
            int core_index=0;
            for(long[] core_reading : reading) {
                int jiff_index=0;
                for(long jiff : core_reading) {
                    core_reading[jiff_index]-=core_mins[core_index][jiff_index];
                    if(jiff_index!=3) jiff_group_epoch_socket[epoch][core_index/Chaperone.CORE_PER_SOCKETS]+=core_reading[jiff_index];
                    jiff_index++;
                }
                core_index++;
            }


            if(epoch%10==0) {
                int selected_epoch=epoch/10;
                jiff_group_epoch_socket_staggered[selected_epoch][0]=jiff_group_epoch_socket[epoch][0];
                jiff_group_epoch_socket_staggered[selected_epoch][1]=jiff_group_epoch_socket[epoch][1];


                if(selected_epoch==0) {
                    previous_socket_0_total=jiff_group_epoch_socket_staggered[selected_epoch][0];
                    previous_socket_1_total=jiff_group_epoch_socket_staggered[selected_epoch][1];
                    jiff_group_epoch_socket_staggered[selected_epoch][0]=0;
                    jiff_group_epoch_socket_staggered[selected_epoch][1]=0;
                } else {
                    long current_socket_0_total=jiff_group_epoch_socket_staggered[selected_epoch][0];
                    long current_socket_1_total=jiff_group_epoch_socket_staggered[selected_epoch][1];
                    jiff_group_epoch_socket_staggered[selected_epoch][0]-=previous_socket_0_total;
                    jiff_group_epoch_socket_staggered[selected_epoch][1]-=previous_socket_1_total;
                    previous_socket_0_total=current_socket_0_total;
                    previous_socket_1_total=current_socket_1_total;

                }
            }
            epoch++;
        }

        //Parse and process application jiffies files
        //epoch,tid,core,u_jiffies,k_jiffies
        Map<Long, List<ThreadReading>> thread_readings = new HashMap<Long, List<ThreadReading>>();
        Map<Long, long[]> thread_mins = new HashMap<Long, long[]> ();

        //Parse Thread Jiffies and find minimums [epoch,tid,core,u_jiffies,k_jiffies]

        for(List<Object> thread_reading : raw_app_jiffies) {
            ThreadReading tr = new ThreadReading();
            tr.tid= Long.parseLong(thread_reading.get(1).toString());
            tr.epoch = Integer.parseInt(thread_reading.get(0).toString());
            tr.core_id = Integer.parseInt(thread_reading.get(2).toString());
            tr.kernel_jiffies = Long.parseLong(thread_reading.get(4).toString());
            tr.user_jiffies = Long.parseLong(thread_reading.get(3).toString());

            if(!thread_readings.containsKey(tr.tid)) {
                thread_readings.put(tr.tid, new ArrayList<ThreadReading>());
                long[] mins = new long[2];
                mins[0] = Long.MAX_VALUE;
                mins[1] = Long.MAX_VALUE;
                thread_mins.put(tr.tid, mins);
            }

            thread_readings.get(tr.tid).add(tr);
            long[] mins = thread_mins.get(tr.tid);
            if(tr.user_jiffies < mins[0]) mins[0] = tr.user_jiffies;
            if(tr.kernel_jiffies < mins[1]) mins[1] = tr.kernel_jiffies;
        }

        long previous_u=0;
        long previous_k=0;

        for(List<ThreadReading> trlist : thread_readings.values()) {
            boolean first_encountered=false;
            for(ThreadReading tr : trlist) {
                tr.user_jiffies-=thread_mins.get(tr.tid)[0];
                tr.kernel_jiffies-=thread_mins.get(tr.tid)[1];

                if(tr.epoch%10==0) {
                    int selected_epoch = tr.epoch/10;
                    int socket_index = tr.core_id/Chaperone.CORE_PER_SOCKETS;

                    if(!first_encountered) {
                        previous_u=tr.user_jiffies;
                        previous_k=tr.kernel_jiffies;
                        tr.user_jiffies=0;
                        tr.kernel_jiffies=0;
                        first_encountered=true;
                    } else {
                        long current_u=tr.user_jiffies;
                        long current_k=tr.kernel_jiffies;
                        tr.user_jiffies-=previous_u;
                        tr.kernel_jiffies-=previous_k;
                        previous_k=current_k;
                        previous_u=current_u;
                    }


                    app_jiff_group_epoch_socket_staggered[selected_epoch][socket_index]+=tr.user_jiffies;
                    app_jiff_group_epoch_socket_staggered[selected_epoch][socket_index]+=tr.kernel_jiffies;

                }
            }
        }

        //Calculate os_state
        for(int i=0; i < epoch_no_staggered;i++) {
            for(int j=0; j< Chaperone.NUMER_OF_SOCKETS; j++) {
                if(jiff_group_epoch_socket_staggered[i][j]!=0) {
                    os_state[i][j] = (double) app_jiff_group_epoch_socket_staggered[i][j] / (double) jiff_group_epoch_socket_staggered[i][j];
                    if (os_state[i][j] > 1.0) {
                        os_state[i][j] = 1;
                    }
                } else {
                    os_state[i][j]=1;
                }
            }
        }

        os_state[0][0]=0;
        os_state[0][1]=0;

        return os_state;

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