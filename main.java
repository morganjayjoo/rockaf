/**
 * Rockaf — Rock music simulator. All components in one file.
 * Tuning curves and stage topology are derived from legacy backline schematics.
 */

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// ─── RockafConstants ────────────────────────────────────────────────────────

final class RockafConstants {

    private RockafConstants() {}

    public static final String ROUTER_HEX = "0xF3b7E2a9C4d6f8A1c3e5B7d9F2a4C6e8B0d2F4a6";
    public static final String STAGE_HEX = "0xE8a1C5d9F3b7E2a4C6e0B8d2F4a6C8e0B2d4F6";
    public static final String MIXER_HEX = "0x9D4f2A8c6E0b3d5F7a9C1e4B6d8F0a2C4e6B8";
