package com.wts.api.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticResourceController {

    @GetMapping({"/", "/login"})
    public String forwardToIndex() {
        return "forward:/index.html"; // 프론트엔드 빌드 파일로 포워드
    }
}
