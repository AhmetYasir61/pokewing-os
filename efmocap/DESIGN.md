# EFMocap — Design & Roadmap

An **Epic Fight-native machinima toolkit**. Instead of exporting animations to
another mod (and fighting bone-retargeting / format issues), EFMocap records
your Epic Fight performance and replays it on **clone actors rendered by Epic
Fight itself** — so the animation is always 100% correct.

## Why this architecture

Epic Fight renders each patched entity from its own `Animator` + `Armature`.
If a clone entity is Epic-Fight-patched and we drive it to play the same
animations (or the same pose) at the same times, it renders identically to the
original — **no retargeting, no `.animation.json`, no BBS**. Everything can run
**client-side** for single-player filming (no server mod, no networking).

## Verified Epic Fight API (from epicfight 20.14.17, 1.20.1)

Reflection is used so EFMocap compiles/loads without the Epic Fight jar.

Playback (drive a clone):
- `AnimationManager.byId(int)` (static) -> `AnimationAccessor`
- `LivingEntityPatch.playAnimationInClientSide(AssetAccessor, float transition)`
- `LivingEntityPatch.playAnimation(AssetAccessor, float)`

Read the performer's state (record):
- `EpicFightCapabilities.getEntityPatch(Entity, Class)` -> patch
- `patch.getClientAnimator()` / `getAnimator()`
- `Animator.getPlayer(accessor)` -> `Optional<AnimationPlayer>`;
  `AnimationPlayer.getAnimation()` -> accessor, `.getElapsedTime()`, `.isEnd()`
- `ClientAnimator.currentMotion()` -> `LivingMotion` (walk/idle/etc.)
- Pose fallback: `Animator.getPose(float)` -> `Pose.getJointTransformData()`
  (map of joint -> `JointTransform{translation, rotation, scale}`)
- `AnimationAccessor.id()` -> int (record the id, replay via `byId`)

## Two possible replay fidelities

1. **Action-level** (preferred): record the sequence of animation ids + start
   times + movement; replay by calling `playAnimationInClientSide(byId(id), t)`
   on the clone. Epic Fight blends/plays natively -> perfect look, tiny data.
2. **Pose-level** (fallback): record the composed pose every tick and force it
   onto the clone each frame. Exact, but needs a render/animator hook.

Phase 1 targets action-level for the body plus raw position/rotation replay.

## Roadmap (each phase independently testable)

- **Phase 1 — Core mocap + clone.** Record self (position, body/head rotation,
  current animation id + elapsed, held items) each tick. Spawn one client-side
  clone that replays it. Keybind/commands: `record`, `stop`, `spawn clone`,
  `clear`.
- **Phase 2 — Layered scenes.** Multiple named recordings + multiple clones
  playing together; start/sync offsets so a fight choreographs across takes.
- **Phase 3 — Cinematic camera.** Free-fly + keyframed camera paths (position,
  look, FOV, roll) with smooth interpolation; play the scene through it.
- **Phase 4 — MP4 export.** Capture frames during playback and pipe to `ffmpeg`
  (if installed) to produce an .mp4; configurable fps/resolution.
- **Phase 5 — Timeline/editing UI.** Trim, retime, offset clips; per-actor
  visibility; camera keyframe editing.

## Open engineering questions (to resolve during Phase 1)

- **Clone entity type.** Simplest high-fidelity option: a client-only fake
  player (`AbstractClientPlayer`/`RemotePlayer`) that Epic Fight's client
  renderer patches. Needs a valid `GameProfile` + skin.
- **Reading the "current" animation id.** `Animator` exposes `getPlayer(accessor)`
  but not a no-arg "what is playing". Options: track Epic Fight's animation-play
  events/`AnimatorControlPacket`, or scan the animator's active layers via
  reflection. Fallback to pose-level if needed.

## Controls

| Key | Action |
|-----|--------|
| `K` | Start / stop recording (auto-saves as `takeN`) |
| `N` | Stage the scene — every saved take plays at once, looping |
| `J` | Clear all clones |

Commands: `/efmocap rec start|stop`, `scene`, `restart`, `list`,
`play <take>`, `delete <take>`, `clear`.

**Workflow for a fight scene:** record yourself as fighter A (`K`…`K`), then
record fighter B reacting (`K`…`K`), then press `N` — both clones play together,
looping, so you can keep adding layers and shoot the result.

## BBS integration

BBS is the natural camera operator and character builder, so EFMocap defers to
it rather than competing with it. While a BBS film is playing, that film owns
the timeline: our actors are posed from its playhead each tick, so they perform
inside its shot and BBS's camera, preview and export see them like anything
else in the scene. Our own camera only drives playback when no film is running.

The bridge is reflection-only (`bbs/BBSBridge`): BBS ships as a Fabric mod
reaching Forge through Sinytra Connector, and EFMocap has to keep working with
BBS absent. Entry points used: `BBSModClient.getFilms()` for the controller map,
then the controller's `getTick()`, `duration`, `paused` and `hasFinished()`.

BBS is also the character builder. Export a form from BBS's own editor into
`config/efmocap/forms` and an attachment can use it instead of an `.obj`: BBS
draws the part with its own renderer while Epic Fight keeps animating the bone
it hangs from, so its whole model/texture/body-part system is available without
rebuilding any of it here. Entry points: `DataToString.mapFromString` ->
`FormUtils.fromData` for loading, then `MCEntity` + `FormRenderingContext` ->
`FormUtilsClient.render` for drawing.

The traffic runs both ways: EFMocap also hands BBS a **form type of its own**,
the way Emoticons does, so an attachment `.obj` can be picked, placed and
animated from inside BBS's editor and films. That half can't be reflection —
`FormUtilsClient.register(Class, IFormRendererFactory)` requires subclassing
`Form` — so it lives in the optional `src/bbsapi` source set, compiled only when
a `bbs*.jar` is present in `efmocap/libs/`. `BBSForms.registerFormType()` looks
its entry point up by name at client setup, so with no jar at build time the
class simply isn't there and the registration is skipped; the rest of the mod,
and the build, are unaffected either way.

## Attachments

Cosmetic parts are Wavefront `.obj` files dropped into
`config/efmocap/attachments` — model them in Blockbench or Blender and every
file in that folder appears in the character editor. Each attachment picks a
model, an optional PNG, an Epic Fight bone to ride on, and an offset/rotation/
scale. They're drawn in world space after entities, because Epic Fight replaces
player rendering with its own; the bone transform comes from
`Armature.getBoundTransformFor(pose, joint)` and is rotated out of Epic Fight's
Z-up rig space into Minecraft's Y-up.

## Status

- **Phase 1 — done (in testing).** Recorder + client clone replay working:
  clones spawn, move, hold the recorded equipment, and are force-synced each
  tick to the recorded Epic Fight animation id + elapsed time.
- **Phase 2 — in progress.** Named takes persisted to
  `config/efmocap/takes/*.json`, multi-clone scene staging, per-take playback.
- Phases 3–5 (cinematic camera, ffmpeg MP4 export, timeline UI) next.

The old `epicfight-bbs-bridge` mod has been folded in: its Epic Fight animation
exporter now lives under `bbs/` and is reachable as `/efmocap bbsexport`, so
there is a single mod and a single jar to install.
