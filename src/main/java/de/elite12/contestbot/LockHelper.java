package de.elite12.contestbot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LockHelper {
	private static Map<String, CommandLock> locks = new HashMap<>();
	private static Object lock = new Object();
	private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	
	public static boolean checkAccess(String key, boolean ispermitted, boolean whisper, int time) {
		if(whisper) return true;
		synchronized (LockHelper.lock) {
			if(ispermitted) {
				if(LockHelper.locks.containsKey(key)) {
					LockHelper.locks.get(key).getTimer().cancel(false);
					LockHelper.locks.get(key).setLocked(true);
					LockHelper.locks.get(key).setTimer(LockHelper.scheduler.schedule(() -> {
						LockHelper.locks.get(key).setLocked(false);
					}, time, TimeUnit.MINUTES));
					return true;
				}
				else {
					LockHelper.locks.put(key, new CommandLock());
					LockHelper.locks.get(key).setLocked(true);
					LockHelper.locks.get(key).setTimer(LockHelper.scheduler.schedule(() -> {
						LockHelper.locks.get(key).setLocked(false);
					}, time, TimeUnit.MINUTES));
					return true;
				}
			}
			else {
				if(LockHelper.locks.containsKey(key)) {
					if(LockHelper.locks.get(key).isLocked()) return false;
					LockHelper.locks.get(key).setLocked(true);
					LockHelper.locks.get(key).setTimer(LockHelper.scheduler.schedule(() -> {
						LockHelper.locks.get(key).setLocked(false);
					}, time, TimeUnit.MINUTES));
					return true;
				}
				else {
					LockHelper.locks.put(key, new CommandLock());
					LockHelper.locks.get(key).setLocked(true);
					LockHelper.locks.get(key).setTimer(LockHelper.scheduler.schedule(() -> {
						LockHelper.locks.get(key).setLocked(false);
					}, time, TimeUnit.MINUTES));
					return true;
				}
			}
		}
	}
	public static boolean checkAccess(String key, boolean ispermitted, boolean whisper) {
		return checkAccess(key, ispermitted, whisper, 3);
	}
	
	private static class CommandLock {
		private ScheduledFuture<?> timer;
		private boolean lock;
		
		public ScheduledFuture<?> getTimer() {
			return this.timer;
		}
		public void setTimer(ScheduledFuture<?> timer) {
			this.timer = timer;
		}
		public boolean isLocked() {
			return this.lock;
		}
		public void setLocked(boolean lock) {
			this.lock = lock;
		}
	}
}