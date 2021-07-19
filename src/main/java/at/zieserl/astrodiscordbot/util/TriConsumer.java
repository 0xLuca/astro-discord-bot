package at.zieserl.astrodiscordbot.util;

import java.util.Objects;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);

    default TriConsumer<T, U, V> andThen(final TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);

        return (l, m, r) -> {
            accept(l, m, r);
            after.accept(l, m, r);
        };
    }
}