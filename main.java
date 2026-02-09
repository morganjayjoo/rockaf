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
    public static final String BACKLINE_HEX = "0x2C6e8A0b4D2f6a8C0e2B4d6F8a0C2e4B6d8F0";
    public static final String FOH_HEX = "0x5A7c9E1f3B5d7F9a1C3e5B7d9F1a3C5e7B9d1";
    public static final String CROWD_ORACLE_HEX = "0xB1d3F5a7C9e2B4d6F8a0C2e4B6d8F0a2C4e6";
    public static final String RIG_VAULT_HEX = "0x7E0a2C4e6B8d0F2a4C6e8B0d2F4a6C8e0B2d4";

    public static final int MAX_RIFF_SLOTS = 256;
    public static final int MAX_SETLIST_TRACKS = 48;
    public static final int CROWD_CAPACITY = 50000;
    public static final int AMP_STACK_CHANNELS = 4;
    public static final int DRUM_KIT_PADS = 12;
    public static final int BASS_STRING_COUNT = 4;
    public static final int FEEDBACK_BUFFER_SAMPLES = 8192;
    public static final double DEFAULT_TEMPO_BPM = 118.0;
    public static final int VENUE_ZONES = 8;
    public static final long SET_CHANGE_COOLDOWN_MS = 120_000L;
    public static final byte RIFF_VERSION = 0x52;
    public static final String DOMAIN_TAG = "rockaf-sim-v26";
    public static final long GENESIS_EPOCH_MS = 1998473629157L;
    public static final int MAX_ENCORE_COUNT = 3;
    public static final double MIN_DISTORTION_GAIN = 0.2;
    public static final double MAX_DISTORTION_GAIN = 2.8;
}

// ─── RockafRuntimeException ──────────────────────────────────────────────────

final class RockafRuntimeException extends RuntimeException {
