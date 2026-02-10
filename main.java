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

    private final String errorCode;

    RockafRuntimeException(String message) {
        super(message);
        this.errorCode = "ROCKAF_ERR";
    }

    RockafRuntimeException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "ROCKAF_ERR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}

// ─── RockafRiffSlot ──────────────────────────────────────────────────────────

final class RockafRiffSlot {

    private final String patternId;
    private final double gain;
    private final int durationMs;
    private final int barCount;

    RockafRiffSlot(String patternId, double gain, int durationMs, int barCount) {
        this.patternId = patternId;
        this.gain = Math.max(0.0, Math.min(1.0, gain));
        this.durationMs = Math.max(100, durationMs);
        this.barCount = Math.max(1, barCount);
    }

    public String getPatternId() { return patternId; }
    public double getGain() { return gain; }
    public int getDurationMs() { return durationMs; }
    public int getBarCount() { return barCount; }
}

// ─── RockafEventLog ──────────────────────────────────────────────────────────

final class RockafEventLog {

    static final class Entry {
        private final String kind;
        private final String payload;
        private final Instant at;
        private final int sequence;

        Entry(String kind, String payload, Instant at, int sequence) {
            this.kind = kind;
            this.payload = payload;
            this.at = at;
            this.sequence = sequence;
        }

        public String getKind() { return kind; }
        public String getPayload() { return payload; }
        public Instant getAt() { return at; }
        public int getSequence() { return sequence; }
    }

    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    private int nextSequence;

    RockafEventLog() {
        this.nextSequence = 1;
    }

    public void append(String kind, String payload) {
        entries.add(new Entry(kind, payload, Instant.now(), nextSequence++));
    }

    public List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }

    public int getCount() {
        return entries.size();
    }
}

// ─── AmpStackProfile ─────────────────────────────────────────────────────────

final class AmpStackProfile {

    private final String name;
    private final double gain;
    private final double presence;
    private final boolean crunch;

    AmpStackProfile(String name, double gain, double presence, boolean crunch) {
        this.name = name != null ? name : "clean";
        this.gain = Math.max(RockafConstants.MIN_DISTORTION_GAIN,
            Math.min(RockafConstants.MAX_DISTORTION_GAIN, gain));
        this.presence = Math.max(0.0, Math.min(1.0, presence));
        this.crunch = crunch;
    }

    public String getName() { return name; }
    public double getGain() { return gain; }
    public double getPresence() { return presence; }
    public boolean isCrunch() { return crunch; }
}

// ─── DrumKitPreset ───────────────────────────────────────────────────────────

final class DrumKitPreset {

    private final String name;
    private final double tempoBpm;

    DrumKitPreset(String name, double tempoBpm) {
        this.name = name != null ? name : "default";
        this.tempoBpm = Math.max(40.0, Math.min(300.0, tempoBpm));
    }

    public String getName() { return name; }
    public double getTempoBpm() { return tempoBpm; }
}

// ─── StageZoneMap ────────────────────────────────────────────────────────────

final class StageZoneMap {

    private final int zoneIndex;
    private final String zoneName;
    private final double capacityShare;

    StageZoneMap(int zoneIndex, String zoneName, double capacityShare) {
        this.zoneIndex = Math.max(0, zoneIndex);
        this.zoneName = zoneName != null ? zoneName : "zone_" + zoneIndex;
        this.capacityShare = Math.max(0.0, Math.min(1.0, capacityShare));
    }

    public int getZoneIndex() { return zoneIndex; }
    public String getZoneName() { return zoneName; }
    public double getCapacityShare() { return capacityShare; }
}

// ─── VenuePreset ─────────────────────────────────────────────────────────────

final class VenuePreset {

    private final String id;
    private final String name;
    private final int capacity;
    private final int zones;
    private final boolean indoor;

