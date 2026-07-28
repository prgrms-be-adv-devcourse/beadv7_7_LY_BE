package site.memberservice.auth.application.dto;

import site.memberservice.member.domain.Email;

public record LoginCommand(Email email, String password) {
}
