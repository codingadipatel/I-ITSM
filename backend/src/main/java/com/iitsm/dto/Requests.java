package com.iitsm.dto;

import com.iitsm.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public final class Requests {
  private Requests() {}

  public record Register(
      @NotBlank @Size(max = 120) String name,
      @Email @NotBlank @Size(max = 200) String email,
      @NotBlank @Size(min = 10, max = 72) String password) {}

  public record Login(@Email @NotBlank String email, @NotBlank String password) {}

  public record IncidentInput(
      @NotBlank @Size(min = 5, max = 180) String title,
      @NotBlank @Size(min = 15, max = 10000) String description,
      @NotNull Long serviceId,
      @NotNull Long categoryId,
      @Min(1) @Max(3) int impact,
      @Min(1) @Max(3) int urgency,
      @Min(1) @Max(1000000) int affectedUsers) {}

  public record Detail(@NotBlank @Size(min = 3, max = 10000) String details) {}

  public record Assign(
      @NotNull Long technicianId, @NotBlank @Size(min = 3, max = 255) String reason) {}

  public record Rating(@Min(1) @Max(5) int rating, @Size(max = 2000) String comment) {}

  public record UserInput(
      @NotBlank @Size(max = 120) String name,
      @Email @NotBlank @Size(max = 200) String email,
      @Size(min = 10, max = 72) String password,
      @NotNull Role role,
      boolean active,
      boolean available,
      @Min(1) @Max(100) int maxWorkload,
      @Min(0) @Max(60) int experienceYears,
      @NotNull Set<@NotBlank @Size(max = 80) String> skills) {}

  public record Profile(@NotBlank @Size(max = 120) String name) {}

  public record CatalogInput(
      @NotBlank @Size(max = 120) String name,
      boolean active,
      @Min(1) @Max(3) int criticality,
      @NotBlank @Size(max = 80) String requiredSkill) {}

  public record Policy(
      @NotNull Priority priority,
      @Min(1) @Max(525600) int responseMinutes,
      @Min(1) @Max(525600) int resolutionMinutes) {}

  public record Settings(
      @NotBlank String assignmentStrategy,
      @NotBlank String priorityStrategy,
      @DecimalMin("0.1") @DecimalMax("1000") double p1Threshold,
      @DecimalMin("0.1") @DecimalMax("1000") double p2Threshold,
      @DecimalMin("0.1") @DecimalMax("1000") double p3Threshold,
      boolean emailEnabled) {}

  public record ChatMessage(
      @Positive long technicianId, @NotBlank @Size(max = 4000) String body) {}

  public record Decision(@Pattern(regexp = "CONFIRMED|REJECTED") @NotNull String decision) {}
}
