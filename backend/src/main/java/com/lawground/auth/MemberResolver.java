package com.lawground.auth;

import com.lawground.global.error.ApiException;
import com.lawground.global.error.ErrorCode;
import com.lawground.member.repository.MemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MemberResolver {
    private final MemberRepository members;

    public MemberResolver(MemberRepository members) {
        this.members = members;
    }

    public UUID optionalMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) return null;
        UUID id;
        try {
            id = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException invalid) {
            throw required();
        }
        var member = members.findById(id).orElseThrow(MemberResolver::required);
        if (!member.isActive())
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "접근할 수 없습니다.");
        return id;
    }

    public UUID requiredMemberId() {
        UUID id = optionalMemberId();
        if (id == null) throw required();
        return id;
    }

    private static ApiException required() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }
}
