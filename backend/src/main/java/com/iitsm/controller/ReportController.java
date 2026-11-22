package com.iitsm.controller;

import com.iitsm.dto.Views;
import com.iitsm.entity.EmailMessage;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.repository.*;
import com.iitsm.service.ReportService;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReportController {
  private final ReportService reports;
  private final AuditLogRepository audits;
  private final EmailMessageRepository emails;
  private final ViewMapper mapper;

  public ReportController(
      ReportService r, AuditLogRepository a, EmailMessageRepository e, ViewMapper m) {
    reports = r;
    audits = a;
    emails = e;
    mapper = m;
  }

  @GetMapping("/reports/comparison")
  public ReportService.Comparison comparison(@RequestParam String period, @RequestParam java.time.LocalDate date) {
    return reports.comparison(period, date);
  }

  @GetMapping("/reports/overview")
  public Map<String, Object> overview() {
    return reports.overview();
  }

  @GetMapping("/reports/technician-workload")
  @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
  public List<Map<String, Object>> workload() {
    return reports.workload();
  }

  @GetMapping("/reports/category-analysis")
  public List<Map<String, Object>> category() {
    return reports.category();
  }

  @GetMapping("/reports/service-analysis")
  public List<Map<String, Object>> service() {
    return reports.service();
  }

  @GetMapping("/reports/incident-trends")
  public List<Map<String, Object>> trends() {
    return reports.trends();
  }

  @GetMapping("/audit-logs")
  @PreAuthorize("hasRole('ADMIN')")
  public Page<Views.Audit> audit(@RequestParam(defaultValue = "0") int page) {
    return audits
        .findAll(PageRequest.of(Math.max(0, page), 30, Sort.by(Sort.Direction.DESC, "createdAt")))
        .map(mapper::audit);
  }

  @GetMapping("/email-outbox")
  @PreAuthorize("hasRole('ADMIN')")
  public Page<EmailMessage> outbox(@RequestParam(defaultValue = "0") int page) {
    return emails.findAll(
        PageRequest.of(Math.max(page, 0), 30, Sort.by(Sort.Direction.DESC, "createdAt")));
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "UP");
  }
}
