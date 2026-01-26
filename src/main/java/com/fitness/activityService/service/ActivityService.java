package com.fitness.activityService.service;

import com.fitness.activityService.dto.ActivityRequest;
import com.fitness.activityService.dto.ActivityResponse;
import com.fitness.activityService.model.Activity;
import com.fitness.activityService.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityService {
    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);
    private final ActivityRepository repository;
    private final UserValidationService userValidationService;

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;
    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public ActivityResponse trackActivity(ActivityRequest request) {
        boolean isValid = userValidationService.validateUser(request.getUserId());
        if(!isValid) {
            throw new RuntimeException("User does not exists");
        }
        Activity activity = Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurned(request.getCaloriesBurned())
                .additionalMetrics(request.getAdditionalMetrics())
                .startTime(request.getStartTime())
                .build();
        Activity savedActivity = repository.save(activity);

//        Publish to rabbitmq for AI processing
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, savedActivity);
        }
        catch(Exception e) {
            log.error("Failed to publish the message: ", e);
        }
        return mapToResponse(savedActivity);
    }

    private ActivityResponse mapToResponse(Activity activity) {
        ActivityResponse activityResponse = new ActivityResponse();
        activityResponse.setUserId(activity.getUserId());
        activityResponse.setId(activity.getId());
        activityResponse.setType(activity.getType());
        activityResponse.setCaloriesBurned(activity.getDuration());
        activityResponse.setStartTime(activity.getStartTime());
        activityResponse.setDuration(activity.getDuration());
        activityResponse.setAdditionalMetrics(activity.getAdditionalMetrics());
        activityResponse.setUpdatedAt(activity.getUpdatedAt());
        activityResponse.setCreatedAt(activity.getCreatedAt());
        return activityResponse;
    }

    public List<ActivityResponse> getUserActivities(String userId) {
        boolean isValid = userValidationService.validateUser(userId);
        if(!isValid) {
            throw new RuntimeException("User does not exists");
        }
        List<Activity> activities = repository.findByUserId(userId);
        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}

