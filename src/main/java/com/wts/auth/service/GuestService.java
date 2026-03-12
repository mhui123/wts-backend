package com.wts.auth.service;

import com.wts.auth.JwtUtil;
import com.wts.auth.dto.JwtResponse;
import com.wts.auth.jpa.entity.User;
import com.wts.auth.jpa.repository.UserRepository;
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
        Optional<User> g = userRepository.findById(1L); // ID 1번은 게스트 사용자로 고정
        if(g.isPresent()) {
            guest = g.get();
        } else {
            // 게스트 ID 생성
            String guestId = "GUEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            // 게스트 사용자 생성
            User guestUser = createGuest(guestId);
            guestUser.setRoles("ROLE_GUEST");

            // DB 저장
            guest = userRepository.save(guestUser);
        }


        return guest;
    }

    public void setGuestAuthentication(User guestUser) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(guestUser, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static User createGuest(String guestId) {
        User guest = new User();
        guest.setProvider("guest");
        guest.setProviderId(guestId);
        guest.setEmail(guestId + "@guest.wts");
        guest.setName("게스트_" + guestId.substring(6, 14)); // GUEST_ 제거 후 8자리
        guest.setPictureUrl(null);
        guest.setRoles("ROLE_GUEST");
        guest.setEnabled(true);
        return guest;
    }

    public boolean isGuest(User user) {
        return "guest".equals(user.getProvider());
    }

    public boolean isExpiredGuest(User user) {
        if (!isGuest(user)) return false;
        return user.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24));
    }
}