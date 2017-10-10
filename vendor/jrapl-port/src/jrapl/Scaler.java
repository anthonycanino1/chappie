package energy;
import java.io.*;
import java.lang.reflect.Field;
class Scaler {
	public native static int scale(int freq);
	public native static int[] freqAvailable();
	public native static void SetGovernor(String gov);
	static {
		System.setProperty("java.library.path",
				System.getProperty("user.dir") + "/energy");
				//"/home/kenan/dacapo");
		try {
			Field fieldSysPath = ClassLoader.class
					.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {

		}
		System.loadLibrary("CPUScaler");
	}
	public static void main(String[] argv) {
		String option = null;
		option = argv[0];
		SetGovernor("ondemand");
		int[] a = freqAvailable();
		for(int i = 0; i < 16; i++) {
			System.out.println(a[i]);
		}
		
		/*
		if(Integer.parseInt(option) == 1) {
			scale(a[0]);
		} else if(Integer.parseInt(option) == 2) {
			scale(a[3]);
		} else if(Integer.parseInt(option) == 3) {
			scale(a[5]);
		} else if(Integer.parseInt(option) == 4) {
			scale(a[7]);
		} else if(Integer.parseInt(option) == 5) {
			scale(a[9]);
		} else if(Integer.parseInt(option) == 6) {
			scale(a[11]);
		} else if(Integer.parseInt(option) == 7) {
			scale(a[13]);
		} else if(Integer.parseInt(option) == 8) {
			scale(a[15]);
		} else if(Integer.parseInt(option) == 9) {
			scale(a[6]);
		}
		*/
			
	}

}
