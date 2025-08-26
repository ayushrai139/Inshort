package com.assignment.InshortAssignment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmQueryAnalysis {
    private List<String> entities;
    private String intent;
    private String originalQuery;
}
