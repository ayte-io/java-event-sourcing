package io.ayte.es.api.v1;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("squid:S00119")
public interface DeprecatedMutation<E, ID> extends Mutation<E, ID> {
    CompletableFuture<Mutation<E, ID>> upgrade(DependencyInjector injector);
}
