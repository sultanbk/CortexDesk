package com.prodapt.network_ticketing.service;

import com.prodapt.network_ticketing.entity.Ticket;

public interface SlaAlertService {
    void sendSlaBreachAlert(Ticket ticket);
}
