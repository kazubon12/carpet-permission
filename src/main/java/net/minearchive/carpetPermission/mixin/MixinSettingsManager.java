package net.minearchive.carpetPermission.mixin;

import carpet.api.settings.SettingsManager;
import net.minearchive.carpetPermission.CarpetPermissionServer;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SettingsManager.class)
public class MixinSettingsManager {

    @Redirect(method = "lambda$registerCommand$11", at = @At(value = "INVOKE", target = "Lcarpet/utils/CommandHelper;canUseCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/Object;)Z"))
    public boolean carpetPermission$checkPermission(CommandSourceStack source, Object commandLevel) {
        return check(source);
    }

    @Unique
    public boolean check(CommandSourceStack source) {
        if (source.hasPermission(4)) return true;
        if (source.isPlayer()) {
            return CarpetPermissionServer.getInstance().getLevel(source.getPlayer().getUUID()) >= 2;
        }
        return false;
    }

}
