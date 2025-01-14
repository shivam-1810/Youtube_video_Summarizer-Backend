package com.shivam.ytsummarizer.Models;

import lombok.Data;

@Data
public class SummaryRequest {
    
    private String videoURL;
    private String prompt;
}
