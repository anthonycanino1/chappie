package chappie.util;


import jrapl.EnergyCheckUtils.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import java.io.*;

public class StatsUtil {

	public static final int NAIVE=0;
	public static final int OS_NAIVE=1;
	public static final int OS_SAMPLE=2;
	public static final int VM_SAMPLE=3;

	public static final int METHOD_START = 1;
	public static final int METHOD_END = 2; 

	public static final int MAX_THREADS = 2048*1024;
	public static List[] method_stats = new ArrayList[MAX_THREADS];

	public static List<List<Double>> read_energy() {
		double[] jrapl_reading = jrapl.EnergyCheckUtils.getEnergyStats();
		List<List<Double>> energy_reading = new ArrayList<List<Double>>();

		for (int i = 0; i < jrapl_reading.length / 3; ++i) {
			List<Double> measure = new ArrayList<Double>();
			measure.add(jrapl_reading[3 * i + 2]);
		   	measure.add(jrapl_reading[3 * i]);	   
			energy_reading.add(measure);
		}
		return energy_reading;
	}


	public static List get_all_thread_jiffies() {
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		
		//For performacne, one array list will contain both names and jiffies
		//First entry is the thread name, next entry is a long[] that represents the jiffies readings and so on
		List os_stats = new ArrayList(threadSet.size() * 2);
		
		for(Thread thr : threadSet) {
			String name = thr.getName();
			int[] os_stat =  GLIBC.getOSStats(name);
			os_stats.add(name);
			os_stats.add(os_stat);
		}

		return os_stats;
	}


	public static int get_energy_epoch() {
		return -1;
	}

	//This will be called at the end of Chappie by a possibly VM Shutdown Hook
	public static void print_method_stats() throws IOException {
		StringBuilder stats_str = new StringBuilder();
		for(int thread_indx = 0; thread_indx < MAX_THREADS; thread_indx++) {
			if(method_stats[thread_indx]==null) continue;
			List samples = method_stats[thread_indx];
			for(Object obj : samples) {
				MethodStatsSample sample = (MethodStatsSample) obj;
				stats_str.append(sample.thread_name).append(",");
				stats_str.append(sample.method_name).append(",");
				stats_str.append(sample.event==METHOD_START?"START":"END");
				stats_str.append(sample.timestamp).append(",");
				stats_str.append(sample.epoch).append(",");
				
				if(sample.energy!=null) {
					for(List<Double> ener : sample.energy) {
						for(Double val : ener) {
							stats_str.append(val.doubleValue()).append(",");
						}
					}
				}

				if(sample.jiffies!=null) {
					for (Object jiff_entry : sample.jiffies) {
						stats_str.append(jiff_entry).append(",");
					}
				}

				stats_str.append("end \n");
			}
		}

		 PrintWriter methods = new PrintWriter(new BufferedWriter(new FileWriter("method_stats.csv")));
		 methods.print(stats_str.toString());
		 methods.close();

	}

	public static void notify_before(String method_name, int profile_mode) {
		notify_method(method_name, profile_mode, METHOD_START);
	}

	public static void notify_after(String method_name, int profile_mode) {
		notify_method(method_name, profile_mode, METHOD_END);
	}

	public static void notify_method(String method_name, int profile_mode, int event) {
		MethodStatsSample sample = new MethodStatsSample();
		sample.timestamp = System.currentTimeMillis();
		sample.method_name = method_name;
		sample.event = event;

		if(profile_mode == NAIVE || profile_mode == OS_NAIVE) {
			sample.energy = read_energy(); 
		}

		if(profile_mode == OS_NAIVE) {
			sample.jiffies = get_all_thread_jiffies();
		}

		if(profile_mode == OS_SAMPLE || profile_mode==VM_SAMPLE) {
			sample.epoch = get_energy_epoch();	
		}


	
		Thread cur_thread = Thread.currentThread();
		int my_id = (int) cur_thread.getId();
		String my_name = cur_thread.getName();
		List this_thread_stats = method_stats[my_id];
		if(this_thread_stats == null) {
			method_stats[my_id] = this_thread_stats = new ArrayList();
		}

		sample.thread_name = my_name;
		this_thread_stats.add(sample);
	}
}

class MethodStatsSample {
	public int epoch;
	public long timestamp;
	public List<List<Double>> energy;
	public String method_name;
	public List jiffies;
	//Either METHOD_START or METHOD_END
	public int event;
	public String thread_name;
}


