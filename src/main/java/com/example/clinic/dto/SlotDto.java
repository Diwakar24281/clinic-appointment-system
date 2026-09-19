package com.example.clinic.dto;

import java.time.LocalTime;

public class SlotDto {
    private LocalTime time;
    private String displayTime;
    private boolean available;

    public SlotDto() {
    }

    public SlotDto(LocalTime time, String displayTime, boolean available) {
        this.time = time;
        this.displayTime = displayTime;
        this.available = available;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getDisplayTime() {
        return displayTime;
    }

    public void setDisplayTime(String displayTime) {
        this.displayTime = displayTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
