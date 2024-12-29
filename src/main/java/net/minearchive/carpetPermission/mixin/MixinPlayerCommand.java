package net.minearchive.carpetPermission.mixin;

import carpet.commands.PlayerCommand;
import net.minearchive.carpetPermission.CarpetPermissionServer;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerCommand.class)
public abstract class MixinPlayerCommand {

    @Redirect(method = "lambda$register$0", at = @At(value = "INVOKE", target = "Lcarpet/utils/CommandHelper;canUseCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/Object;)Z"))
    private static boolean carpetPermission$checkPermission(CommandSourceStack source, Object commandLevel) {
        return check(source);
    }

    @Unique
    private static boolean check(CommandSourceStack source) {
        if (source.hasPermission(4)) return true;
        if (source.isPlayer()) {
            return CarpetPermissionServer.getInstance().getLevel(source.getPlayer().getUUID()) >= 1;
        }
        return false;
    }
}
