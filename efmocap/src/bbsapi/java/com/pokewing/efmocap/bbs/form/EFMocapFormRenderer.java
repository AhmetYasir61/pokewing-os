package com.pokewing.efmocap.bbs.form;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pokewing.efmocap.ObjRender;
import com.pokewing.efmocap.bbs.BBSForms;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.framework.UIContext;

/**
 * Draws {@link EFMocapForm} with our own OBJ pass.
 *
 * <p>{@code FormRenderer.render} is final: it pushes the stack, applies the
 * form's transforms and then calls {@link #render3D}, so by the time we run,
 * BBS's placement is already on the matrix and all we owe it is the mesh.</p>
 *
 * <p>The context's {@code stack} field is read through {@link BBSForms} rather
 * than named here, because BBS is Fabric-mapped: its signature says
 * {@code class_4587}, which a Mojmap Forge classpath can't resolve even though
 * it is the very same {@link PoseStack} at runtime.</p>
 */
public class EFMocapFormRenderer extends FormRenderer<EFMocapForm> {
    public EFMocapFormRenderer(EFMocapForm form) {
        super(form);
    }

    @Override
    protected void render3D(FormRenderingContext context) {
        PoseStack stack = BBSForms.stackOf(context);
        if (stack == null) return;
        EFMocapForm f = this.form;
        ObjRender.draw(f.model.get(), f.texture.get(), stack, f.scale.get(), context.light);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2) {
        // The model lives in world space; BBS's UI preview draws nothing for it.
    }
}
