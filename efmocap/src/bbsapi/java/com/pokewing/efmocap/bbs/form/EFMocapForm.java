package com.pokewing.efmocap.bbs.form;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.ValueFloat;
import mchorse.bbs_mod.settings.values.ValueString;

/**
 * An EFMocap attachment model as a BBS form.
 *
 * <p>This is the Emoticons-style half of the integration: instead of only
 * letting BBS draw parts for us, we hand BBS a form type of our own, so an
 * {@code .obj} from {@code config/efmocap/attachments} can be picked, placed
 * and animated inside BBS's own character editor and films.</p>
 *
 * <p>Compiled only when a BBS jar is present in {@code efmocap/libs/} — see
 * {@code build.gradle}. The rest of the mod never references these classes
 * directly, so EFMocap still builds and runs with BBS absent.</p>
 */
public class EFMocapForm extends Form {
    /** File name under config/efmocap/attachments. */
    public final ValueString model = new ValueString("model", "");
    /** Optional PNG under config/efmocap/skins; empty uses the skeleton sheet. */
    public final ValueString texture = new ValueString("texture", "");
    public final ValueFloat scale = new ValueFloat("scale", 1.0F);

    public EFMocapForm() {
        super();
        this.register(this.model);
        this.register(this.texture);
        this.register(this.scale);
    }

    @Override
    public String getDefaultDisplayName() {
        String m = this.model.get();
        return m == null || m.isEmpty() ? "EFMocap model" : m;
    }
}
