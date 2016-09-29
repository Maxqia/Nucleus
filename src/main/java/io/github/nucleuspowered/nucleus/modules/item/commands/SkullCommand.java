/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.argumentparsers.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RegisterCommand({"skull"})
@NoCooldown
@NoWarmup
@NoCost
@Permissions
public class SkullCommand extends AbstractCommand<Player> {

    private final String player = "player";
    private final String amountKey = "amount";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.others", this.getAliases()[0]), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optionalWeak(
                GenericArguments.requiringPermission(
                    GenericArguments.onlyOne(GenericArguments.user(Text.of(player))), permissions.getPermissionWithSuffix("others"))),
            GenericArguments.optional(new PositiveIntegerArgument(Text.of(amountKey)))
        };
    }

    @Override
    public CommandResult executeCommand(Player pl, CommandContext args) throws Exception {
        Optional<User> opl = this.getUser(User.class, pl, player, args);
        if (!opl.isPresent()) {
            return CommandResult.empty();
        }

        int amount = args.<Integer>getOne(amountKey).orElse(1);

        int fullStacks = amount / 64;
        int partialStack = amount % 64;

        // Create the Skull
        ItemStack skullStack = ItemStack.builder().itemType(ItemTypes.SKULL).quantity(64).build();

        // Set it to player skull type and set the owner to the specified player
        if (skullStack.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER).isSuccessful()
                && skullStack.offer(Keys.REPRESENTED_PLAYER, opl.get().getProfile()).isSuccessful()) {
            skullStack.toContainer().set(DataQuery.of("SkullOwner"), opl.get().getName());

            List<ItemStack> itemStackList = Lists.newArrayList();

            // If there were stacks, create as many as needed.
            if (fullStacks > 0) {
                itemStackList.add(skullStack);
                for (int i = 2; i <= fullStacks; i++) {
                    itemStackList.add(skullStack.copy());
                }
            }

            // Same with the partial stacks.
            if (partialStack > 0) {
                ItemStack is = skullStack.copy();
                is.setQuantity(partialStack);
                itemStackList.add(is);
            }

            int accepted = 0;
            int failed = 0;

            Inventory inventoryToOfferTo = pl.getInventory().query(Hotbar.class, GridInventory.class);
            for (ItemStack itemStack : itemStackList) {
                int stackSize = itemStack.getQuantity();
                InventoryTransactionResult itr = inventoryToOfferTo.offer(itemStack);
                int currentFail = itr.getRejectedItems().stream().collect(Collectors.summingInt(ItemStackSnapshot::getCount));
                failed += currentFail;
                accepted += stackSize - currentFail;
            }

            // What was accepted?
            if (accepted > 0) {
                if (failed > 0) {
                    pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.skull.semifull", String.valueOf(failed)));
                }

                if (accepted == 1) {
                    pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.skull.success.single", opl.get().getName()));
                } else {
                    pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.skull.success.plural", String.valueOf(accepted), opl.get().getName()));
                }

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
