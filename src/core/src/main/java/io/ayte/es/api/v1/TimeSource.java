package io.ayte.es.api.v1;

import java.time.ZonedDateTime;

@SuppressWarnings({"squid:S1214"})
public interface TimeSource {
    TimeSource DEFAULT = ZonedDateTime::now;

    ZonedDateTime getCurrentTime();
}
