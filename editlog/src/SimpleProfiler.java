import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SimpleProfiler extends Thread {
	static class Counter {
		int all = 0;
		int only = 0;
	};
	
	private class Profile {
		Thread target;
		HashMap<String, Counter> data = new HashMap<String, Counter>();

		Profile(Thread target) {
			this.target = target;
		}
		
		void Sample() {
			StackTraceElement stack[] = target.getStackTrace();
			
			if (stack.length == 0)
				return;
			
			String fname = stack[0].getClassName()+"."+stack[0].getMethodName();
			Counter ctr = data.get(fname);
			if (ctr == null) {
				ctr = new Counter();
				data.put(fname, ctr);
			}
			ctr.all++;
			ctr.only++;
			
			for(int i=1 ; i<stack.length ; i++) {
				fname = stack[i].getClassName()+"."+stack[i].getMethodName();
				ctr = data.get(fname);
				if (ctr == null) {
					ctr = new Counter();
					data.put(fname, ctr);
				}
				ctr.all++;
			}
		}
	};
	
	ArrayList<Profile> profiles = new ArrayList<Profile>();;
	int period = 100, count = 100;
	File output;
	Logger log = Logger.getAnonymousLogger();
	
	public SimpleProfiler(Thread[] targets, File out) {
		this(Arrays.asList(targets), out);
	}
	
	public SimpleProfiler(Collection<Thread> targets, File out) {
		for (Thread t : targets) {
			this.profiles.add(new Profile(t));
		}
		output = out;
	}
	
	public void setLog(Logger log) {
		this.log = log;
	}
	
	public void setPeriod(int period) {
		this.period = period;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	@Override
	public void run() {
		log.info(String.format("Starting profile, sampling %d threads every %dms for %d times",
				profiles.size(), period, count));
		try {
			long lastSample;
			
			while(count > 0) {
				lastSample = System.currentTimeMillis();
				for(Profile p : profiles)
					p.Sample();
				
				sleep(lastSample+period-System.currentTimeMillis());
				count--;
			}
			
			log.info(String.format("Profile done, writing %s", output.getPath()));
			
			PrintStream out;
			try {
				out = new PrintStream(output);
			} catch (FileNotFoundException e1) {
				log.log(Level.SEVERE, "Failed to open profile output", e1);
				return;
			}
			
			for(Profile p : profiles) {
				out.printf("Thread: %s\n", p.target.getName());
				for(Map.Entry<String, Counter> e : p.data.entrySet()) {
					out.printf("%d\t%d\t%s\n", e.getValue().all, e.getValue().only, e.getKey());
				}
				out.println();
			}
			out.close();
			log.info("Profile saved");
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "Profiling Interrupted", e);
		}
	}
}
