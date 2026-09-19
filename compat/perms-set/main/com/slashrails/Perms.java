package com.slashrails;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

/** Permission checks (MC 1.21.11+: permission sets). */
public final class Perms {

    private Perms() {
    }

    /** Game-master permission: commands and the operator-only Track Smoother. */
    public static boolean isGamemaster(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean isGamemaster(Player player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** The same source with full permissions (dev-only scripts). */
    public static CommandSourceStack asAdmin(CommandSourceStack source) {
        return source.withPermission(PermissionSet.ALL_PERMISSIONS);
    }
}
