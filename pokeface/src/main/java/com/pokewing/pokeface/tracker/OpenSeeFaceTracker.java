package com.pokewing.pokeface.tracker;

import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.face.FaceState;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Receives OpenSeeFace UDP packets (the protocol VSeeFace/VTube-Studio-style
 * trackers speak) and turns them into a {@link FaceState}.
 *
 * <p>Packet layout, little endian, one face per packet:
 * <pre>
 *   0  double  timestamp
 *   8  int32   face id
 *  12  float   frame width
 *  16  float   frame height
 *  20  float   blink right
 *  24  float   blink left
 *  28  byte    success
 *  29  float   pnp error
 *  33  float[4] rotation quaternion
 *  49  float[3] euler angles (deg)
 *  61  float[3] translation
 *  ... landmark confidences / 2D landmarks / 3D points ...
 *  end-56: float[14] feature weights, the last two being mouth open + mouth wide
 * </pre>
 * The feature block is read relative to the packet end so trackers that ship a
 * different landmark count still parse.
 */
public final class OpenSeeFaceTracker implements TrackerSource {

    private static final int MIN_PACKET = 100;
    private static final int FEATURE_COUNT = 14;

    private final String bindAddress;
    private final int port;
    private final long stalenessMillis;

    private DatagramSocket socket;
    private Thread thread;
    private volatile boolean running;

    private final Object lock = new Object();
    private final FaceState latest = new FaceState();
    private volatile long latestAt;
    private volatile long packetCount;

