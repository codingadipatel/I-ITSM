package com.iitsm.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.iitsm.entity.*;
import com.iitsm.factory.UserFactory;
import com.iitsm.model.*;
import com.iitsm.repository.*;
import com.iitsm.security.JwtService;
import com.iitsm.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IncidentWorkflowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserFactory factory;
  @Autowired UserRepository users;
  @Autowired SkillRepository skills;
  @Autowired IncidentRepository incidents;
  @Autowired IncidentNoteRepository notes;
  @Autowired AuditLogRepository audits;
  @Autowired NotificationRepository notifications;
  @Autowired EmailMessageRepository emails;
  @Autowired SlaMonitoringService monitor;
  @Autowired JwtService jwt;
  User employee, other, admin;
  Technician tech;
  String empToken, otherToken, techToken, adminToken;

  @BeforeEach
  void setup() {
    employee =
        users.save(factory.create(Role.EMPLOYEE, "Employee", "emp@test.local", "TestingPass123!"));
    other =
        users.save(factory.create(Role.EMPLOYEE, "Other", "other@test.local", "TestingPass123!"));
    admin = users.save(factory.create(Role.ADMIN, "Admin", "admin@test.local", "TestingPass123!"));
    tech =
        (Technician)
            factory.create(Role.TECHNICIAN, "Technician", "tech@test.local", "TestingPass123!");
    tech.configure(true, 10, 5, new HashSet<>(skills.findAll()));
    users.save(tech);
    empToken = jwt.issue(employee.getEmail());
    otherToken = jwt.issue(other.getEmail());
    techToken = jwt.issue(tech.getEmail());
    adminToken = jwt.issue(admin.getEmail());
  }

  String body() {
    return "{\"title\":\"Payment gateway connection failure\",\"description\":\"Payment gateway"
               + " connection timeout prevents customers from completing"
               + " checkout\",\"serviceId\":1,\"categoryId\":1,\"impact\":3,\"urgency\":3,\"affectedUsers\":150}";
  }

  JsonNode create() throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/incidents")
                    .header("Authorization", "Bearer " + empToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  void action(long id, String action, String token, String content, int code) throws Exception {
    mvc.perform(
            post("/api/incidents/" + id + "/" + action)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
        .andExpect(status().is(code));
  }

  @Test
  void endToEndLifecycleHasPersistentAuditAndNotifications() throws Exception {
    var created = create();
    long id = created.get("id").asLong();
    assertEquals("ASSIGNED", created.get("status").asText());
    assertEquals("P1", created.get("priority").asText());
    assertEquals(tech.getId(), created.get("technician").get("id").asLong());
    action(id, "acknowledge", techToken, "{}", 200);
    action(id, "investigate", techToken, "{}", 200);
    action(
        id, "notes", techToken, "{\"details\":\"Investigated connection pool exhaustion\"}", 201);
    action(
        id,
        "request-information",
        techToken,
        "{\"details\":\"Please share the error timestamp\"}",
        200);
    action(id, "notes", empToken, "{\"details\":\"The issue started this morning\"}", 201);
    action(
        id,
        "resolve",
        techToken,
        "{\"details\":\"Increased connection pool capacity and verified payment processing\"}",
        200);
    action(id, "verify", empToken, "{\"details\":\"Successfully completed a test payment\"}", 200);
    action(id, "close", empToken, "{}", 200);
    action(id, "feedback", empToken, "{\"rating\":5,\"comment\":\"Excellent support\"}", 201);
    assertEquals(Status.CLOSED, incidents.findById(id).orElseThrow().getStatus());
    assertTrue(audits.findByIncidentIdOrderByCreatedAtAsc(id).size() >= 12);
    assertEquals(3, notes.findByIncidentIdOrderByCreatedAtAsc(id).size());
    assertFalse(notifications.findByRecipientIdOrderByCreatedAtDesc(employee.getId()).isEmpty());
    var messages = emails.findAll();
    assertEquals(List.of("classified", "assigned", "acknowledged", "in progress", "resolved", "verified", "closed"),
        messages.stream().filter(m -> m.getRecipient().equals(employee.getEmail()))
            .sorted(Comparator.comparing(EmailMessage::getId))
            .map(m -> m.getSubject().substring(("[I-ITSM] INC-" + id + " ").length())).toList());
    var assigned = messages.stream().filter(m -> m.getRecipient().equals(tech.getEmail())).toList();
    assertEquals(1, assigned.size());
    assertTrue(assigned.getFirst().getBody().contains("Response due:"));
    assertTrue(assigned.getFirst().getBody().contains("Resolution due:"));
  }

  @Test
  void slaEmailsReachBothMilestonesOnceForEachOutstandingClock() throws Exception {
    long id = create().get("id").asLong();
    var start = incidents.findById(id).orElseThrow().getCreatedAt();
    monitor.scanAt(start.plusSeconds(149));
    assertEquals(0, slaEmails());
    monitor.scanAt(start.plusSeconds(150));
    assertEquals(1, slaEmails());
    monitor.scanAt(start.plusSeconds(269));
    assertEquals(1, slaEmails());
    monitor.scanAt(start.plusSeconds(270));
    monitor.scanAt(start.plusSeconds(270));
    assertEquals(2, slaEmails());
    action(id, "acknowledge", techToken, "{}", 200);
    monitor.scanAt(start.plusSeconds(900));
    assertEquals(3, slaEmails());
    monitor.scanAt(start.plusSeconds(1620));
    monitor.scanAt(start.plusSeconds(1620));
    assertEquals(4, slaEmails());
    assertTrue(emails.findAll().stream().filter(m -> m.getSubject().contains("SLA reminder"))
        .allMatch(m -> m.getRecipient().equals(tech.getEmail())));
  }

  private long slaEmails() {
    return emails.findAll().stream().filter(m -> m.getSubject().contains("SLA reminder")).count();
  }

  @Test
  void crossEmployeeAccessAndAdministrativeAccessBlocked() throws Exception {
    long id = create().get("id").asLong();
    mvc.perform(get("/api/incidents/" + id).header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/incidents").header("Authorization", "Bearer " + otherToken))
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(get("/api/users").header("Authorization", "Bearer " + empToken))
        .andExpect(status().isForbidden());
    action(id, "acknowledge", empToken, "{}", 403);
    mvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());
  }

  @Test
  void invalidLifecycleAndShortResolutionFail() throws Exception {
    long id = create().get("id").asLong();
    action(id, "close", empToken, "{}", 409);
    action(id, "verify", empToken, "{\"details\":\"verified prematurely\"}", 409);
    action(id, "resolve", techToken, "{\"details\":\"fixed\"}", 400);
    assertEquals(Status.ASSIGNED, incidents.findById(id).orElseThrow().getStatus());
  }

  @Test
  void duplicateSuggestionReviewedByAuthorizedTechnician() throws Exception {
    create();
    long id = create().get("id").asLong();
    var r =
        mvc.perform(get("/api/incidents/" + id).header("Authorization", "Bearer " + techToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var suggestion = json.readTree(r.getContentAsString()).get("duplicates").get(0);
    assertEquals(1.0, suggestion.get("score").asDouble());
    long suggestionId = suggestion.get("id").asLong();
    action(id, "duplicates/" + suggestionId, empToken, "{\"decision\":\"CONFIRMED\"}", 403);
    action(id, "duplicates/" + suggestionId, techToken, "{\"decision\":\"CONFIRMED\"}", 200);
    assertEquals(2, incidents.count());
  }

  @Test
  void slaViolationEscalatesOnce() throws Exception {
    long id = create().get("id").asLong();
    var i = incidents.findById(id).orElseThrow();
    i.getSla().plan(Instant.now().minusSeconds(3600), new SlaPolicy(Priority.P1, 5, 30));
    monitor.scan();
    long before =
        audits.findByIncidentIdOrderByCreatedAtAsc(id).stream()
            .filter(a -> a.getAction().equals("SLA_VIOLATED_ESCALATION"))
            .count();
    assertEquals(1, before);
    assertTrue(i.getSla().isResolutionViolated());
    assertFalse(notifications.findByRecipientIdOrderByCreatedAtDesc(admin.getId()).isEmpty());
    monitor.scan();
    assertEquals(
        before,
        audits.findByIncidentIdOrderByCreatedAtAsc(id).stream()
            .filter(a -> a.getAction().equals("SLA_VIOLATED_ESCALATION"))
            .count());
  }

  @Test
  void validationAndMissingRecordsHaveConsistentErrors() throws Exception {
    mvc.perform(
            post("/api/incidents")
                .header("Authorization", "Bearer " + empToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
    mvc.perform(get("/api/incidents/999999").header("Authorization", "Bearer " + empToken))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/incidents?priority=INVALID").header("Authorization", "Bearer " + empToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void accountDeactivationInvalidatesExistingJwt() throws Exception {
    employee.update(employee.getName(), false);
    users.flush();
    mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + empToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void realLoginAndRegistrationKeepRoleAndHashPrivate() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"emp@test.local\",\"password\":\"TestingPass123!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"emp@test.local\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"New"
                        + " user\",\"email\":\"new@test.local\",\"password\":\"TestingPass123!\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.role").value("EMPLOYEE"));
    assertNotEquals(
        "TestingPass123!",
        users.findByEmailIgnoreCase("new@test.local").orElseThrow().getPasswordHash());
  }

  @Test
  void notificationCannotBeMarkedByAnotherUser() throws Exception {
    create();
    long notificationId =
        notifications.findByRecipientIdOrderByCreatedAtDesc(employee.getId()).get(0).getId();
    mvc.perform(
            patch("/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void filtersCombineAndPaginate() throws Exception {
    create();
    mvc.perform(
            get("/api/incidents?priority=P1&status=ASSIGNED&serviceId=1&categoryId=1&size=1")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].reporter.id").value(employee.getId()));
    mvc.perform(get("/api/incidents?priority=P4").header("Authorization", "Bearer " + adminToken))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void administratorManagesTechnicianCapacityAndAvailability() throws Exception {
    String input =
        "{\"name\":\"New"
            + " technician\",\"email\":\"new-tech@test.local\",\"password\":\"TestingPass123!\",\"role\":\"TECHNICIAN\",\"active\":true,\"available\":true,\"maxWorkload\":3,\"experienceYears\":2,\"skills\":[\"Network\",\"Cloud\"]}";
    var result =
        mvc.perform(
                post("/api/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(input))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.skills.length()").value(2))
            .andReturn()
            .getResponse();
    long userId = json.readTree(result.getContentAsString()).get("id").asLong();
    mvc.perform(
            put("/api/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(input.replace("\"available\":true", "\"available\":false")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false));
    mvc.perform(delete("/api/users/" + userId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
    assertFalse(users.findById(userId).orElseThrow().isActive());
  }

  @Test
  void administratorUpdatesCatalogPoliciesAndStrategies() throws Exception {
    mvc.perform(
            post("/api/services")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Build"
                        + " System\",\"active\":true,\"criticality\":2,\"requiredSkill\":\"Cloud\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Build System"));
    mvc.perform(
            put("/api/sla/policies/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\":\"P1\",\"responseMinutes\":10,\"resolutionMinutes\":45}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolutionMinutes").value(45));
    mvc.perform(
            put("/api/settings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"assignmentStrategy\":\"LOAD\",\"priorityStrategy\":\"RULE\",\"p1Threshold\":25,\"p2Threshold\":15,\"p3Threshold\":8,\"emailEnabled\":false}"))
        .andExpect(status().isOk());
    create();
    assertEquals(0, emails.count());
    assertTrue(notifications.count() > 0);
  }

  @Test
  void unavailableTechnicianLeavesIncidentClassified() throws Exception {
    tech.configure(false, 10, 5, new HashSet<>(skills.findAll()));
    long id = create().get("id").asLong();
    assertEquals(Status.CLASSIFIED, incidents.findById(id).orElseThrow().getStatus());
    action(
        id,
        "assign",
        adminToken,
        "{\"technicianId\":" + tech.getId() + ",\"reason\":\"Manual override attempted\"}",
        409);
  }

  @Test
  void reopeningRequiresReasonAndReturnsToInvestigation() throws Exception {
    long id = create().get("id").asLong();
    action(id, "acknowledge", techToken, "{}", 200);
    action(id, "investigate", techToken, "{}", 200);
    action(id, "resolve", techToken, "{\"details\":\"Repaired the payment connection pool\"}", 200);
    action(id, "reopen", empToken, "{\"details\":\"bad\"}", 400);
    action(
        id,
        "reopen",
        empToken,
        "{\"details\":\"Payment errors continue after retrying checkout\"}",
        200);
    var i = incidents.findById(id).orElseThrow();
    assertEquals(Status.IN_PROGRESS, i.getStatus());
    assertNull(i.getSla().getResolvedAt());
  }

  @Test
  void informationRequestDeniedToOtherTechnician() throws Exception {
    long id = create().get("id").asLong();
    var otherTech =
        users.save(
            factory.create(
                Role.TECHNICIAN, "Other Tech", "second-tech@test.local", "TestingPass123!"));
    String token = jwt.issue(otherTech.getEmail());
    action(
        id,
        "request-information",
        token,
        "{\"details\":\"Please provide your account details\"}",
        403);
  }

  @Test
  void chatIsTwoWayAndFollowsTheCurrentAssignment() throws Exception {
    long id = create().get("id").asLong();
    var message = json.writeValueAsString(Map.of("technicianId", tech.getId(), "body", "Payment is back"));
    action(id, "chat", empToken, message, 201);
    action(id, "chat", techToken, message, 201);
    var url = "/api/incidents/" + id + "/chat?technicianId=" + tech.getId();
    mvc.perform(get(url).header("Authorization", "Bearer " + empToken))
        .andExpect(jsonPath("$[0].senderId").value(employee.getId()))
        .andExpect(jsonPath("$[1].senderId").value(tech.getId()))
        .andExpect(jsonPath("$[0].notificationReply").value(false))
        .andExpect(jsonPath("$[1].notificationReply").value(false));
    mvc.perform(get(url).header("Authorization", "Bearer " + otherToken)).andExpect(status().isForbidden());
    var replacement = users.save(factory.create(Role.TECHNICIAN, "Replacement", "replacement@test.local", "TestingPass123!"));
    action(id, "assign", adminToken, json.writeValueAsString(Map.of("technicianId", replacement.getId(), "reason", "Shift change")), 200);
    action(id, "chat", empToken, message, 409);
    mvc.perform(get(url).header("Authorization", "Bearer " + techToken)).andExpect(status().isForbidden());
    mvc.perform(get("/api/incidents/" + id + "/chat?technicianId=" + replacement.getId())
        .header("Authorization", "Bearer " + jwt.issue(replacement.getEmail())))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void notificationRepliesShareIncidentChatAndNotifyOnlyTheOtherParticipant() throws Exception {
    long id = create().get("id").asLong();
    var source = notifications.findByRecipientIdOrderByCreatedAtDesc(employee.getId()).getFirst();
    String replyUrl = "/api/notifications/" + source.getId() + "/reply";
    var first = json.writeValueAsString(Map.of("technicianId", tech.getId(), "body", "Can you check the gateway?"));

    mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + empToken))
        .andExpect(jsonPath("$[0].conversation.technicianId").value(tech.getId()));
    mvc.perform(post(replyUrl).header("Authorization", "Bearer " + otherToken)
        .contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isForbidden());
    assertFalse(source.isRead());
    long emailCount = emails.count();
    mvc.perform(post(replyUrl).header("Authorization", "Bearer " + empToken)
        .contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isCreated());
    assertTrue(source.isRead());

    var received = notifications.findByRecipientIdOrderByCreatedAtDesc(tech.getId()).getFirst();
    assertEquals("CHAT_MESSAGE", received.getEventType());
    assertFalse(received.isRead());
    var second = json.writeValueAsString(Map.of("technicianId", tech.getId(), "body", "I am checking it now."));
    mvc.perform(post("/api/notifications/" + received.getId() + "/reply").header("Authorization", "Bearer " + techToken)
        .contentType(MediaType.APPLICATION_JSON).content(second)).andExpect(status().isCreated());
    assertEquals("CHAT_MESSAGE",
        notifications.findByRecipientIdOrderByCreatedAtDesc(employee.getId()).getFirst().getEventType());
    assertEquals(emailCount, emails.count());

    var chatUrl = "/api/incidents/" + id + "/chat?technicianId=" + tech.getId();
    mvc.perform(get(chatUrl).header("Authorization", "Bearer " + empToken))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].body").value("Can you check the gateway?"))
        .andExpect(jsonPath("$[1].body").value("I am checking it now."))
        .andExpect(jsonPath("$[0].notificationReply").value(true))
        .andExpect(jsonPath("$[1].notificationReply").value(true));

    var replacement = users.save(factory.create(Role.TECHNICIAN, "Replacement", "replacement@test.local", "TestingPass123!"));
    action(id, "assign", adminToken, json.writeValueAsString(Map.of("technicianId", replacement.getId(), "reason", "Shift change")), 200);
    mvc.perform(post(replyUrl).header("Authorization", "Bearer " + empToken)
        .contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isConflict());
    mvc.perform(post("/api/notifications/" + received.getId() + "/reply").header("Authorization", "Bearer " + techToken)
        .contentType(MediaType.APPLICATION_JSON).content(second)).andExpect(status().isForbidden());
    mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + techToken))
        .andExpect(jsonPath("$[0].conversation").doesNotExist());
  }

  @Test
  void exportsAllFilteredVisibleRowsAndComparesCalendarPeriods() throws Exception {
    var today = LocalDate.now(ZoneOffset.UTC);
    create(); create();
    mvc.perform(get("/api/incidents/export?size=1&priority=P1").header("Authorization", "Bearer " + empToken))
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(get("/api/incidents/export?priority=P4").header("Authorization", "Bearer " + empToken))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/incidents/export").header("Authorization", "Bearer " + otherToken))
        .andExpect(jsonPath("$.length()").value(0));
    for (String period : List.of("week","month","year")) {
      String url = "/api/reports/comparison?period=" + period + "&date=" + today;
      mvc.perform(get(url).header("Authorization", "Bearer " + empToken))
          .andExpect(jsonPath("$.total.selected").value(2))
          .andExpect(jsonPath("$.total.previous").value(0))
          .andExpect(jsonPath("$.technicians[0].id").value(tech.getId()));
      mvc.perform(get(url).header("Authorization", "Bearer " + otherToken))
          .andExpect(jsonPath("$.total.selected").value(0));
    }
    mvc.perform(get("/api/reports/comparison?period=week&date=2026-01-01").header("Authorization", "Bearer " + empToken))
        .andExpect(jsonPath("$.selectedStart").value("2025-12-29"))
        .andExpect(jsonPath("$.endExclusive").value("2026-01-05"));
  }
}
