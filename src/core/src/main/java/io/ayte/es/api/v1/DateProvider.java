package io.ayte.es.api.v1;

import java.time.ZonedDateTime;

public interface DateProvider {
    ZonedDateTime getCurrentTime();
}
