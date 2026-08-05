package com.telecom.notification_service.provider;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;

public interface NotificationProvider {

	void sendExpiring(SubscriptionExpiringEvent event);
	void sendCreated(SubscriptionCreatedEvent event);
}
