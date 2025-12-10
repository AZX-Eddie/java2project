package com.example.demo.model.dto;

import java.util.List;

public class MultithreadingPitfallDTO {

    private String pitfallCategory;
    private String description;
    private Long occurrenceCount;
    private Double percentage;
    private List<String> keywords;
    private List<String> exampleTitles;

    public MultithreadingPitfallDTO() {}

    // ======= Getter/Setter ========
    public String getPitfallCategory() { return pitfallCategory; }
    public void setPitfallCategory(String pitfallCategory) { this.pitfallCategory = pitfallCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Long occurrenceCount) { this.occurrenceCount = occurrenceCount; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getExampleTitles() { return exampleTitles; }
    public void setExampleTitles(List<String> exampleTitles) { this.exampleTitles = exampleTitles; }
}