package com.pokewing.efmocap.bbs.form;

import mchorse.bbs_mod.forms.FormArchitect;
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

    public static void register() throws Exception {
        // The architect maps type key -> class, which is what (de)serialisation
        // and BBS's "add form" menu both go through. It is reached through
        // BBSMod.getForms(), and BBSMod implements Fabric's ModInitializer —
        // not on this classpath — so that one call is reflective. FormArchitect
        // itself is plain, so everything after it can be typed normally.
        FormArchitect forms = (FormArchitect) Class.forName("mchorse.bbs_mod.BBSMod")
                .getMethod("getForms").invoke(null);
        forms.register(TYPE, EFMocapForm.class);

        FormUtilsClient.register(EFMocapForm.class, EFMocapFormRenderer::new);
    }
}
