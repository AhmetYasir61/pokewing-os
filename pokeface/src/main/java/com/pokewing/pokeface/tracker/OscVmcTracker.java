package com.pokewing.pokeface.tracker;

import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.face.Expression;
import com.pokewing.pokeface.face.FaceState;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Receives VMC protocol (OSC over UDP) tracking data — the format VSeeFace,
 * VNyan, Warudo and friends actually <em>send</em>.
 *
 * <p>This matters because {@link OpenSeeFaceTracker} speaks the format VSeeFace
 * <em>receives</em>: pointing VSeeFace at the game and listening for OpenSeeFace
 * packets binds a socket successfully and then never sees a single byte, which
 * looks exactly like "connected but frozen".
 *
 * <p>Only what the face needs is parsed:
 * <ul>
 *   <li>{@code /VMC/Ext/Blend/Val} — blendshape name + weight (Blink, A/I/U/E/O,
 *       Joy, Angry, Sorrow, Fun, ...)</li>
 *   <li>{@code /VMC/Ext/Blend/Apply} — commit the accumulated weights</li>
 *   <li>{@code /VMC/Ext/Bone/Pos} for the Head bone — head rotation, used as gaze</li>
 * </ul>
 * OSC bundles are unwrapped; anything else is ignored.
 */
public final class OscVmcTracker implements TrackerSource {

    private final String bindAddress;
    private final int port;
    private final long stalenessMillis;

    private DatagramSocket socket;
    private Thread thread;
    private volatile boolean running;
    private volatile long latestAt;
    private volatile long packetCount;

    private final Object lock = new Object();
    private final Map<String, Float> pending = new HashMap<>();
    private final FaceState latest = new FaceState();
    private float headYaw;
    private float headPitch;

