package com.solarianofc.archfixtures.modules.alpha;

import com.solarianofc.archfixtures.modules.beta.internal.BetaInternal;

/** Fixture module {@code alpha}: deliberately uses an internal type of module {@code beta} (D-106). */
public class AlphaService {

    public String name() {
        return "alpha uses " + new BetaInternal().name();
    }
}
