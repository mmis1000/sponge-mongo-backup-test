package testmongo.testmongo.extensions

import org.spongepowered.api.text.LiteralText
import org.spongepowered.api.text.Text

inline val String.spongeText: LiteralText
    get() = Text.of(this)