package com.nocturne.news;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class NewsJobs {
    public static final int PERIODIC=100, NOW=101;
    public static final long PERIOD=900_000;
    private NewsJobs() {}
    public static void ensurePeriodic(Context context) {
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);
        if(scheduler.getPendingJob(PERIODIC)==null) scheduler.schedule(new JobInfo.Builder(PERIODIC,
                new ComponentName(context,NewsJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD,300_000).setPersisted(true).build());
    }
    public static void requestNow(Context context) {
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);
        if(scheduler.getPendingJob(NOW)!=null) return;
        JobInfo.Builder builder=new JobInfo.Builder(NOW,new ComponentName(context,NewsJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if(scheduler.schedule(builder.setExpedited(true).build())!=JobScheduler.RESULT_SUCCESS)
            scheduler.schedule(builder.setExpedited(false).build());
    }
    public static void stopPeriodic(Context context) { context.getSystemService(JobScheduler.class).cancel(PERIODIC); }
}
