package site.common.event;

public interface EventHandler<T extends Event> {

    void handle(T event);
}
