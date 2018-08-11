package chappie.util;


import jrapl.EnergyCheckUtils.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class StatsUtil {
	
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


	//The jiffies for the calling thread will be the first entry
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



	public static final int NAIVE=0;
	public static final int OS_NAIVE=1;
	public static final int OS_SAMPLE=2;
	public static final int VM_SAMPLE=3;

	public static int get_energy_epoch() {
		return -1;
	}

	//This will be called at the end of Chappie by a possibly VM Shutdown Hook
	public static void print_method_stats() {
		System.out.println("Hi ... I am printing ... Implementing to Come Very Soon");
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


		int my_id = (int) Thread.currentThread().getId();
		List this_thread_stats = method_stats[my_id];
		if(this_thread_stats == null) {
			method_stats[my_id] = this_thread_stats = new ArrayList();
		}

		this_thread_stats.add(sample);
	}
}

class MethodStatsSample {
	public int epoch;
	public long timestamp;
	public List<List<Double>> energy;
	public String method_name;
	public List jiffies;
	public long mythread_jiffiers;
	//Either METHOD_START or METHOD_END
	public int event;
}


