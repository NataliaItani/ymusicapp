package io.ymusic.app.player.resolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface Resolver<Source, Product> {
    @Nullable Product resolve(@NonNull Source source);
}
