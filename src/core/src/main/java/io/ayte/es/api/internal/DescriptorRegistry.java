package io.ayte.es.api.internal;

@SuppressWarnings("squid:S00119")
public interface DescriptorRegistry {
    <E, ID> EntityDescriptor<E, ID> getDescriptor(Class<E> entityClass);
    <E, ID> EntityDescriptor<E, ID> getDescriptor(String entityType);
}
