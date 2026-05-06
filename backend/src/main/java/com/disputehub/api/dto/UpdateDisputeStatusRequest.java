package com.disputehub.api.dto;

import com.disputehub.api.entity.DisputeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDisputeStatusRequest {
    @NotNull(message = "Status is required")
    private DisputeStatus status;

    @Size(max = 1000, message = "Resolution notes cannot exceed 1000 characters")
    private String resolutionNotes;
}
