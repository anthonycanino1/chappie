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

	public static final int MAX_THREADS = 256;
	public static final int MAX_LOG_BUFFER = 2048;

	public static final int SAMPLES_PER_THREAD=1390240;

	//public static List[] method_stats = new ArrayList[MAX_THREADS];
	//public static PrintWriter[] thread_writers = new PrintWriter[MAX_THREADS];
	//public static int[] thread_index = new int[MAX_THREADS];
	//public static MethodStatsSample[][] thread_samples = new MethodStatsSample[MAX_THREADS][MAX_LOG_BUFFER]; 
	public static String[][] sample_method_names 	= new String[MAX_THREADS][SAMPLES_PER_THREAD];
	public static int[][] sample_event_epochs 	= new int[MAX_THREADS][SAMPLES_PER_THREAD];
	public static int[][] sample_event_types 	= new int [MAX_THREADS][SAMPLES_PER_THREAD];
	public static int[]sample_counts 		= new int[MAX_THREADS];
	public static String[] thread_names      	= new String[MAX_THREADS];

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
			int[] os_stat =  GLIBC.getOSStats(name);
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
		StringBuilder stats_str = new StringBuilder();
		PrintWriter mlog = new PrintWriter(new BufferedWriter(new FileWriter("method_stats.csv")));

		for(int thread_index = 0; thread_index < MAX_THREADS; thread_index++) {
			String thread_name = thread_names[thread_index];
			if(thread_name == null) continue;
			//print samples
			int no_samples = sample_counts[thread_index];
			
			for(int sample_index = 0; no_samples < no_samples; sample_index++) {
				stats_str.append(thread_name).append(",");
				stats_str.append(sample_method_names[thread_index][sample_index]).append(",");
				stats_str.append(sample_event_types[thread_index][sample_index]).append(",");
				stats_str.append(sample_event_epochs[thread_index][sample_index]).append(",");
				stats_str.append("\n");
			}
		}

		
		mlog.print(stats_str.toString());

	}

	public static void notify_before(String method_name, int profile_mode) {
		notify_method(method_name, profile_mode, METHOD_START);
	}

	public static void notify_after(String method_name, int profile_mode) {
		notify_method(method_name, profile_mode, METHOD_END);
	}

	public static void notify_method(String method_name, int profile_mode, int event) {

		Thread cur_thread = Thread.currentThread();
		int my_id = (int) cur_thread.getId();
		String my_name = cur_thread.getName();
		int my_sample_count = sample_counts[my_id]++;
		
		if(my_sample_count==0) {
			thread_names[my_sample_count] = cur_thread.getName();	
		}

		sample_event_epochs[my_id][my_sample_count] = get_energy_epoch();
		sample_event_types[my_id][my_sample_count] = event;
		sample_method_names[my_id][my_sample_count] = method_name;
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
