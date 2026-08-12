package com.pokewing.pokeface.face;

/**
 * Discrete face tiles. The ordinal is the column in the expression atlas
 * ({@code assets/pokeface/textures/face/expressions.png}), so the order here and
 * the order in the texture must stay in sync.
 */
public enum Expression {
    NEUTRAL("neutral"),
    HAPPY("happy"),
    ANGRY("angry"),
    HURT("hurt"),
    SURPRISED("surprised"),
    FOCUSED("focused"),
    TIRED("tired"),
    SAD("sad");

    private final String key;

    Expression(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public String translationKey() {
        return "pokeface.expression." + this.key;
    }

    /** Fills a {@link FaceState} with this expression's canonical pose. */
    public FaceState pose(float intensity) {
        FaceState s = new FaceState();
        s.expression = this;
        s.intensity = FaceState.clamp(intensity, 0.0F, 1.0F);
        switch (this) {
            case HAPPY -> {
                s.mouthSmile = 0.9F * intensity;
                s.brow = 0.3F * intensity;
                s.blinkLeft = s.blinkRight = 0.35F * intensity;
            }
            case ANGRY -> {
                s.brow = -1.0F * intensity;
                s.mouthSmile = -0.5F * intensity;
                s.mouthOpen = 0.3F * intensity;
            }
            case HURT -> {
                s.brow = -0.7F * intensity;
                s.mouthOpen = 0.8F * intensity;
                s.blinkLeft = s.blinkRight = 0.7F * intensity;
            }
            case SURPRISED -> {
                s.brow = 1.0F * intensity;
                s.mouthOpen = 0.9F * intensity;
            }
            case FOCUSED -> {
                s.brow = -0.4F * intensity;
                s.blinkLeft = s.blinkRight = 0.25F * intensity;
            }
            case TIRED -> {
                s.blinkLeft = s.blinkRight = 0.6F * intensity;
                s.mouthSmile = -0.2F * intensity;
                s.gazeY = -0.3F * intensity;
            }
            case SAD -> {
                s.brow = 0.5F * intensity;
                s.mouthSmile = -0.8F * intensity;
                s.gazeY = -0.4F * intensity;
            }
            default -> {
            }
        }
        return s;
    }

    public static Expression byName(String name) {
        for (Expression e : values()) {
            if (e.key.equalsIgnoreCase(name)) {
                return e;
            }
        }
        return NEUTRAL;
    }
}
