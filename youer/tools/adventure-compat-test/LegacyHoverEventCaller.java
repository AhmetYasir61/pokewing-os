import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer;

/**
 * Stands in for a plugin built against Adventure 4.x — ItemsAdder 4.0.16 does exactly this in
 * {@code itemsadder.m.aun.<clinit>}, which is where it throws on an Adventure 5 server:
 *
 * <pre>
 * java.lang.NoSuchMethodError: 'net.kyori.adventure.text.serializer.gson.GsonComponentSerializer$Builder
 *   net.kyori.adventure.text.serializer.gson.GsonComponentSerializer$Builder.legacyHoverEventSerializer(
 *     net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer)'
 * </pre>
 *
 * <p>Compiled against Adventure 4.17.0 so the call site carries the gson-package descriptor, then run
 * against the patched Adventure 5 serializer. See {@code check.sh}.
 */
public final class LegacyHoverEventCaller {
    public static void main(String[] args) {
        LegacyHoverEventSerializer legacy = null;
        GsonComponentSerializer serializer = GsonComponentSerializer.builder()
                .legacyHoverEventSerializer(legacy)
                .build();

        String json = serializer.serialize(Component.text("hello"));
        if (!json.contains("hello")) {
            throw new AssertionError("serializer produced unexpected output: " + json);
        }
        System.out.println("OK: builder accepted the gson LegacyHoverEventSerializer, serialize() -> " + json);
    }
}