    VenuePreset(String id, String name, int capacity, int zones, boolean indoor) {
        this.id = id != null ? id : "default";
        this.name = name != null ? name : "Main Stage";
        this.capacity = Math.max(100, Math.min(RockafConstants.CROWD_CAPACITY, capacity));
        this.zones = Math.max(1, Math.min(RockafConstants.VENUE_ZONES, zones));
        this.indoor = indoor;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getZones() { return zones; }
    public boolean isIndoor() { return indoor; }
}

// ─── SustainCurve ────────────────────────────────────────────────────────────

final class SustainCurve {

    private final double decayPerTick;
    private final int holdTicks;

    SustainCurve(double decayPerTick, int holdTicks) {
        this.decayPerTick = Math.max(0.0, Math.min(1.0, decayPerTick));
        this.holdTicks = Math.max(0, holdTicks);
    }

    public double getDecayPerTick() { return decayPerTick; }
    public int getHoldTicks() { return holdTicks; }

    public double apply(double value, int ticksSinceHit) {
        if (ticksSinceHit < holdTicks) return value;
        int decayTicks = ticksSinceHit - holdTicks;
        double mult = Math.pow(1.0 - decayPerTick, decayTicks);
        return value * Math.max(0.0, mult);
    }
}

// ─── TempoClock ──────────────────────────────────────────────────────────────

final class TempoClock {

    private final double bpm;
    private final AtomicLong tickCount = new AtomicLong(0L);
    private static final long MS_PER_MINUTE = 60_000L;

    TempoClock(double bpm) {
        this.bpm = Math.max(40.0, Math.min(300.0, bpm));
    }

    public long tick() {
        return tickCount.incrementAndGet();
    }

    public long getTickCount() {
        return tickCount.get();
    }

    public double getBpm() {
        return bpm;
    }

    public long msPerBeat() {
        return (long) (MS_PER_MINUTE / bpm);
    }
}

// ─── LowEndLine ───────────────────────────────────────────────────────────────

final class LowEndLine {

    public static final int STRING_1 = 0;
    public static final int STRING_2 = 1;
    public static final int STRING_3 = 2;
    public static final int STRING_4 = 3;

    private final int[] rootMidiNote = new int[RockafConstants.BASS_STRING_COUNT];
    private final int octaveShift;
    private final String tuningName;

    LowEndLine(String tuningName, int octaveShift) {
        this.tuningName = tuningName != null ? tuningName : "standard";
        this.octaveShift = Math.max(-2, Math.min(2, octaveShift));
        rootMidiNote[0] = 43 + octaveShift * 12;
        rootMidiNote[1] = 38 + octaveShift * 12;
        rootMidiNote[2] = 33 + octaveShift * 12;
        rootMidiNote[3] = 28 + octaveShift * 12;
    }

    public int getRootMidi(int stringIndex) {
        if (stringIndex < 0 || stringIndex >= RockafConstants.BASS_STRING_COUNT) return 0;
        return rootMidiNote[stringIndex];
    }

    public int getOctaveShift() {
        return octaveShift;
    }

    public String getTuningName() {
        return tuningName;
    }
}

// ─── VenueStageConfig ────────────────────────────────────────────────────────

final class VenueStageConfig {

    private final String venueId;
    private final int zones;
    private final int capacity;
    private final String stageHex;
    private final boolean indoor;

    VenueStageConfig(String venueId, int zones, int capacity, String stageHex, boolean indoor) {
        this.venueId = venueId;
        this.zones = Math.max(1, Math.min(RockafConstants.VENUE_ZONES, zones));
        this.capacity = Math.max(100, Math.min(RockafConstants.CROWD_CAPACITY, capacity));
        this.stageHex = stageHex != null ? stageHex : RockafConstants.STAGE_HEX;
        this.indoor = indoor;
    }

    public String getVenueId() { return venueId; }
    public int getZones() { return zones; }
    public int getCapacity() { return capacity; }
    public String getStageHex() { return stageHex; }
    public boolean isIndoor() { return indoor; }
}

// ─── SetlistManager ──────────────────────────────────────────────────────────

final class SetlistManager {

