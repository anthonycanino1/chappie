package chappie.util;

import chappie.Chaperone;

import jrapl.EnergyCheckUtils.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import java.io.*;
import java.util.Random;

public class StatsUtil {

	public static final int NAIVE=0;
	public static final int OS_NAIVE=1;
	public static final int OS_SAMPLE=2;
	public static final int VM_SAMPLE=3;

	public static final int METHOD_START = 1;
	public static final int METHOD_END = 2;

	public static final int MAX_THREADS = 1024;
	public static final int MAX_LOG_BUFFER = 1024*1024;

	public static List[] method_stats = new ArrayList[MAX_THREADS];
	//public static PrintWriter[] thread_writers = new PrintWriter[MAX_THREADS];
	public static int[] thread_index = new int[MAX_THREADS];
	public static MethodStatsSample[][] thread_samples = new MethodStatsSample[MAX_THREADS][MAX_LOG_BUFFER];



	public static void close_open_files() {
		//To be called by Chaperon retire() method
	}

	public static int INITIAL_STATS_ALLOC = 1024*1024*64;

	public static double[] read_energy() {
		double[] jrapl_reading = jrapl.EnergyCheckUtils.getEnergyStats();
		return jrapl_reading;
		/*List<List<Double>> energy_reading = new ArrayList<List<Double>>();

		for (int i = 0; i < jrapl_reading.length / 3; ++i) {
			List<Double> measure = new ArrayList<Double>();
			measure.add(jrapl_reading[3 * i + 2]);
		   	measure.add(jrapl_reading[3 * i]);
			energy_reading.add(measure);
		}
		return energy_reading;*/
	}


	public static List get_all_thread_jiffies() {
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

		//For performacne, one array list will contain both names and jiffies
		//First entry is the thread name, next entry is a long[] that represents the jiffies readings and so on
		List os_stats = new ArrayList(threadSet.size() * 2);
		for(Thread thr : threadSet) {
			String name = thr.getName();
			String[] os_stat =  GLIBC.getOSStats(name);
			os_stats.add(name);
			os_stats.add(os_stat);
		}

		return os_stats;
	}


	public static int get_energy_epoch() {
		return Chaperone.epoch;
	}

	//This will be called at the end of Chappie by a possibly VM Shutdown Hook
	public static void print_method_stats() throws Exception  {
		//System.out.println("Printing Method Stats");
		StringBuilder stats_str = new StringBuilder();
		PrintWriter mywriter = new PrintWriter(new BufferedWriter(new FileWriter("method_stats.csv")));
		for(int my_id = 0; my_id < MAX_THREADS; my_id++) {
		//System.out.println("Number of Stats for thread" + my_id + " is " + thread_index[my_id]);
		if(thread_index[my_id]==0) continue;
		for(int sample_index=0; sample_index < thread_index[my_id]; sample_index++) {
				MethodStatsSample sample = thread_samples[my_id][sample_index];
				stats_str.append(sample.thread_name).append(",");
				stats_str.append(sample.method_name).append(",");
				stats_str.append(sample.event==METHOD_START?"START":"END").append(",");
				stats_str.append(sample.timestamp).append(",");
				stats_str.append(sample.epoch).append(",");

				if(sample.energy!=null) {
					for(double val : sample.energy) {
						stats_str.append(val).append(",");
					}
				} else {
					stats_str.append("no_energy").append(",");
				}

				if(sample.jiffies!=null) {
					int indx=1;
					stats_str.append(sample.jiffies.size()/2).append(",");
					for (Object os_entry : sample.jiffies) {
						if (indx%2==1) {
							stats_str.append(os_entry).append(",");
						} else {
							int[] os_vals = (int[]) os_entry;
							stats_str.append(os_vals[0]).append(",");
							stats_str.append(os_vals[1]).append(",");
							stats_str.append(os_vals[2]).append(",");
						}
						indx++;
					}

				} else {
					stats_str.append("0,");
				}

				stats_str.append("end \n");
		}
		}

		 try {
			 mywriter.print(stats_str.toString());
		 	 mywriter.flush();
			 mywriter.close();
		 } catch (Exception exception) {
			exception.printStackTrace();
		 }
	}

	public static void notify_before(String method_name, int profile_mode) {
		notify_method(method_name, profile_mode, METHOD_START);
	}

	public static void notify_after(String method_name, int profile_mode) {
		notify_method(method_name, profile_mode, METHOD_END);
	}


	public static int cnt = 0;

	public static void notify_method(String method_name, int profile_mode, int event) {
		Thread cur_thread = Thread.currentThread();
		int my_id = (int) cur_thread.getId();
		String my_name = cur_thread.getName();
		MethodStatsSample sample = thread_samples[my_id][0];

		if(sample==null) {
			for(int sample_indx = 0; sample_indx < MAX_LOG_BUFFER; sample_indx++) {
				thread_samples[my_id][sample_indx] = new MethodStatsSample();
			}
		}

		int sample_index = thread_index[my_id];
		sample = thread_samples[my_id][sample_index];

		long ts = 0;
		if(profile_mode != VM_SAMPLE && profile_mode != OS_SAMPLE) {
			ts = System.currentTimeMillis();
		}

		sample.timestamp = ts;
			sample.method_name = method_name;
			sample.event = event;

			if(profile_mode == NAIVE || profile_mode == OS_NAIVE) {
				sample.energy = read_energy();
				//read_energy();
			}

			if(profile_mode == OS_NAIVE) {
				sample.jiffies = get_all_thread_jiffies();
			}

			if(profile_mode == OS_SAMPLE || profile_mode==VM_SAMPLE) {
				sample.epoch = get_energy_epoch();
			}

			sample.thread_name = my_name;
		thread_index[my_id]++;
	}
}

class MethodStatsSample {
	public int epoch;
	public long timestamp;
	public double[] energy;
	public String method_name;
	public List jiffies;
	//Either METHOD_START or METHOD_END
	public int event;
	public String thread_name;
}
