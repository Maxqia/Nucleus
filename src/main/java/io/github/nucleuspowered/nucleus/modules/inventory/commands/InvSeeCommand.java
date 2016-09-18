/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.text.Text;

import java.util.Map;

@NoWarmup
@NoCooldown
@NoCost
@Permissions
@RegisterCommand("invsee")
public class InvSeeCommand extends AbstractCommand<Player> {

    private final String player = "player";
    @Inject
    private UserDataManager udm;

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = super.permissionSuffixesToRegister();
        mspi.put("exempt.target", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.invsee.exempt.inspect"), SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.onlyOne(new NicknameArgument(Text.of(player), udm, NicknameArgument.UnderlyingType.PLAYER))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        Player target = args.<Player>getOne(player).get();

        if (target.getUniqueId().equals(src.getUniqueId())) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.invsee.self"));
        }

        if (permissions.testSuffix(target, "exempt.target")) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.invsee.targetexempt", target.getName()));
        }

        Inventory i = target.getInventory().query(GridInventory.class, Hotbar.class);
        src.openInventory(i, Cause.of(NamedCause.of("plugin", plugin), NamedCause.source(src)));
        return CommandResult.success();
    }
}
