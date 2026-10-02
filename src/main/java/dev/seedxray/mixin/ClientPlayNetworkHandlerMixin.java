package dev.seedxray.mixin;

import dev.seedxray.seed.SeedFinder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Grabs the hashed seed the server puts in the join and respawn packets. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "onGameJoin", at = @At("TAIL"))
	private void seedxray$onJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		SeedFinder.onJoin(MinecraftClient.getInstance(), packet.commonPlayerSpawnInfo().seed());
	}

	@Inject(method = "onPlayerRespawn", at = @At("TAIL"))
	private void seedxray$onRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
		SeedFinder.onJoin(MinecraftClient.getInstance(), packet.commonPlayerSpawnInfo().seed());
	}
}
