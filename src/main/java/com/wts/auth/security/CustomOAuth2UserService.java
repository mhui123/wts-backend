package com.wts.auth.security;

import com.wts.entity.User;
import com.wts.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauthUser = super.loadUser(userRequest);

        // registrationId: "google" 등
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oauthUser.getAttributes();

        // Google의 경우 고유 id는 "sub"에 존재. (기타 공급자는 "id" 등)
        String providerId = attrs.get("sub") != null ? String.valueOf(attrs.get("sub"))
                : (attrs.get("id") != null ? String.valueOf(attrs.get("id")) : null);
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");

        // 1) provider+providerId로 우선 조회
        User user = null;
        if (providerId != null) {
            Optional<User> byProv = userRepository.findByProviderAndProviderId(provider, providerId);
            if (byProv.isPresent()) {
                user = byProv.get();
            }
        }
        // 2) 없으면 이메일 병합
        if (user == null && email != null) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                user = byEmail.get();
            }
        }
        // 3) 최종 없으면 신규 생성
        if (user == null) {
            user = new User(provider, providerId, email, name, picture);
        } else {
            // 업데이트 필드 동기화
            if (provider != null) user.setProvider(provider);

            if (providerId != null) user.setProviderId(providerId);
            if (name != null) user.setName(name);
            if (picture != null) user.setPictureUrl(picture);
            if (email != null && user.getEmail() == null) user.setEmail(email);
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 반환은 기존 OAuth2User 그대로 (시큐리티 컨텍스트용)
        return oauthUser;
    }
}
