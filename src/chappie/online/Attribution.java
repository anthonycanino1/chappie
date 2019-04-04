package chappie.online;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chappie.config.ThreadIndices;
import chappie.config.TraceIndices;

import chappie.Chaperone;


/**
 *  API for client to call attribution functions.
 */
public class Attribution {


  public static final int OS_EPOCH_INDX = 0;
  public static final int OS_TID_INDX = 1;
  public static final int OS_CORE_INDX = 2;
  public static final int OS_USR_JIFF_INDX = 3;
  public static final int OS_KER_JIFF_INDX = 4;

  private Chaperone chappie;
  int start_epoch;
  int end_epoch;

  public void init_attribution(Chaperone chapp) {
    chappie =  chapp;
  }


  //Attribution attrib = new Attribution();


  public Map<Integer, List<ThreadEnergyAttribution>> get_all_thread_attrib(int start, int end) {
    start_epoch = start;
    end_epoch = end;
    double[][] os_report = get_os_activeness(start_epoch, end_epoch);
    /*for( double[] d : os_report){
      for(double b: d){
      System.out.println(b+"*Attribution");
      }
      }*/

    AppOSActivityReport os_activity_report = new AppOSActivityReport();
    os_activity_report.setEpoch_start(start_epoch);
    os_activity_report.setEpoch_end(end_epoch);
    os_activity_report.setEpoch_activity(os_report);
    Map<Integer, List<ThreadEnergyAttribution>> thread_report = get_thread_energy_reports(os_activity_report);
    return thread_report;
  }

