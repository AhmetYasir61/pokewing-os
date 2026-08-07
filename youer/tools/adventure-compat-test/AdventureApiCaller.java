import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.json.JSONOptions;

/**
 * Stands in for a plugin built against Adventure 4.x. ItemsAdder 4.0.17 hits both of these:
 *
 * <ul>
 *   <li>{@code JSONOptions.HoverEventValueMode.MODERN_ONLY} — renamed to {@code CAMEL_CASE} in
 *       Adventure 5, so the read fails with
 *       {@code NoSuchFieldError: ...HoverEventValueMode does not have member field 'MODERN_ONLY'}
 *       (thrown from {@code itemsadder.m.axy.<clinit>}).</li>
 *   <li>{@code ClickEvent.Action.RUN_COMMAND} and {@code ClickEvent.clickEvent(Action, String)} —
 *       Adventure 5 turned {@code Action} into a generic class hierarchy, so the constants are typed
 *       as their own subclasses and the factory takes a {@code Payload}. Field and method resolution
 *       both key on the descriptor, so 4.x call sites no longer link.</li>
 * </ul>
 *
 * <p>Compiled against Adventure 4.17.0 so the call sites carry the 4.x descriptors, then run against
 * the patched Adventure 5 artifacts. See {@code check.sh}.
 */
public final class AdventureApiCaller {
    public static void main(String[] args) {
        JSONOptions.HoverEventValueMode modern = JSONOptions.HoverEventValueMode.MODERN_ONLY;
        JSONOptions.HoverEventValueMode legacy = JSONOptions.HoverEventValueMode.LEGACY_ONLY;
        JSONOptions.HoverEventValueMode both = JSONOptions.HoverEventValueMode.BOTH;
        if (modern == null || legacy == null || both == null) {
            throw new AssertionError("hover event value mode aliases resolved to null");
        }
        System.out.println("OK: HoverEventValueMode MODERN_ONLY=" + modern + " LEGACY_ONLY=" + legacy + " BOTH=" + both);

        ClickEvent.Action run = ClickEvent.Action.RUN_COMMAND;
        ClickEvent.Action url = ClickEvent.Action.OPEN_URL;
        if (run == null || url == null) {
            throw new AssertionError("click event action aliases resolved to null");
        }
        ClickEvent event = ClickEvent.clickEvent(run, "/help");
        if (event == null) {
            throw new AssertionError("clickEvent(Action, String) returned null");
        }
        System.out.println("OK: ClickEvent RUN_COMMAND=" + run + " OPEN_URL=" + url + " -> " + event);
    }
}