    public OscVmcTracker(String bindAddress, int port, long stalenessMillis) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.stalenessMillis = stalenessMillis;
    }

    @Override
    public String name() {
        return "VMC/OSC(" + this.bindAddress + ":" + this.port + ")";
    }

    @Override
    public void start() {
        if (this.running) {
            return;
        }
        try {
            this.socket = new DatagramSocket(null);
            this.socket.setReuseAddress(true);
            this.socket.setSoTimeout(1000);
            this.socket.bind(new InetSocketAddress(this.bindAddress, this.port));
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not bind the VMC listener on {}:{} ({}).",
                    this.bindAddress, this.port, e.toString());
            this.socket = null;
            return;
        }
        this.running = true;
        this.thread = new Thread(this::receiveLoop, "pokeface-vmc");
        this.thread.setDaemon(true);
        this.thread.start();
        PokeFace.LOGGER.info("PokeFace: listening for VMC protocol data on {}:{}", this.bindAddress, this.port);
    }

    private void receiveLoop() {
        byte[] buf = new byte[8192];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (this.running) {
            try {
                packet.setData(buf);
                this.socket.receive(packet);
                this.packetCount++;
                handlePacket(ByteBuffer.wrap(buf, 0, packet.getLength()).order(ByteOrder.BIG_ENDIAN));
            } catch (java.net.SocketTimeoutException ignored) {
                // Nothing sending right now; isLive() reports that on its own.
            } catch (Exception e) {
                if (this.running) {
                    PokeFace.LOGGER.debug("PokeFace: VMC packet dropped: {}", e.toString());
                }
            }
        }
    }

    private void handlePacket(ByteBuffer in) {
        if (in.remaining() < 4) {
            return;
        }
        in.mark();
        String first = readString(in);
        if ("#bundle".equals(first)) {
            in.getLong();                       // time tag
            while (in.remaining() >= 4) {
                int size = in.getInt();
                if (size <= 0 || size > in.remaining()) {
                    return;
                }
                ByteBuffer element = in.slice().order(ByteOrder.BIG_ENDIAN);
                element.limit(size);
                handlePacket(element);
                in.position(in.position() + size);
            }
            return;
        }
        in.reset();
        handleMessage(in);
    }

    private void handleMessage(ByteBuffer in) {
        String address = readString(in);
        String types = readString(in);
        if (address == null || types == null || !types.startsWith(",")) {
            return;
        }
        switch (address) {
            case "/VMC/Ext/Blend/Val" -> {
                String name = readArgString(in, types, 0);
                Float value = readArgFloat(in, types, name);
                if (name != null && value != null) {
                    synchronized (this.lock) {
                        this.pending.put(name.toLowerCase(java.util.Locale.ROOT), value);
                    }
                }
            }
            case "/VMC/Ext/Blend/Apply" -> apply();
            case "/VMC/Ext/Bone/Pos" -> readHeadBone(in, types);
            default -> {
                // Not a message this mod needs.
            }
        }
    }

    /** The blend messages are always (string, float) in practice. */
    private String readArgString(ByteBuffer in, String types, int index) {
        return types.length() > index + 1 && types.charAt(index + 1) == 's' ? readString(in) : null;
    }

    private Float readArgFloat(ByteBuffer in, String types, String alreadyRead) {
        if (alreadyRead == null || types.length() < 3 || in.remaining() < 4) {
            return null;
        }
        char type = types.charAt(2);
        if (type == 'f') {
            return in.getFloat();
        }
        if (type == 'i') {
            return (float) in.getInt();
        }
        return null;
    }

    private void readHeadBone(ByteBuffer in, String types) {
        String bone = readArgString(in, types, 0);
        if (!"Head".equalsIgnoreCase(bone) || in.remaining() < 28) {
            return;
        }
        in.getFloat();                          // position x
        in.getFloat();                          // position y
        in.getFloat();                          // position z
        float qx = in.getFloat();
        float qy = in.getFloat();
        float qz = in.getFloat();
        float qw = in.getFloat();
        // Yaw/pitch straight off the quaternion; only the sign and rough range
        // matter here, the values feed a +-1 gaze offset.
        double yaw = Math.atan2(2.0 * (qw * qy + qx * qz), 1.0 - 2.0 * (qy * qy + qx * qx));
        double pitch = Math.asin(Math.max(-1.0, Math.min(1.0, 2.0 * (qw * qx - qz * qy))));
        this.headYaw = (float) Math.toDegrees(yaw);
        this.headPitch = (float) Math.toDegrees(pitch);
    }

    /** Folds the accumulated blendshape weights into a {@link FaceState}. */
    private void apply() {
        synchronized (this.lock) {
            float blink = weight("blink");
            float blinkL = Math.max(weight("blink_l"), weight("blinkleft"));
            float blinkR = Math.max(weight("blink_r"), weight("blinkright"));
            float mouth = Math.max(Math.max(weight("a"), weight("o")),
                    Math.max(weight("i"), Math.max(weight("u"), weight("e"))));
            float joy = Math.max(weight("joy"), weight("fun"));
            float angry = weight("angry");
            float sorrow = weight("sorrow");
            float surprised = weight("surprised");

            this.latest.blinkLeft = FaceState.clamp(Math.max(blink, blinkL), 0.0F, 1.0F);
            this.latest.blinkRight = FaceState.clamp(Math.max(blink, blinkR), 0.0F, 1.0F);
            this.latest.mouthOpen = FaceState.clamp(mouth, 0.0F, 1.0F);
            this.latest.mouthSmile = FaceState.clamp(joy - sorrow, -1.0F, 1.0F);
            this.latest.brow = FaceState.clamp(surprised + sorrow * 0.5F - angry, -1.0F, 1.0F);
            this.latest.gazeX = FaceState.clamp(this.headYaw / 30.0F, -1.0F, 1.0F);
            this.latest.gazeY = FaceState.clamp(-this.headPitch / 30.0F, -1.0F, 1.0F);
            this.latest.intensity = Math.max(Math.abs(this.latest.brow), Math.abs(this.latest.mouthSmile));
            this.latest.expression = classify(angry, joy, sorrow, surprised, this.latest);
            this.pending.clear();
        }
        this.latestAt = System.currentTimeMillis();
    }

    private float weight(String name) {
        Float v = this.pending.get(name);
        return v == null ? 0.0F : v;
    }

    private static Expression classify(float angry, float joy, float sorrow, float surprised, FaceState s) {
        if (surprised > 0.5F || (s.mouthOpen > 0.6F && s.brow > 0.4F)) {
            return Expression.SURPRISED;
        }
        if (angry > 0.45F) {
            return Expression.ANGRY;
        }
        if (joy > 0.4F) {
            return Expression.HAPPY;
        }
        if (sorrow > 0.4F) {
            return Expression.SAD;
        }
        if (s.blinkLeft > 0.7F && s.blinkRight > 0.7F) {
            return Expression.TIRED;
        }
        return Expression.NEUTRAL;
    }

    /** OSC strings are null terminated and padded to a multiple of four bytes. */
    private static String readString(ByteBuffer in) {
        int start = in.position();
        while (in.hasRemaining() && in.get() != 0) {
            // Scan to the terminator.
        }
        int end = in.position() - 1;
        if (end < start) {
            return null;
        }
        byte[] bytes = new byte[end - start];
        int mark = in.position();
        in.position(start);
        in.get(bytes);
        in.position(mark);
        int padded = ((end - start) / 4 + 1) * 4;
        int target = start + padded;
        in.position(Math.min(target, in.limit()));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public boolean poll(FaceState out) {
        if (!isLive()) {
            return false;
        }
        synchronized (this.lock) {
            out.set(this.latest);
        }
        return true;
    }

    @Override
    public boolean isLive() {
        return this.socket != null && System.currentTimeMillis() - this.latestAt < this.stalenessMillis;
    }

    /** Packets seen since start; 0 while bound means "nothing is sending here". */
    public long packetCount() {
        return this.packetCount;
    }

    public boolean isBound() {
        return this.socket != null;
    }

    @Override
    public void close() {
        this.running = false;
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
    }
}