  public int get_curret_epoch() {
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
        System.out.print("");
        if(!line.startsWith("cpu")) continue;
        line = line.replace("  "," ");
        String[] raw_core_reading = line.split(" ");
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
        selected_epoch=(selected_epoch%epoch_no_staggered);
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
      tr.tid= Long.parseLong(thread_reading.get(OS_TID_INDX).toString());
      tr.epoch = Integer.parseInt(thread_reading.get(OS_EPOCH_INDX).toString());
      tr.core_id = Integer.parseInt(thread_reading.get(OS_CORE_INDX).toString());
      tr.kernel_jiffies = Long.parseLong(thread_reading.get(OS_KER_JIFF_INDX).toString());
      tr.user_jiffies = Long.parseLong(thread_reading.get(OS_USR_JIFF_INDX).toString());

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
          selected_epoch = (selected_epoch%epoch_no_staggered);
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


          try {
            app_jiff_group_epoch_socket_staggered[selected_epoch][socket_index]+=tr.user_jiffies;
            app_jiff_group_epoch_socket_staggered[selected_epoch][socket_index]+=tr.kernel_jiffies;
          } catch(Throwable thr) {
            thr.printStackTrace();
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("epochs no:"+ epochs_no);
            System.out.println(selected_epoch);
            System.out.println(socket_index);
            System.out.println(app_jiff_group_epoch_socket_staggered.length);
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");

          }

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
        //System.out.println(os_state[i][j]+ ":" + epochs_no);
      }
    }

    /*os_state[0][0]=0;
      os_state[0][1]=0;*/

    return os_state;

  }

  //Rachit code starts


  static final int RAPL_WRAP_AROUND = 16384;
  static final int JRAPL_FACTOR = 2;
  int epoch_len = 10;
    /*try {
      epoch_len = Integer.parseInt(System.getenv("EPOCH_LENGTH"));
    } catch(Exception exc) {
			System.out.print();
    }*/


  public Map<Integer, List<ThreadEnergyAttribution>> attributeEnergy(List<List<Object>> raw_thread,
      List<List<Object>> raw_trace, double[][] jiffy_dfs) {

	try {
      epoch_len = Integer.parseInt(System.getenv("EPOCH_LENGTH"));
    } catch(Exception exc) {
      //System.out.print();
    }
    /*for( double[] d : jiffy_dfs){
      for(double b: d){
      System.out.println(b+"From attributeEnergy");
      }
      }*/

    Map<String, Double> threadMap = new HashMap<>();
    List<List<Object>> threadList = new ArrayList<>();
		List<List<Object>> newThreadList = new ArrayList<>();

    Map<Integer, List<ThreadEnergyAttribution>> energyMap = new HashMap<>();

    List<Double> frac = new ArrayList<>();

    //double packageEnergy[][] = new double[(int)(raw_trace.get(raw_trace.size() - 1).get(TraceIndices.EPOCH)) + 1][2];
    //double dramEnergy[][] = new double[(int)(raw_trace.get(raw_trace.size() - 1).get(TraceIndices.EPOCH)) + 1][2];
    //double activity[][] = new double[(int)(raw_thread.get(raw_thread.size() - 1).get(TraceIndices.EPOCH)) / 2 + 1][2];
    double packageEnergy[][] = new double[epoch_len][2];
    double dramEnergy[][] = new double[epoch_len][2];
    double activity[][] = new double[epoch_len/2][2];

    int init = -1;
    /*
     * Calculate minimum package and dram values contained in trace file for each
     * socket
     */
    //System.out.println("attributeEnergy() called");
    for (List<Object> trace_reading : raw_trace) {
      int epoch = (int)(trace_reading.get(TraceIndices.EPOCH));
      if(init == -1)
        init = epoch;
      epoch = epoch - init;
      int socket = Integer.parseInt(trace_reading.get(TraceIndices.SOCKET).toString());
      packageEnergy[epoch][socket - 1] = (double)(trace_reading.get(TraceIndices.PACKAGE));
      dramEnergy[epoch][socket - 1] = (double)(trace_reading.get(TraceIndices.DRAM));
      //System.out.println(trace_reading);
      //System.out.println(trace_reading.get(TraceIndices.PACKAGE));
      //System.out.println("dramEnergy["+ epoch+"]"+"["+(socket-1)+"]: "+ dramEnergy[epoch][socket-1]);
      //System.out.println(trace_reading);
    }
    init = -1;

    /*for(int i=0; i<packageEnergy.length; i++){
      for(int j=0; j<packageEnergy[0].length; j++){
      System.out.println("dramEnergy["+ i+"]"+"["+j+"]: "+ dramEnergy[i][j]);
      }
      }*/

    packageEnergy = subtractValues(packageEnergy);
    dramEnergy = subtractValues(dramEnergy);

    /*
     * Take a diff - Subtract value of column(x) from column(x+1)
     */
    packageEnergy = diff(packageEnergy);
    dramEnergy = diff(dramEnergy);

    /*
     * check package and dram energy and apply rapl_wrap_around value if found
     * negative
     */
    packageEnergy = checkJrapl(packageEnergy);
    dramEnergy = checkJrapl(dramEnergy);

    /*for(int i=0; i<packageEnergy.length; i++){
      for(int j=0; j<packageEnergy[0].length; j++){
      System.out.println("packageEnergy["+ i+"]"+"["+j+"]: "+ packageEnergy[i][j]);
      }
      }*/

    /*
     * Thread csv data - change format to ['epoch', 'time', 'thread', 'pid', 'tid',
     * 'core', 'u_jiffies', 'k_jiffies', 'state', 'socket', 'os_state', 'd_epoch']
     *
     * Add os state from jiffy_dfs
     */

    for (List<Object> list : raw_thread) {
      // List<String> list = new ArrayList<String>(list);
      int start_epoch = Integer.parseInt((raw_thread.get(0)).get(ThreadIndices.CORE).toString());
      int end_epoch = Integer.parseInt((raw_thread.get(0)).get(ThreadIndices.CORE).toString());
      int epochDiff = end_epoch-start_epoch+1;

      int epoch = (int)(list.get(ThreadIndices.EPOCH));
      if(init == -1 )
        init = epoch;

      int d_epoch = (epoch-init) / JRAPL_FACTOR;

      // check core and assign socket
      if (Integer.parseInt(((String)list.get(ThreadIndices.CORE))) > 19)
        list.add("2");
      else if (Integer.parseInt((String)list.get(ThreadIndices.CORE)) > -1)
        list.add("1");
      else
        list.add("-1");
      int sock = Integer.parseInt(list.get(ThreadIndices.SOCKET).toString());


      // if socket is -1 divide the energy among both sockets using state info
      if (sock == -1) {
        double state = Double.parseDouble(list.get(ThreadIndices.VM_STATE).toString());
        //System.out.println(state+"*********");
        int socket;

        // divide state value equally for both sockets
        state /= 2;

        // for unmappables1
        socket = 1;

        if (epoch % 10 == 0) {
          int epochIndex;
          if(epochDiff/10 == 0)
            epochIndex = 0;
          else
            epochIndex = (epochDiff/10) - 1;
          threadMap.put(String.valueOf(list.get(ThreadIndices.THREAD)).trim(), jiffy_dfs[epochIndex][0]);
        }

        // add os_state info
        if (epoch % 10 != 0) {
          Double val = 0.0;
          val = (threadMap.get(String.valueOf(list.get(ThreadIndices.THREAD)).trim()) == null) ? 0.0
            : threadMap.get(String.valueOf(list.get(ThreadIndices.THREAD)).trim());
          list.add(String.valueOf(val));
        } 
        else{
          int epochIndex;
          if(epochDiff/10 == 0)
            epochIndex = 0;
          else
            epochIndex = (epochDiff/10) - 1;

          list.add(String.valueOf(jiffy_dfs[epochIndex][0]));
        }

        // Update state and socket info
        list.set(ThreadIndices.VM_STATE, String.valueOf(state));
        list.set(ThreadIndices.SOCKET, String.valueOf(socket));
        list.add(String.valueOf(d_epoch));
        threadList.add(list);

        // activity and d_epoch
        activity[d_epoch][socket - 1] += state;

        // for unmappables2
        List<Object> tempList = new ArrayList<Object>(list);
        socket = 2;

        // update os_state info
        if (epoch % 10 == 0) {
          int epochIndex;
          if(epochDiff/10 == 0)
            epochIndex = 0;
          else
            epochIndex = (epochDiff/10) - 1;

          threadMap.put(String.valueOf(tempList.get(ThreadIndices.THREAD)).trim(), jiffy_dfs[epochIndex][1]);
        }

        if (epoch % 10 != 0) {
          Double val = 0.0;
          val = (threadMap.get(String.valueOf(tempList.get(ThreadIndices.THREAD)).trim()) == null) ? 0.0
            : threadMap.get(String.valueOf(tempList.get(ThreadIndices.THREAD)).trim());
          tempList.set(ThreadIndices.OS_STATE, String.valueOf(val));
        } 
        else {
          int epochIndex;
          if(epochDiff/10 == 0)
            epochIndex = 0;
          else
            epochIndex = (epochDiff/10) - 1;

          tempList.set(ThreadIndices.OS_STATE, String.valueOf(jiffy_dfs[epochIndex][1]));
        }
        // update socket info
        tempList.set(ThreadIndices.SOCKET, String.valueOf(socket));
        tempList.add(String.valueOf(d_epoch));
        threadList.add(tempList);

        // d_epoch and activity
        activity[d_epoch][socket - 1] += state;
      } // end-if for socket = -1
      else {
        // add os_state info for known socket
        int socket = sock;//Integer.parseInt(list.get(ThreadIndices.SOCKET).toString());
        if (epoch % 10 == 0) { 
          int epochIndex;
          if(epochDiff/10 == 0)
            epochIndex = 0;
          else
            epochIndex = (epochDiff/10) - 1;
          threadMap.put(String.valueOf(list.get(ThreadIndices.THREAD)).trim(), jiffy_dfs[epochIndex][socket - 1]);
        }

        if (epoch % 10 != 0) {
          Double val = 0.0;
          val = (threadMap.get(String.valueOf(list.get(ThreadIndices.THREAD)).trim()) == null) ? 0.0
            : threadMap.get(String.valueOf(list.get(ThreadIndices.THREAD)).trim());
          list.add(String.valueOf(val));
        } else {
          int epochIndex;
          if(epochDiff/10 == 0)
            epochIndex = 0;
          else
            epochIndex = (epochDiff/10) - 1;

          list.add(String.valueOf(jiffy_dfs[epochIndex][socket-1]));
        }
        list.add(String.valueOf(d_epoch));
        threadList.add(list);

        // d_epoch and activity
        activity[d_epoch][socket - 1] += Double.parseDouble(list.get(ThreadIndices.VM_STATE).toString());
      }

    }


    /*for(int i=0; i<activity.length; i++){
      for(int j=0; j<activity[0].length; j++){
    //System.out.println("activity[" +i+ "][" +j+ "] = " +activity[i][j]);
      }
      }*/

    //New Attrib Data

    /* 
		 * Implement #  thread = pd.merge(thread, trace, on = ['d_epoch', 'socket']).fillna(0)
		 * Thread csv data - change format to ['epoch', 'time', 'thread', 'pid', 'tid',
		 * 'core', 'u_jiffies', 'k_jiffies', 'state', 'socket', 'os_state', 'd_epoch', package, dram]
		 */
		double pkgEng=0.0;
		for(List<Object> thread : threadList) {
			List<Object> temp = new ArrayList<Object>();
			ThreadEnergyAttribution currentAttrib = new ThreadEnergyAttribution();
			
      if(thread.get(ThreadIndices.TID) == null)
        continue;
			//Merge data for epoch (which is d_epoch*2)
			temp.addAll(thread);
			int epoch = Integer.parseInt(thread.get(ThreadIndices.EPOCH).toString());
			int tid = Integer.parseInt(thread.get(ThreadIndices.TID).toString());
			int d_epoch = Integer.parseInt(thread.get(ThreadIndices.D_EPOCH).toString());
			int socket = Integer.parseInt(thread.get(ThreadIndices.SOCKET).toString());
			int index = (d_epoch*2);
			double vm_state = Double.parseDouble(thread.get(ThreadIndices.VM_STATE).toString());
			vm_state = vm_state / (activity[d_epoch][socket - 1]);
			double pkg = packageEnergy[index][socket-1];
			double dram  = dramEnergy[index][socket-1];
			double t_pkg = 0.0;
			double t_dram = 0.0;
			t_pkg = pkg
					* Double.parseDouble((String)thread.get(ThreadIndices.OS_STATE))
					* vm_state;
			t_dram = dram
					* Double.parseDouble((String)thread.get(ThreadIndices.OS_STATE))
					* vm_state;
			//pkgEng+=t_dram;
			temp.add(t_pkg);
			temp.add(t_dram);
			newThreadList.add(temp);
			currentAttrib.setEpoch_no(epoch);
			currentAttrib.setCore_no(Integer.parseInt((String)thread.get(ThreadIndices.CORE)));
			currentAttrib.setDram_energy(t_dram);
			currentAttrib.setPkg_energy(t_pkg);
			currentAttrib.setTid(tid);

			if (energyMap.get(tid) == null) {
				List<ThreadEnergyAttribution> currentList = new ArrayList<>();
				currentList.add(currentAttrib);
				energyMap.put(tid, currentList);
			} else {
				List<ThreadEnergyAttribution> currentList = energyMap.get(tid);
				currentList.add(currentAttrib);
				energyMap.put(tid, currentList);
			}
			
			// Merge data for epoch+1
			temp = new ArrayList<Object>();
			currentAttrib = new ThreadEnergyAttribution();
			pkg = packageEnergy[index +1][socket-1];
			dram  = dramEnergy[index +1][socket-1];
			t_pkg = 0.0;
			t_dram = 0.0;
			temp.addAll(thread);
			
			t_pkg = pkg
					* Double.parseDouble((String)thread.get(ThreadIndices.OS_STATE))
					* vm_state;
			t_dram = dram
					* Double.parseDouble((String)thread.get(ThreadIndices.OS_STATE))
					* vm_state;
			
			temp.add(t_pkg);
			temp.add(t_dram);
			//pkgEng+=t_dram;
			newThreadList.add(temp);
			currentAttrib.setEpoch_no(epoch);
			currentAttrib.setCore_no(Integer.parseInt((String)thread.get(ThreadIndices.CORE)));
			currentAttrib.setDram_energy(t_dram);
			currentAttrib.setPkg_energy(t_pkg);
			currentAttrib.setTid(tid);
			if (energyMap.get(tid) == null) {
				List<ThreadEnergyAttribution> currentList = new ArrayList<>();
				currentList.add(currentAttrib);
				energyMap.put(tid, currentList);
			} else {
				List<ThreadEnergyAttribution> currentList = energyMap.get(tid);
				currentList.add(currentAttrib);
				energyMap.put(tid, currentList);
			}
			
		}
		

    
    
    
    /*
     * Thread csv data - change format to ['epoch', 'time', 'thread', 'pid', 'tid',
     * 'core', 'u_jiffies', 'k_jiffies', 'state', 'socket', 'os_state', 'd_epoch']
     */

    /*double total = 0.0;
    for (List<Object> thread : threadList) {
      ThreadEnergyAttribution currentAttrib = new ThreadEnergyAttribution();
      if(thread.get(ThreadIndices.TID) == null)
        continue;
      int tid = (int)(thread.get(ThreadIndices.TID));
      int d_epoch = Integer.parseInt(thread.get(ThreadIndices.D_EPOCH).toString());
      int socket = Integer.parseInt((thread.get(ThreadIndices.SOCKET).toString()));
      double vm_state = Double.parseDouble(thread.get(ThreadIndices.VM_STATE).toString());
      vm_state = vm_state / (activity[d_epoch][socket - 1]);
      thread.add(String.valueOf(vm_state));
      int trace_index = (d_epoch * 2) + 1;
      double t_pkg = 0.0;
      double t_dram = 0.0;
      //System.out.println("d_epoch:"+ d_epoch);
      //System.out.println("Trace_index: "+ trace_index);
      //System.out.println("Socket: "+socket);

      //System.out.println("dramEnergy["+trace_index+"]["+(socket - 1)+"] = "+ dramEnergy[trace_index][socket - 1]);
      //System.out.println("packageEnergy["+trace_index+"]["+(socket - 1)+"] = "+ packageEnergy[trace_index][socket - 1]);
      //System.out.println((String)thread.get(ThreadIndices.OS_STATE));
      //System.out.println(thread.get(ThreadIndices.VM_STATE).toString());

      t_pkg = packageEnergy[trace_index][socket - 1]
        * Double.parseDouble((String)thread.get(ThreadIndices.OS_STATE))
        * Double.parseDouble(thread.get(ThreadIndices.VM_STATE).toString());
      t_dram = dramEnergy[trace_index][socket - 1]
        * Double.parseDouble((String)thread.get(ThreadIndices.OS_STATE))
        * Double.parseDouble(thread.get(ThreadIndices.VM_STATE).toString());

      total += t_dram;

      //System.out.println("============================"+t_pkg);
      //System.out.println("****************************"+total);
      currentAttrib.setEpoch_no(d_epoch);
      currentAttrib.setCore_no(Integer.parseInt((String)thread.get(ThreadIndices.CORE)));
      currentAttrib.setDram_energy(t_dram);
      currentAttrib.setPkg_energy(t_pkg);
      currentAttrib.setTid(tid);

      if (energyMap.get(tid) == null) {
        List<ThreadEnergyAttribution> currentList = new ArrayList<>();
        currentList.add(currentAttrib);
        energyMap.put(tid, currentList);
      } else {
        List<ThreadEnergyAttribution> currentList = energyMap.get(tid);
        currentList.add(currentAttrib);
        energyMap.put(tid, currentList);
      }
    }

    System.out.println("Total dram: "+total);
    */

    // Add OS_State mean
    double tempFrac = 0.0;
    for (List<Object> list : threadList) {
      tempFrac += Double.parseDouble(list.get(ThreadIndices.OS_STATE).toString());
    }
    tempFrac /= threadList.size();
    frac.add(tempFrac);
    return energyMap;
  }

  private double[][] subtractValues(double[][] energy) {
    double min1 = Double.MAX_VALUE;
    double min2 = Double.MAX_VALUE;
    for (int i = 0; i < energy.length; i++) {
      if (energy[i][0] < min1)
        min1 = energy[i][0];
      if (energy[i][1] < min2)
        min2 = energy[i][1];
    }

    for (int i = 0; i < energy.length; i++) {
      energy[i][0] -= min1;
      energy[i][1] -= min2;
    }
    return energy;
  }

  private double[][] diff(double[][] energy) {
    double temp[][] = new double[energy.length][2];
    for (int i = 1; i < energy.length; i++) {
      if (i == 1) {
        temp[0][0] = 0;
        temp[0][1] = 0;
      }
      temp[i][0] = energy[i][0] - energy[i - 1][0];
      temp[i][1] = energy[i][1] - energy[i - 1][1];
    }
    return temp;
  }

  private double[][] checkJrapl(double[][] energy) {
    for (int i = 0; i < energy.length; i++) {
      double newval1 = energy[i][0];
      double newval2 = energy[i][1];

      if (newval1 < 0)
        energy[i][0] = Math.max(newval1 + RAPL_WRAP_AROUND, 0);
      if (newval2 < 0)
        energy[i][1] = Math.max(newval2 + RAPL_WRAP_AROUND, 0);

    }
    return energy;
  }



  //Rachit code ends

  public Map<Integer, List<ThreadEnergyAttribution>> get_thread_energy_reports(AppOSActivityReport os_report) {
    Map<Integer, List<ThreadEnergyAttribution>> energy_reports = null;
    double[][] os_state = os_report.getEpoch_activity();
    //int start_epoch = this.start_epoch;//os_report.getEpoch_start();
    //int end_epoch = this.end_epoch;//os_report.getEpoch_end();
    List<List<Object>> threads = chappie.get_thread_info(start_epoch, end_epoch);
    List<List<Object>> trace = chappie.get_energy_info(start_epoch, end_epoch);
    //System.out.println("START EPOCH: "+ start_epoch+ "END EPOCH: "+ end_epoch);
    //for (List<Object> trace_reading : trace) {
    //System.out.println(trace_reading);
    //}

    //Place call to Rachit Code here ....
    energy_reports = attributeEnergy(threads, trace, os_state);

    return energy_reports;
  }

}
