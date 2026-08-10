package com.pokewing.efmocap.bbs.form;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

/**
 * An EFMocap attachment model as a BBS form.
 *
 * <p>This is the Emoticons-style half of the integration: instead of only
 * letting BBS draw parts for us, we hand BBS a form type of our own, so an
 * {@code .obj} from {@code config/efmocap/attachments} can be picked, placed
 * and animated inside BBS's own editor and films — with BBS's transforms, body
 * parts and keyframes applying to it like any built-in form.</p>
 *
 * <p>Compiled only when a BBS jar is in {@code efmocap/libs/} and
 * {@code bbs_form_type=true} — see {@code build.gradle}. Nothing else in the
 * mod references these classes directly.</p>
 */
public class EFMocapForm extends Form {
    /** File name under config/efmocap/attachments. */
    public final ValueString model = new ValueString("model", "");
    /** Optional PNG under config/efmocap/skins; empty uses the skeleton sheet. */
    public final ValueString texture = new ValueString("texture", "");
    public final ValueFloat scale = new ValueFloat("scale", 1.0F);

    public EFMocapForm() {
        this.add(this.model);
        this.add(this.texture);
        this.add(this.scale);
    }

    @Override
    public String getDefaultDisplayName() {
        String m = this.model.get();
        return m == null || m.isEmpty() ? "EFMocap model" : m;
    }
}
