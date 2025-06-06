package org.monarchinitiative.maxodiff.html.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RankMaxoService {

    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private int totalTasks = 0;

    public void setTotalTasks(int total) {
        this.totalTasks = total;
        this.completedTasks.set(0);
    }

    public void taskCompleted() {
        completedTasks.incrementAndGet();
    }

    public int getProgressPercentage() {
            return totalTasks == 0 ? 0 : (completedTasks.get() * 100) / totalTasks;
        }

}
