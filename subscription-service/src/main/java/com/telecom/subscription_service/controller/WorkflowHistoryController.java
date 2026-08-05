package com.telecom.subscription_service.controller;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/history")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WorkflowHistoryController {

    private final HistoryService historyService;

    @GetMapping(value = "/bpmn-xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Resource> getBpmnXml() {
        return ResponseEntity.ok(new ClassPathResource("processes/subscreation.bpmn20.xml"));
    }

    @GetMapping("/customer/{customerId}/instances")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<Map<String, String>>> getCustomerInstances(@PathVariable String customerId) {
        List<HistoricProcessInstance> instances = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(customerId)
                .orderByProcessInstanceStartTime()
                .desc()
                .list();

        List<Map<String, String>> result = instances.stream().map(i -> {
            Map<String, String> map = new HashMap<>();
            map.put("instanceId", i.getId());
            map.put("startTime", i.getStartTime().toString());
            map.put("status", i.getEndTime() != null ? "COMPLETED" : "FAILED_OR_RUNNING");

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/instance/{instanceId}/activities")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getInstanceActivities(@PathVariable String instanceId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<Map<String, Object>> result = activities.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("activityId", a.getActivityId());
            map.put("activityName", a.getActivityName());
            map.put("type", a.getActivityType());
            map.put("startTime", a.getStartTime());
            map.put("endTime", a.getEndTime());
            map.put("durationMs", a.getDurationInMillis());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/instance/{instanceId}/variables")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getInstanceVariables(@PathVariable String instanceId) {
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instanceId)
                .list();

        Map<String, Object> variableMap = variables.stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue
                ));

        return ResponseEntity.ok(variableMap);
    }
}