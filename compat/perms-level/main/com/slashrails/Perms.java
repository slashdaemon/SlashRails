package com.slashrails;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

/** Permission checks (MC ≤ 1.21.10: numeric op levels). */
public final class Perms {

    private Perms() {
    }

    /** Op level 2: commands and the operator-only Track Smoother. */
    public static boolean isGamemaster(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    public static boolean isGamemaster(Player player) {
        return player.hasPermissions(2);
    }

    /** The same source with full permissions (dev-only scripts). */
    public static CommandSourceStack asAdmin(CommandSourceStack source) {
        return source.withPermission(4);
    }
}
