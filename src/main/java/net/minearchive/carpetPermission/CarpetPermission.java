package net.minearchive.carpetPermission;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;

public class CarpetPermission implements ModInitializer {

    @Override
    public void onInitialize() {
        CarpetExtension extension = new CarpetPermissionServer();
        CarpetServer.manageExtension(extension);
    }
}
