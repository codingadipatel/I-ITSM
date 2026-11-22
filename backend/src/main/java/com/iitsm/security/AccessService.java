package com.iitsm.security;

import com.iitsm.entity.*;
import com.iitsm.exception.*;
import com.iitsm.model.Role;
import com.iitsm.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessService {
  private final UserRepository users;

  public AccessService(UserRepository users) {
    this.users = users;
  }

  public User current() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) throw new UnauthorizedOperationException("Authentication required");
    return users
        .findByEmailIgnoreCase(a.getName())
        .filter(User::isActive)
        .orElseThrow(() -> new UnauthorizedOperationException("Account unavailable"));
  }

  public void read(User u, Incident i) {
    if (u.getRole() == Role.ADMIN
        || i.getReporter().getId().equals(u.getId())
        || (u.getRole() == Role.TECHNICIAN
            && i.getTechnician() != null
            && i.getTechnician().getId().equals(u.getId()))) return;
    throw new UnauthorizedOperationException("You cannot access this incident");
  }

  public void technician(User u, Incident i) {
    if (u.getRole() == Role.ADMIN) return;
    if (u.getRole() != Role.TECHNICIAN
        || i.getTechnician() == null
        || !i.getTechnician().getId().equals(u.getId()))
      throw new UnauthorizedOperationException(
          "Only the assigned technician or administrator can do this");
  }

  public void reporter(User u, Incident i) {
    if (!i.getReporter().getId().equals(u.getId()))
      throw new UnauthorizedOperationException(
          "Only the reporter can verify, close or rate this incident");
  }

  public void admin(User u) {
    if (u.getRole() != Role.ADMIN)
      throw new UnauthorizedOperationException("Administrator access required");
  }
}
