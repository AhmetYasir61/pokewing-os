# libs/

Optional jars. Nothing here is required — EFMocap reaches Epic Fight and BBS
through reflection and builds fine with this folder empty.

**`bbs*.jar`** — drop the BBS mod jar (e.g. `bbs-mod-2.4-1.20.1.jar`) here to
also build the EFMocap **form type** that shows up inside BBS's own editor
(`src/bbsapi`). That part needs a real compile dependency because it subclasses
BBS's `Form`. With no jar here the source set is skipped and the mod logs
"BBS form type not built into this jar" at startup.
