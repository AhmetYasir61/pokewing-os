package com.pokewing.efmocap.bbs.form;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.resources.Link;

/**
 * Entry point for the optional BBS form type. Called reflectively by
 * {@code BBSForms.registerFormType()} so nothing on the main classpath ever
 * mentions a BBS class; if this class or BBS is missing, the call is a no-op.
 */
public final class BBSFormRegistry {
    private BBSFormRegistry() {}

    /** Serialised type key, so saved films can find the form again. */
    private static final Link TYPE = Link.create("efmocap:model");

    public static void register() {
        // The architect maps type key -> class, which is what (de)serialisation
        // and BBS's "add form" menu both go through.
        BBSMod.getForms().register(TYPE, EFMocapForm.class);
        FormUtilsClient.register(EFMocapForm.class, EFMocapFormRenderer::new);
    }
}
