package com.cooking.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class CookingTimerScheduler {

	private long taskDuration;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public CookingTimerScheduler(String taskName, String durationInMilis) {

		this.taskDuration = Long.parseLong(durationInMilis);
		
		System.out.println("Timer for " + taskName + " has started.");

		Runnable task = () -> {

			System.out.println("Timer for " + taskName + " has completed.");
			
		};
		
		scheduler.schedule(task, taskDuration, TimeUnit.MILLISECONDS);

	}
}
