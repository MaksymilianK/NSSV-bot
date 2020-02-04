package pl.konradmaksymilian.nssvbot.session;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimerTest {

    private Timer timer;
    
    @BeforeEach
    public void setUp() {
        timer = new Timer();
    }
    
    @Test
    public void returnTrueIfIsNowAfter() {
        timer.setTime("before", Instant.now().minus(Duration.ofSeconds(5)));
        
        assertThat(timer.isNowAfter("before")).isTrue();
    }
    
    @Test
    public void returnFalseIfIsBefore() {
        timer.setTime("after", Instant.MAX);
        
        assertThat(timer.isNowAfter("after")).isFalse();
    }
    
    @Test
    public void getPositiveDurationIfTimeLeft() {
        timer.setTime("after", Instant.MAX);
        
        assertThat(timer.getTimeLeft("after").isNegative()).isFalse();
    }
    
    @Test
    public void getPositiveDurationIfNoTimeLeft() {
        timer.setTime("before", Instant.now().minus(Duration.ofSeconds(5)));
        
        assertThat(timer.getTimeLeft("before").isNegative()).isTrue();
    }
    
    @Test
    public void returnTrueIfIsNowAfterDuration() {
        timer.setTime("before", Instant.now().minus(Duration.ofSeconds(5)));
        timer.setDuration("duration", Duration.ofSeconds(3));
        
        assertThat(timer.isNowAfterDuration("before", "duration")).isTrue();
    }
    
    @Test
    public void returnFalseIfIsNowBeforeDuration() {
        timer.setTime("before", Instant.now().minus(Duration.ofSeconds(5)));
        timer.setDuration("duration", Duration.ofDays(100));
        
        assertThat(timer.isNowAfterDuration("before", "duration")).isFalse();
    }
    
    @Test
    public void returnTrueIfIsNowAfterDurationProvided() {
        timer.setTime("before", Instant.now().minus(Duration.ofSeconds(5)));
        
        assertThat(timer.isNowAfterDuration("before", Duration.ofSeconds(3))).isTrue();
    }
    
    @Test
    public void returnFalseIfIsNowBeforeDurationProvided() {
        timer.setTime("before", Instant.now().minus(Duration.ofSeconds(5)));
        
        assertThat(timer.isNowAfterDuration("before", Duration.ofDays(100))).isFalse();
    }
}
