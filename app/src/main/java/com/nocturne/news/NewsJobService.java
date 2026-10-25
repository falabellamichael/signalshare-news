package com.nocturne.news;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class NewsJobService extends JobService {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<Integer,Future<?>> running=new HashMap<>();
    @Override public boolean onStartJob(JobParameters params) {
        running.put(params.getJobId(),worker.submit(() -> {
            boolean success=NewsFetcher.refresh(this);
            main.post(() -> {
                if(running.remove(params.getJobId())!=null)
                    jobFinished(params,!success && params.getJobId()==NewsJobs.NOW);
            });
        }));
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        Future<?> future=running.remove(params.getJobId());
        if(future!=null) future.cancel(true);
        return true;
    }
    @Override public void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
