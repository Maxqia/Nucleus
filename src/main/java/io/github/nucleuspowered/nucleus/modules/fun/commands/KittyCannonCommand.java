/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.argumentparsers.SelectorWrapperArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

@Permissions(supportsSelectors = true, supportsOthers = true)
@RegisterCommand({"kittycannon", "kc"})
public class KittyCannonCommand extends AbstractCommand<CommandSource> {

    private final Random random = new Random();
    private final String playerKey = "player";

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optional(
                GenericArguments.requiringPermission(
                    new SelectorWrapperArgument(GenericArguments.player(Text.of(playerKey)), permissions, SelectorWrapperArgument.ALL_SELECTORS),
                    permissions.getOthers()))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Collection<Player> playerList = args.getAll(playerKey);
        if (playerList.isEmpty()) {
            if (src instanceof Player) {
                playerList = Lists.newArrayList((Player)src);
            } else {
                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.playeronly"));
            }
        }

        // For each player, create a kitten, throw it out in the direction of the player, and make it explode after between 2 and 5 seconds
        playerList.forEach(x -> getACat(src, x));
        return CommandResult.success();
    }

    private void getACat(CommandSource source, Player spawnAt) {
        // Fire it in the direction that the player is facing with a speed of 0.5 to 1.5, plus the player's current velocity.
        Vector3d velocity = spawnAt.getVelocity().add(spawnAt.getRotation().normalize().mul(random.nextDouble() + 0.5)).add(0, 1, 0);
        World world = spawnAt.getWorld();
        Optional<Entity> ocat = world.createEntity(EntityTypes.OCELOT, spawnAt.getLocation().getPosition().add(0, 1, 0).add(spawnAt.getTransform().getRotationAsQuaternion().getDirection()));
        Entity cat = ocat.get();
        cat.offer(Keys.VELOCITY, velocity);

        Sponge.getScheduler().createTaskBuilder().intervalTicks(5).delayTicks(5)
            .execute(new CatTimer(world.getUniqueId(), cat.getUniqueId(), random.nextInt(60) + 20)).submit(plugin);

        world.spawnEntity(cat, Cause.of(NamedCause.owner(SpawnCause.builder().type(SpawnTypes.PLUGIN).build()), NamedCause.source(source)));
    }

    private class CatTimer implements Consumer<Task> {

        private final UUID entity;
        private final UUID world;
        private int ticksToDestruction;

        private CatTimer(UUID world, UUID entity, int ticksToDestruction) {
            this.entity = entity;
            this.ticksToDestruction = ticksToDestruction;
            this.world = world;
        }

        @Override public void accept(Task task) {
            Optional<World> oWorld = Sponge.getServer().getWorld(world);
            if (!oWorld.isPresent()) {
                task.cancel();
                return;
            }

            Optional<Entity> oe = oWorld.get().getEntity(entity);
            if (!oe.isPresent()) {
                task.cancel();
                return;
            }

            Entity e = oe.get();
            if (e.isRemoved()) {
                task.cancel();
                return;
            }

            ticksToDestruction -= 5;
            if (ticksToDestruction <= 0 || e.isOnGround()) {
                // Cat explodes.
                Explosion explosion = Explosion.builder().origin(e.getLocation().getPosition()).canCauseFire(false)
                    .world(oWorld.get()).shouldDamageEntities(false).shouldPlaySmoke(true).shouldBreakBlocks(false)
                    .radius(2).build();
                e.remove();
                oWorld.get().triggerExplosion(explosion);
                task.cancel();
            }
        }
    }
}
