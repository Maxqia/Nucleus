/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RegisterCommand({"skull"})
@NoCooldown
@NoWarmup
@NoCost
@Permissions
public class SkullCommand extends AbstractCommand<Player> {

    private final String player = "player";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.others", this.getAliases()[0]), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] { GenericArguments.optional(GenericArguments.requiringPermission(
                GenericArguments.onlyOne(GenericArguments.player(Text.of(player))), permissions.getPermissionWithSuffix("others")))};
    }

    @Override
    public CommandResult executeCommand(Player pl, CommandContext args) throws Exception {
        Optional<Player> opl = this.getUser(Player.class, pl, player, args);
        if (!opl.isPresent()) {
            return CommandResult.empty();
        }

        // Create the Skull
        ItemStack skullStack = ItemStack.builder().itemType(ItemTypes.SKULL).quantity(1).build();

        // Set it to player skull type and set the owner to the specified player
        if (skullStack.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER).isSuccessful()
                && skullStack.offer(Keys.REPRESENTED_PLAYER, opl.get().getProfile()).isSuccessful()) {
            // Put it in inventory
            if (pl.getInventory().offer(skullStack).getRejectedItems().isEmpty()) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.skull.success", opl.get().getName()));
                return CommandResult.success();
            }

            pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.skull.full", opl.get().getName()));
            return CommandResult.empty();
        } else {
            pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.skull.error", opl.get().getName()));
            return CommandResult.empty();
        }
    }
}
