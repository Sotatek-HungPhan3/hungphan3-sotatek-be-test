package com.sotatek.order.application.port.out;

import com.sotatek.order.application.dto.MemberDto;

import java.util.Optional;

/**
 * Output port for Member Service integration.
 */
public interface MemberClientPort {

    /**
     * Get member by ID.
     * 
     * @param memberId the member ID
     * @return Member if found
     * @throws com.sotatek.order.application.exception.ExternalServiceException on
     *                                                                          timeout/unavailable
     */
    Optional<MemberDto> getMember(Long memberId);
}
