package com.wts.api.service;

import com.wts.api.entity.User;
import com.wts.api.repository.UserRepository;
import com.wts.auth.JwtUtil;
import com.wts.auth.dto.JwtResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public JwtResponse loginAsGuest(){
        User guest = createGuestSession();
        setGuestAuthentication(guest);
        String token = jwtUtil.createToken(guest.getEmail());
        return JwtResponse.builder().token(token).build();
    }

    @Transactional
    public User createGuestSession() {
        User guest;
        Optional<User> g = userRepository.findByRoles("ROLE_GUEST");
        if(g.isPresent()) {
            guest = g.get();
        } else {
            // 게스트 ID 생성
            String guestId = "GUEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            // 게스트 사용자 생성
            User guestUser = User.createGuest(guestId);
            guestUser.setRoles("ROLE_GUEST");

            // DB 저장
            guest = userRepository.save(guestUser);
        }


        return guest;
    }

    @Transactional
    public void cleanupExpiredGuests() {
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(24);
        userRepository.deleteByProviderAndCreatedAtBefore("guest", expiredTime);
    }

    public void setGuestAuthentication(User guestUser) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(guestUser, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}