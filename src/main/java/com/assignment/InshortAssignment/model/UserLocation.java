package com.assignment.InshortAssignment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLocation {
    private Double latitude;
    private Double longitude;
    private Double radius; // in kilometers
}
