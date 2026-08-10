package com.pokewing.efmocap.bbs.form;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;

/**
 * Entry point for the optional BBS form type. Called reflectively by
 * {@code BBSForms.registerFormType()} so nothing on the main classpath ever
 * mentions a BBS class; if this class or BBS is missing, the call is a no-op.
 */
public final class BBSFormRegistry {
    private BBSFormRegistry() {}

    public static void register() {
        FormUtils.getFactory().register(
                mchorse.bbs_mod.utils.resources.Link.create("efmocap:model"),
                EFMocapForm.class);
        FormUtilsClient.register(EFMocapForm.class, EFMocapFormRenderer::new);
    }
}