    public OpenSeeFaceTracker(String bindAddress, int port, long stalenessMillis) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.stalenessMillis = stalenessMillis;
    }

    @Override
    public String name() {
        return "OpenSeeFace(" + this.bindAddress + ":" + this.port + ")";
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
            PokeFace.LOGGER.warn("PokeFace: could not bind face tracker socket on {}:{} ({}). "
                    + "Falling back to reaction/idle faces.", this.bindAddress, this.port, e.toString());
            this.socket = null;
            return;
        }
        this.running = true;
        this.thread = new Thread(this::receiveLoop, "pokeface-tracker");
        this.thread.setDaemon(true);
        this.thread.start();
        PokeFace.LOGGER.info("PokeFace: listening for face tracking data on {}:{}", this.bindAddress, this.port);
    }

    private void receiveLoop() {
        byte[] buf = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (this.running) {
            try {
                this.socket.receive(packet);
                this.packetCount++;
                if (packet.getLength() >= MIN_PACKET) {
                    decode(ByteBuffer.wrap(buf, 0, packet.getLength()).order(ByteOrder.LITTLE_ENDIAN),
                            packet.getLength());
                }
            } catch (java.net.SocketTimeoutException ignored) {
                // No tracker running right now; isLive() goes false on its own.
            } catch (Exception e) {
                if (this.running) {
                    PokeFace.LOGGER.debug("PokeFace: tracker packet dropped: {}", e.toString());
                }
            }
        }
    }

    private void decode(ByteBuffer in, int length) {
        in.getDouble();                       // timestamp
        in.getInt();                          // face id
        in.getFloat();                        // frame width
        in.getFloat();                        // frame height
        float blinkRight = in.getFloat();
        float blinkLeft = in.getFloat();
        boolean success = in.get() != 0;
        if (!success) {
            return;
        }
        in.getFloat();                        // pnp error
        in.position(in.position() + 16);      // rotation quaternion
        float pitch = in.getFloat();
        float yaw = in.getFloat();
        in.getFloat();                        // roll

        int featureOffset = length - FEATURE_COUNT * 4;
        if (featureOffset < in.position()) {
            return;
        }
        in.position(featureOffset);
        float[] features = new float[FEATURE_COUNT];
        for (int i = 0; i < FEATURE_COUNT; i++) {
            features[i] = in.getFloat();
        }
        // OpenSeeFace's feature vector, in order:
        //  0 eye_l              1 eye_r
        //  2 eyebrow_steepness_l 3 eyebrow_updown_l  4 eyebrow_quirk_l
        //  5 eyebrow_steepness_r 6 eyebrow_updown_r  7 eyebrow_quirk_r
        //  8 mouth_corner_updown_l  9 mouth_corner_inout_l
        // 10 mouth_corner_updown_r 11 mouth_corner_inout_r
        // 12 mouth_open          13 mouth_wide
        // The corner channels are updown at 8/10, NOT inout at 9/11 - reading the
        // inout pair made the mouth read as permanently pulled open.
        float eyeOpenLeft = features[0];
        float eyeOpenRight = features[1];
        float browUpDownLeft = features[3];
        float browUpDownRight = features[6];
        float mouthCornerUpDownLeft = features[8];
        float mouthCornerUpDownRight = features[10];
        float mouthOpen = features[12];
        float mouthWide = features[13];

        // The dedicated blink fields are openness in [0,1]; the eye_l/eye_r
        // features carry the same information but are the ones trackers embedded
        // in VSeeFace actually populate. Take whichever reports the eye as more
        // closed so a blink is never missed.
        float closedLeft = Math.max(1.0F - blinkLeft, 1.0F - normaliseFeature(eyeOpenLeft));
        float closedRight = Math.max(1.0F - blinkRight, 1.0F - normaliseFeature(eyeOpenRight));

        synchronized (this.lock) {
            this.latest.blinkLeft = FaceState.clamp(closedLeft, 0.0F, 1.0F);
            this.latest.blinkRight = FaceState.clamp(closedRight, 0.0F, 1.0F);
            this.latest.brow = FaceState.clamp((browUpDownLeft + browUpDownRight) * 0.5F, -1.0F, 1.0F);
            this.latest.mouthOpen = FaceState.clamp(mouthOpen, 0.0F, 1.0F);
            this.latest.mouthSmile = FaceState.clamp(
                    (mouthCornerUpDownLeft + mouthCornerUpDownRight) * 0.5F + mouthWide * 0.3F, -1.0F, 1.0F);
            this.latest.gazeX = FaceState.clamp(yaw / 30.0F, -1.0F, 1.0F);
            this.latest.gazeY = FaceState.clamp(-pitch / 30.0F, -1.0F, 1.0F);
            this.latest.intensity = Math.max(Math.abs(this.latest.brow), Math.abs(this.latest.mouthSmile));
            this.latest.expression = classify(this.latest);

            if (PokeFaceConfig.trackerDebug() && this.packetCount % 30 == 0) {
                PokeFace.LOGGER.info("PokeFace/OSF blinkL={} blinkR={} brow={} mouthOpen={} smile={} -> {}",
                        this.latest.blinkLeft, this.latest.blinkRight, this.latest.brow,
                        this.latest.mouthOpen, this.latest.mouthSmile, this.latest.expression);
            }
        }
        this.latestAt = System.currentTimeMillis();
    }

    /**
     * Feature channels are signed and loosely scaled around 0 for "neutral"; the
     * eye channels are effectively openness once shifted into [0,1].
     */
    private static float normaliseFeature(float value) {
        return FaceState.clamp((value + 1.0F) * 0.5F, 0.0F, 1.0F);
    }

    /**
     * Picks the atlas tile that best matches the captured blendshapes, so a real
     * face still lands on the same stylised tiles the fallback drivers use.
     */
    private static com.pokewing.pokeface.face.Expression classify(FaceState s) {
        if (s.mouthOpen > 0.6F && s.brow > 0.5F) {
            return com.pokewing.pokeface.face.Expression.SURPRISED;
        }
        if (s.brow < -0.45F) {
            return com.pokewing.pokeface.face.Expression.ANGRY;
        }
        if (s.mouthSmile > 0.4F) {
            return com.pokewing.pokeface.face.Expression.HAPPY;
        }
        if (s.mouthSmile < -0.4F) {
            return com.pokewing.pokeface.face.Expression.SAD;
        }
        if (s.blinkLeft > 0.7F && s.blinkRight > 0.7F) {
            return com.pokewing.pokeface.face.Expression.TIRED;
        }
        return com.pokewing.pokeface.face.Expression.NEUTRAL;
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
