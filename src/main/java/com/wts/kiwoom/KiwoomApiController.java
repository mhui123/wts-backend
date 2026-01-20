package com.wts.kiwoom;

import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.dto.WatchListDto;
import com.wts.kiwoom.service.KiwoomApiService;
import com.wts.kiwoom.service.KiwoomPublicService;
import com.wts.kiwoom.service.KiwoomAuditService;
import com.wts.model.ProcessResult;
import com.wts.util.UtilsForRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kiwoom")
@RequiredArgsConstructor
public class KiwoomApiController {

    private final KiwoomAuditService auditService;
    private final KiwoomApiService apiService;
    private final UtilsForRequest uRe;
    // 조회 권한만 있으면 호출 가능
    @PostMapping("/account/balance")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(authentication.id, 'BASIC_USER')")
    public ResponseEntity<?> getAccountBalance(HttpServletRequest request, @RequestBody KiwoomApiRequest req){
        long startTime = System.currentTimeMillis();

        try {
            // 계좌 잔고 조회 로직
            ProcessResult result = apiService.getAccountInfo(req);
            // 성공 결과 생성
//            ProcessResult result = ProcessResult.success("계좌 잔고 조회 완료");

            // 비동기 감사 로깅
            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    req.getUserId(),
                    "/api/kiwoom/account/balance",
                    executionTime,
                    result
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // 실패 결과 생성
            ProcessResult result = ProcessResult.failure("계좌 잔고 조회 실패: " + e.getMessage());

            // 비동기 감사 로깅 (에러 포함)
            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    req.getUserId(),
                    "/api/kiwoom/account/balance",
                    executionTime,
                    result
            );

            return ResponseEntity.internalServerError().body("처리 중 오류가 발생했습니다.");
        }
    }

//    // 거래 권한이 있어야 호출 가능
//    @PostMapping("/trade/order")
//    @PreAuthorize("@kiwoomPermissionService.hasPermission(authentication.name, 'TRADING_USER')")
//    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
//        // 주문 실행
//    }
//
    // 관리자만 호출 가능
    @PostMapping("/admin/setStockCodes")
