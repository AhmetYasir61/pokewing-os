#!/usr/bin/env python3
"""Generate the EFMocap actor model that BBS loads.

BBS reads its models straight out of jar resources under
``assets/bbs/assets/models/<category>/<model>/`` — that is why the emoticons
models still render with their ``config/`` texture folders empty. So EFMocap
ships its own model the same way, and this script authors it.

The rig deliberately mirrors BBS's own player bone names (anchor, low_body,
body, torso, head, left_arm/right_arm, left_leg/right_leg), so the Epic Fight
retarget map already in RetargetConfig lands on it unchanged. On top of that
each limb is split into two segments with a child bone, so Epic Fight's elbow
and knee rotations have somewhere to go instead of being folded into the
shoulder and hip.

Run from the efmocap directory:  python3 tools/gen_actor_model.py
"""

import json
import os

TEX_W = TEX_H = 64

OUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "src", "main", "resources",
    "assets", "bbs", "assets", "models", "efmocap", "actor",
)


def box_uvs(u, v, w, h, d, y_from=None, y_to=None):
    """Standard Minecraft box UV layout, in y-down texture pixels.

    ``y_from``/``y_to`` cut the side faces vertically, measured in pixels from
    the top of the box, so a 12-tall limb can be emitted as two 6-tall halves
    off the one skin region. The cap faces stay as they are: the seam they
    cover sits inside the joint, where nothing sees it.
    """
    y0 = v + d + (0 if y_from is None else y_from)
    y1 = v + d + (h if y_to is None else y_to)
    return {
        # x1, y1, x2, y2
        "right": [u, y0, u + d, y1],
        "front": [u + d, y0, u + d + w, y1],
        "left": [u + d + w, y0, u + d + w + d, y1],
        "back": [u + d + w + d, y0, u + 2 * d + 2 * w, y1],
        # caps are written with their axes reversed, as BBS's own models do
        "top": [u + d + w, v + d, u + d, v],
        "bottom": [u + d + 2 * w, v, u + d + w, v + d],
    }


def cube(frm, size, uvs, origin, offset=None):
    c = {"origin": origin, "from": frm, "size": size, "uvs": uvs}
    if offset is not None:
        c["offset"] = offset
    return c


def limb(groups, name, child, parent, origin, x, y_top, z, w, h, d, uv, uv_over=None):
    """A two-segment limb: ``name`` is the upper half, ``child`` the lower.

    Splitting at the halfway point puts a joint where the elbow or knee is, and
    keeps the upper segment under the name BBS's player rig uses so existing
    retargets keep working.
    """
    half = h // 2
    mid = y_top - half

    upper = [cube([x, mid, z], [w, half, d],
                  box_uvs(*uv, w, h, d, 0, half), origin)]
    lower_origin = [origin[0], mid, origin[2]]
    lower = [cube([x, y_top - h, z], [w, half, d],
                  box_uvs(*uv, w, h, d, half, h), lower_origin)]

    if uv_over is not None:
        upper.append(cube([x, mid, z], [w, half, d],
                          box_uvs(*uv_over, w, h, d, 0, half), origin, 0.25))
        lower.append(cube([x, y_top - h, z], [w, half, d],
                          box_uvs(*uv_over, w, h, d, half, h), lower_origin, 0.25))

    groups[name] = {"origin": origin, "parent": parent, "cubes": upper}
    groups[child] = {"origin": lower_origin, "parent": name, "cubes": lower}
    return lower_origin


def build():
    g = {}

    g["anchor"] = {"origin": [0, 16, 0], "parent": None}
    g["low_body"] = {"origin": [0, 12, 0], "parent": "anchor"}
    g["body"] = {"origin": [0, 24, 0], "parent": "low_body"}

    g["head"] = {
        "origin": [0, 24, 0], "parent": "low_body",
        "cubes": [cube([-4, 24, -4], [8, 8, 8], box_uvs(0, 0, 8, 8, 8), [0, 24, 0])],
    }
    g["headwear"] = {
        "origin": [0, 28, 0], "parent": "head",
        "cubes": [cube([-4, 24, -4], [8, 8, 8], box_uvs(32, 0, 8, 8, 8),
                       [0, 28, 0], 0.5)],
    }

    g["torso"] = {
        "origin": [0, 24, 0], "parent": "body",
        "cubes": [
            cube([-4, 12, -2], [8, 12, 4], box_uvs(16, 16, 8, 12, 4), [0, 24, 0]),
            cube([-4, 12, -2], [8, 12, 4], box_uvs(16, 32, 8, 12, 4), [0, 24, 0], 0.25),
        ],
    }

    # Classic 4-wide arms, split at the elbow.
    limb(g, "right_arm", "right_forearm", "body", [6, 22, 0],
         4, 24, -2, 4, 12, 4, (40, 16), (40, 32))
    limb(g, "left_arm", "left_forearm", "body", [-6, 22, 0],
         -8, 24, -2, 4, 12, 4, (32, 48), (48, 48))

    # Legs hang off the anchor, as in BBS's player rig, and split at the knee.
    limb(g, "right_leg", "right_shin", "anchor", [2, 12, 0],
         0, 12, -2, 4, 12, 4, (0, 16), (0, 32))
    limb(g, "left_leg", "left_shin", "anchor", [-2, 12, 0],
         -4, 12, -2, 4, 12, 4, (16, 48), (0, 48))

    # Empty tips, so Epic Fight's Hand_* / foot joints have a target and
    # attachments can hang off them.
    g["right_hand"] = {"origin": [6, 12, 0], "parent": "right_forearm"}
    g["left_hand"] = {"origin": [-6, 12, 0], "parent": "left_forearm"}
    g["right_foot"] = {"origin": [2, 0, 0], "parent": "right_shin"}
    g["left_foot"] = {"origin": [-2, 0, 0], "parent": "left_shin"}

    # Item anchors, so held weapons land in the hand during a fight scene.
    g["right_arm_item"] = {"origin": [5.5, 12, 0], "parent": "right_forearm"}
    g["left_arm_item"] = {"origin": [-5.5, 12, 0], "parent": "left_forearm"}

    return {"version": "0.7.2", "animations": {},
            "model": {"texture": [TEX_W, TEX_H], "groups": g}}


CONFIG = {
    "texture": "assets:models/efmocap/actor/actor.png",
    "pose_group": "player/steve",
    # Off on purpose: the actor is driven by recorded Epic Fight animation, and
    # BBS's procedural walk/idle would fight it.
    "procedural": False,
    "culling": False,
    "items_main": ["right_arm_item"],
    "items_off": ["left_arm_item"],
    "scale": [0.9375, 0.9375, 0.9375],
    "look_at": {"head": "head", "pitch": True, "head_limit": 45},
}


def main():
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "actor.bbs.json"), "w") as f:
        json.dump(build(), f, indent=1)
        f.write("\n")
    with open(os.path.join(OUT, "config.json"), "w") as f:
        json.dump(CONFIG, f, indent=1)
        f.write("\n")
    print("wrote", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
