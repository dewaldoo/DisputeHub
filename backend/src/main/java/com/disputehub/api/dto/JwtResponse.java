package com.disputehub.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String role;

    @JsonCreator
    public JwtResponse(
            @JsonProperty("token") String token,
            @JsonProperty("id") Long id,
            @JsonProperty("username") String username,
            @JsonProperty("role") String role
    ) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.role = role;
    }
}