//    @PreAuthorize("@kiwoomPermissionService.hasPermission(authentication.name, 'ADMIN_USER')")
    public ResponseEntity<?> syncStockCdsWithMarket(HttpServletRequest request) {
        ProcessResult result = apiService.syncKiwoomStocks();
        return ResponseEntity.ok().body(result);
    }


    @GetMapping("/stocks/master")
    public ResponseEntity<?> getAllStockCodeName(HttpServletRequest request){
        try {
            ProcessResult result = apiService.getAllStockCodeName();
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ProcessResult.failure("처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/watchList/groups/{userId}")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(#userId, 'BASIC_USER')")
    public ResponseEntity<?> getUserWatchListGroups(HttpServletRequest request,
                                                    @PathVariable long userId){
        try {
            String jwt;
            jwt = uRe.attractJwtFromRequest(request);
            if( jwt == null) {
                String msg = "Authorization 헤더에서 JWT를 찾을 수 없습니다.";
                return ResponseEntity.internalServerError().body(ProcessResult.failure(msg));
            }
            ProcessResult result = apiService.getUserWatchList(jwt);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ProcessResult.failure("처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/watchlist/add")
    public ResponseEntity<?> addUserWatchListItem(HttpServletRequest request,
                                                  @RequestBody WatchListDto dto){
        try {
            long userId = dto.getUserId();
            List<String> stockCodes = dto.getStockCodes();
            String groupName = dto.getGroupName();

            ProcessResult result = apiService.addUserWatchListItem(userId, stockCodes, groupName);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ProcessResult.failure("처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/watchlist/syncgroups/{userId}")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(#userId, 'BASIC_USER')")
    public ResponseEntity<?> syncWatchGroup(@PathVariable String userId,
                                            @RequestBody WatchListDto dto,
                                            HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        String groupName = dto.getGroupName();
        long longUserId = Long.parseLong(userId);
        String groupId = dto.getGroupId();
        List<String> stockCodes = dto.getStockCodes();

        try {
            ProcessResult result = apiService.syncUserWatchList(longUserId, groupId, groupName, stockCodes);

            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    longUserId,
                    "/api/kiwoom/users/" + userId + "/watchlist/groups/" + groupName,
                    executionTime,
                    result
            );

            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            ProcessResult result = ProcessResult.failure("관심종목 그룹 동기화 실패: " + e.getMessage());

            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    longUserId,
                    "/api/kiwoom/users/" + userId + "/watchlist/groups/" + groupName,
                    executionTime,
                    result
            );

            return ResponseEntity.internalServerError().body("처리 중 오류가 발생했습니다.");
        }
    }

    // 관심종목 그룹삭제
    @DeleteMapping("/watchlist/delgroups/{userId}/{groupId}")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(#userId, 'BASIC_USER')")
    public ResponseEntity<?> deleteWatchGroup(@PathVariable String userId,
                                              @PathVariable String groupId,
                                              HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        long longGroupId = Long.parseLong(groupId);
        long longUserId = Long.parseLong(userId);

        try {
            ProcessResult result = apiService.deleteWatchGroup(longUserId, longGroupId);

            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    longUserId,
                    "/watchlist/delgroups/" + userId + "/" + groupId,
                    executionTime,
                    result
            );

            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            ProcessResult result = ProcessResult.failure("관심종목 그룹 삭제 실패: " + e.getMessage());

            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    longUserId,
                    "/watchlist/delgroups/" + userId + "/" + groupId,
                    executionTime,
                    result
            );

            return ResponseEntity.internalServerError().body("처리 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/realtime/subscribe/{userId}")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(#userId, 'BASIC_USER')")
    public ResponseEntity<?> subscribePriceData(@PathVariable String userId, @RequestBody WatchListDto dto, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        String jwt = uRe.attractJwtFromRequest(request);
        if( jwt == null) {
            String msg = "Authorization 헤더에서 JWT를 찾을 수 없습니다.";
            return ResponseEntity.internalServerError().body(ProcessResult.failure(msg));
        }

        try {
            ProcessResult result = apiService.reqRealTimeData(jwt, dto, "subscribe");

            long executionTime = System.currentTimeMillis() - startTime;
//            auditService.logApiRequest(
//                    longUserId,
//                    "/api/kiwoom/users/" + userId + "/watchlist/groups/" + groupName,
//                    executionTime,
//                    result
//            );

            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            ProcessResult result = ProcessResult.failure("관심종목 그룹 동기화 실패: " + e.getMessage());

//            long executionTime = System.currentTimeMillis() - startTime;
//            auditService.logApiRequest(
//                    longUserId,
//                    "/api/kiwoom/users/" + userId + "/watchlist/groups/" + groupName,
//                    executionTime,
//                    result
//            );

            return ResponseEntity.internalServerError().body("처리 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/realtime/unsubscribe/{userId}")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(#userId, 'BASIC_USER')")
    public ResponseEntity<?> unsubscribePriceData(@PathVariable String userId, @RequestBody WatchListDto dto, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        String jwt = uRe.attractJwtFromRequest(request);
        if( jwt == null) {
            String msg = "Authorization 헤더에서 JWT를 찾을 수 없습니다.";
            return ResponseEntity.internalServerError().body(ProcessResult.failure(msg));
        }

        try {
            ProcessResult result = apiService.reqRealTimeData(jwt, dto, "unsubscribe");

            long executionTime = System.currentTimeMillis() - startTime;
//            auditService.logApiRequest(
//                    longUserId,
//                    "/api/kiwoom/users/" + userId + "/watchlist/groups/" + groupName,
//                    executionTime,
//                    result
//            );

            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            ProcessResult result = ProcessResult.failure("관심종목 그룹 동기화 실패: " + e.getMessage());

//            long executionTime = System.currentTimeMillis() - startTime;
//            auditService.logApiRequest(
//                    longUserId,
//                    "/api/kiwoom/users/" + userId + "/watchlist/groups/" + groupName,
//                    executionTime,
//                    result
//            );

            return ResponseEntity.internalServerError().body("처리 중 오류가 발생했습니다.");
        }
    }
}
