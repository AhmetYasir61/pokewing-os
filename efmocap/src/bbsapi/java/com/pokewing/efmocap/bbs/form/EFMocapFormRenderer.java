package com.pokewing.efmocap.bbs.form;

import com.pokewing.efmocap.ObjRender;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

/**
 * Draws {@link EFMocapForm} with our own OBJ pass, using whatever transform BBS
 * has already put on the stack — so BBS keeps full control of placement,
 * parenting and animation while the mesh stays ours.
 */
public class EFMocapFormRenderer extends FormRenderer<EFMocapForm> {
    public EFMocapFormRenderer(EFMocapForm form) {
        super(form);
    }

    @Override
    public void render(FormRenderingContext context) {
        EFMocapForm f = this.form;
        ObjRender.draw(f.model.get(), f.texture.get(),
                context.stack, f.scale.get(), context.light);
    }
}
