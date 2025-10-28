// 주문 응답 DTO: 외부 브로커 또는 내부 로직이 반환하는 주문 결과를 표현합니다.
// 주요 책임: 클라이언트 주문 ID, 처리 상태, 브로커 주문 ID 등의 정보를 캡슐화
package com.wts.model;

public class OrderResponse {
    private String clientOrderId;
    private String status;
    private String brokerOrderId;

    public OrderResponse() {}

    public OrderResponse(String clientOrderId, String status, String brokerOrderId) {
        this.clientOrderId = clientOrderId;
        this.status = status;
        this.brokerOrderId = brokerOrderId;
    }

    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBrokerOrderId() { return brokerOrderId; }
    public void setBrokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; }
}
