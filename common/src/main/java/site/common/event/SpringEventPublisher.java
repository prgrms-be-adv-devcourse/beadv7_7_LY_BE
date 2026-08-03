package site.common.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "spring", matchIfMissing = true)
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(final Event event) {
        applicationEventPublisher.publishEvent(event);
    }
}
