package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@SuppressWarnings("squid:S00119")
public class MutationType<M extends Mutation<E, ID>, E, ID> {
    private final Class<M> symbol;
    private final String type;
    private final int version;

    public static <E, ID> boolean equalSymbols(MutationType<?, E, ID> left, MutationType<?, E, ID> right) {
        return left.getSymbol().equals(right.getSymbol());
    }

    public static <E, ID> boolean equalStoragePrint(MutationType<?, E, ID> left, MutationType<?, E, ID> right) {
        return left.getVersion() == right.getVersion() && left.getType().equals(right.getType());
    }
}
