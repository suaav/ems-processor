import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

class Event {
    private String subject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
    private String location;

    String getSubject() {
        return subject;
    }

    void setSubject(String subject) {
        this.subject = subject;
    }

    LocalDateTime getStartTime() {
        return startTime;
    }

    void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    LocalDateTime getEndTime() {
        return endTime;
    }

    void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String getLocation() {
        return location;
    }

    void setLocation(String location) {
        this.location = location;
    }
}
