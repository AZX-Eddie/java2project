package com.example.demo.model.dto;

public class CoOccurrenceDTO {

    private String topic1;
    private String topic2;
    private Long frequency;
    private Double percentage;

    public CoOccurrenceDTO() {}

    public CoOccurrenceDTO(String topic1, String topic2, Long frequency, Double percentage) {
        this.topic1 = topic1;
        this.topic2 = topic2;
        this.frequency = frequency;
        this.percentage = percentage;
    }

    // ======= Getter/Setter ========
    public String getTopic1() { return topic1; }
    public void setTopic1(String topic1) { this.topic1 = topic1; }

    public String getTopic2() { return topic2; }
    public void setTopic2(String topic2) { this.topic2 = topic2; }

    public Long getFrequency() { return frequency; }
    public void setFrequency(Long frequency) { this.frequency = frequency; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
}