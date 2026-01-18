package com.prodapt.network_ticketing.dto;

public class AddAiResolutionRequest {

    private Long ticketId;
    private String aiResolution;

    public AddAiResolutionRequest() {
    }

    public AddAiResolutionRequest(Long ticketId, String aiResolution) {
        this.ticketId = ticketId;
        this.aiResolution = aiResolution;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getAiResolution() {
        return aiResolution;
    }

    public void setAiResolution(String aiResolution) {
        this.aiResolution = aiResolution;
    }
}