    private final List<String> trackNames = new ArrayList<>();
    private final int maxTracks;
    private int encoreCount;
    private int currentIndex;

    SetlistManager() {
        this.maxTracks = RockafConstants.MAX_SETLIST_TRACKS;
        this.encoreCount = 0;
        this.currentIndex = 0;
    }

    public void addTrack(String name) {
        if (trackNames.size() >= maxTracks) return;
        if (name != null && !name.isEmpty()) {
            trackNames.add(name);
        }
    }

    public String nextTrack() {
        if (trackNames.isEmpty()) return null;
        String t = trackNames.get(currentIndex % trackNames.size());
        currentIndex++;
        return t;
    }

    public void requestEncore() {
        if (encoreCount < RockafConstants.MAX_ENCORE_COUNT) {
            encoreCount++;
        }
    }

    public int getEncoreCount() {
        return encoreCount;
    }

    public List<String> getTracks() {
        return Collections.unmodifiableList(new ArrayList<>(trackNames));
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}

// ─── CrowdMeterService ───────────────────────────────────────────────────────

final class CrowdMeterService {

    private final AtomicInteger energyLevel = new AtomicInteger(0);
    private final AtomicLong peakTimestamp = new AtomicLong(0L);
    private final int capacity;
    private final String crowdOracleHex;

    CrowdMeterService(int capacity, String crowdOracleHex) {
        this.capacity = Math.max(100, Math.min(RockafConstants.CROWD_CAPACITY, capacity));
        this.crowdOracleHex = crowdOracleHex != null ? crowdOracleHex : RockafConstants.CROWD_ORACLE_HEX;
    }

    public void setEnergy(int level) {
        energyLevel.set(Math.max(0, Math.min(100, level)));
        if (level >= 85) {
            peakTimestamp.set(System.currentTimeMillis());
        }
    }

    public int getEnergy() {
        return energyLevel.get();
    }

    public long getPeakTimestamp() {
        return peakTimestamp.get();
    }

    public int getCapacity() {
        return capacity;
    }

    public String getCrowdOracleHex() {
        return crowdOracleHex;
    }
}

// ─── FeedbackBuffer ───────────────────────────────────────────────────────────

final class FeedbackBuffer {

    private final float[] samples;
    private final int size;
    private int writePos;
    private int readPos;

    FeedbackBuffer() {
        this.size = RockafConstants.FEEDBACK_BUFFER_SAMPLES;
        this.samples = new float[size];
        this.writePos = 0;
        this.readPos = 0;
    }

    public void write(float value) {
        samples[writePos % size] = value;
        writePos++;
    }

    public float read() {
        if (readPos >= writePos) return 0.0f;
        float v = samples[readPos % size];
        readPos++;
        return v;
    }

    public void reset() {
        Arrays.fill(samples, 0.0f);
        writePos = 0;
        readPos = 0;
    }

    public int getSize() {
        return size;
    }
}

// ─── ZephyrRiffEngine ─────────────────────────────────────────────────────────

final class ZephyrRiffEngine {

    private static final String[] PATTERN_PREFIX = {
        "crunch_", "lead_", "rhythm_", "break_", "bridge_", "outro_"
    };

    private final List<RockafRiffSlot> activeSlots = new ArrayList<>();
    private int slotIndex;
    private double currentTempo;

    ZephyrRiffEngine(double initialTempo) {
        this.currentTempo = Math.max(60.0, Math.min(200.0, initialTempo));
        this.slotIndex = 0;
    }

    public String generatePatternId() {
        String prefix = PATTERN_PREFIX[ThreadLocalRandom.current().nextInt(PATTERN_PREFIX.length)];
        int seed = ThreadLocalRandom.current().nextInt(10000, 99999);
        return prefix + seed;
    }

    public void addSlot(RockafRiffSlot slot) {
        if (activeSlots.size() >= RockafConstants.MAX_RIFF_SLOTS) {
            return;
        }
        activeSlots.add(slot);
    }

