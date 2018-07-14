package io.ayte.es.api.internal;

import io.ayte.es.api.v1.EntityFactory;
import io.ayte.es.api.v1.EntityType;
import io.ayte.es.api.v1.IdentifierConverter;
import io.ayte.es.api.v1.Mutation;
import io.ayte.es.api.v1.MutationType;

import java.util.List;

@SuppressWarnings("squid:S00119")
public interface EntityDescriptor<E, ID> {
    EntityType<E> getType();
    IdentifierConverter<ID> getIdentifierConverter();
    List<MutationType<?, E, ID>> getMutationTypes();
    <M extends Mutation<E, ID>> MutationType<M, E, ID> getMutationType(Class<M> mutationClass);
    <M extends Mutation<E, ID>> MutationType<M, E, ID> getMutationType(String mutationType, int version);
    EntityFactory<E> getFactory();
    EntityConfiguration getConfiguration();
}
