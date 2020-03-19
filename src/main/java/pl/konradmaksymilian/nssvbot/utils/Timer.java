package pl.konradmaksymilian.nssvbot.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Timer {

    private final Map<String, Instant> times = new HashMap<>();
    private final Map<String, Duration> durations = new HashMap<>();
    
    public Instant getTime(String name) {
        return times.get(name);
    }
    
    public void setTime(String name, Instant time) {
        times.put(name, time);
    }
    
    public void setTimeToNow(String name) {
        times.put(name, getNow());
    }

    public void setTimeFromNow(String name, Duration duration) {
        times.put(name, getNow().plus(duration));
    }
    
    public Duration getDuration(String name) {
        return durations.get(name);
    }
    
    public void setDuration(String name, Duration duration) {
        durations.put(name, duration);
    }
    
    public Duration getTimeLeft(String name) {
        return Duration.between(getNow(), times.get(name));
    }
    
    public boolean isNowAfter(String name) {
        return getNow().isAfter(times.get(name));
    }
    
    public boolean isNowAfterDuration(String timeName, String durationName) {
        return Duration.between(times.get(timeName), getNow()).compareTo(durations.get(durationName)) > 0;
    }
    
    public boolean isNowAfterDuration(String name, Duration duration) {
        return Duration.between(times.get(name), getNow()).compareTo(duration) > 0;
    }
    
    public Instant getNow() {
        return Instant.now();
    }
}