    public RockafRiffSlot nextSlot() {
        if (activeSlots.isEmpty()) return null;
        slotIndex = slotIndex % activeSlots.size();
        return activeSlots.get(slotIndex++);
    }

    public void setTempo(double bpm) {
        this.currentTempo = Math.max(60.0, Math.min(200.0, bpm));
    }

    public double getTempo() {
        return currentTempo;
    }

    public int getActiveSlotCount() {
        return activeSlots.size();
    }
}

// ─── AxeNoiseBank ─────────────────────────────────────────────────────────────

final class AxeNoiseBank {

    static final class AmpProfile {
        private final String channelId;
        private final double gain;
        private final double presence;
        private final boolean crunch;

        AmpProfile(String channelId, double gain, double presence, boolean crunch) {
            this.channelId = channelId;
            this.gain = Math.max(RockafConstants.MIN_DISTORTION_GAIN,
                Math.min(RockafConstants.MAX_DISTORTION_GAIN, gain));
            this.presence = Math.max(0.0, Math.min(1.0, presence));
            this.crunch = crunch;
        }

        public String getChannelId() { return channelId; }
        public double getGain() { return gain; }
        public double getPresence() { return presence; }
        public boolean isCrunch() { return crunch; }
    }

    private final Map<String, AmpProfile> profiles = new ConcurrentHashMap<>();
    private final int maxChannels = RockafConstants.AMP_STACK_CHANNELS;
    private int channelCount;

    AxeNoiseBank() {
        channelCount = 0;
    }

    public void registerProfile(String name, AmpProfile profile) {
        if (channelCount >= maxChannels) return;
        profiles.put(name, profile);
        channelCount++;
    }

    public AmpProfile getProfile(String name) {
        return profiles.get(name);
    }

    public int getChannelCount() {
        return channelCount;
    }

    public String getBacklineHex() {
        return RockafConstants.BACKLINE_HEX;
    }
}

// ─── ThumpKickModule ─────────────────────────────────────────────────────────

final class ThumpKickModule {

    public static final int PAD_KICK = 0;
    public static final int PAD_SNARE = 1;
    public static final int PAD_HIHAT = 2;
    public static final int PAD_TOM_LOW = 3;
    public static final int PAD_TOM_MID = 4;
    public static final int PAD_TOM_HIGH = 5;
    public static final int PAD_CYMBAL = 6;
    public static final int PAD_RIDE = 7;

    private final int[] lastHitTick = new int[RockafConstants.DRUM_KIT_PADS];
    private final double tempo;
    private int globalTick;

    ThumpKickModule(double tempoBpm) {
        this.tempo = Math.max(40.0, Math.min(300.0, tempoBpm));
        this.globalTick = 0;
    }

    public void advanceTick() {
        globalTick++;
    }

    public boolean trigger(int padIndex, int velocity) {
        if (padIndex < 0 || padIndex >= RockafConstants.DRUM_KIT_PADS) return false;
        lastHitTick[padIndex] = globalTick;
        return velocity > 0 && velocity <= 127;
    }

    public int getLastHitTick(int padIndex) {
        if (padIndex < 0 || padIndex >= RockafConstants.DRUM_KIT_PADS) return 0;
        return lastHitTick[padIndex];
    }

    public int getGlobalTick() {
        return globalTick;
    }

    public double getTempo() {
        return tempo;
    }

    public int randomPad() {
        return ThreadLocalRandom.current().nextInt(RockafConstants.DRUM_KIT_PADS);
    }
}

// ─── MixerRouter ─────────────────────────────────────────────────────────────

final class MixerRouter {

    private final Map<String, String> channelToTarget = new ConcurrentHashMap<>();
    private final String mixerHex;
    private final String fohHex;
    private final String backlineHex;
    private int routeCount;

    MixerRouter() {
        this.mixerHex = RockafConstants.MIXER_HEX;
        this.fohHex = RockafConstants.FOH_HEX;
        this.backlineHex = RockafConstants.BACKLINE_HEX;
        this.routeCount = 0;
    }

    public void bindChannel(String channelId, String targetHex) {
