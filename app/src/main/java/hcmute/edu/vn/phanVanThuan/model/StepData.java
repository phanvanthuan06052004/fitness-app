package hcmute.edu.vn.phanVanThuan.model;

import java.util.Date;

public class StepData {
    private int id;
    private int stepCount;
    private Date date;
    private Date startTime;
    private long durationInSeconds;

    public StepData() {
        this.date = new Date();
    }

    public StepData(int stepCount) {
        this.stepCount = stepCount;
        this.date = new Date();
    }

    public StepData(int stepCount, Date startTime) {
        this.stepCount = stepCount;
        this.date = new Date();
        this.startTime = startTime;
        if (startTime != null) {
            this.durationInSeconds = (this.date.getTime() - startTime.getTime()) / 1000;
        }
    }

    public StepData(int id, int stepCount, Date date, Date startTime, long durationInSeconds) {
        this.id = id;
        this.stepCount = stepCount;
        this.date = date;
        this.startTime = startTime;
        this.durationInSeconds = durationInSeconds;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
        if (startTime != null && date != null) {
            this.durationInSeconds = (this.date.getTime() - startTime.getTime()) / 1000;
        }
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(long durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public String getFormattedDuration() {
        long hours = durationInSeconds / 3600;
        long minutes = (durationInSeconds % 3600) / 60;
        long seconds = durationInSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    @Override
    public String toString() {
        return "StepData{" +
                "id=" + id +
                ", stepCount=" + stepCount +
                ", date=" + date +
                ", startTime=" + startTime +
                ", durationInSeconds=" + durationInSeconds +
                '}';
    }
} 