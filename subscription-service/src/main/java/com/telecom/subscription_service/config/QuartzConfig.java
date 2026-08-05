package com.telecom.subscription_service.config;

import com.telecom.subscription_service.job.SubscriptionExpiryJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail expiryJobDetail() {
        return JobBuilder.newJob(SubscriptionExpiryJob.class)
                .withIdentity("subscriptionExpiryJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger expiryJobTrigger() {
        // PRO TIP: For production environments, use the cron expression "0 0 0 * * ?" to run exactly at midnight.
        // For development and testing purposes, we schedule it to run every minute at the 00th second.
        return TriggerBuilder.newTrigger()
                .forJob(expiryJobDetail())
                .withIdentity("subscriptionExpiryTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
                .build();
    }
}