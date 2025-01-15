package io.github.zemelua.umu_config.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import io.github.zemelua.umu_config.client.gui.ClientConfigScreen;
import io.github.zemelua.umu_config.client.gui.ConfigsScreen;
import io.github.zemelua.umu_config.config.container.IConfigContainer;
import io.github.zemelua.umu_config.util.ModUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.zemelua.umu_config.network.NetworkHandler.*;

public final class ConfigManager {
	@NotNull private static ImmutableMap<String, ImmutableList<IConfigContainer>> CONFIGS = ImmutableMap.of("dummy", ImmutableList.of());
	@NotNull private static ImmutableMap<String, ImmutableList<IConfigContainer>> CLIENT_CONFIGS = ImmutableMap.of("dummy", ImmutableList.of());
	@NotNull @SuppressWarnings({"unused", "FieldMayBeFinal"}) private static ImmutableMap<String, ImmutableList<IConfigContainer>> SERVER_CONFIGS = ImmutableMap.of("dummy", ImmutableList.of());

	@Internal
	public static void initialize() {
		List<EntrypointContainer<IConfigProvider>> modConfigs = FabricLoader.getInstance().getEntrypointContainers("umu-config", IConfigProvider.class);
		CONFIGS = modConfigs.stream().map(entrypoint -> {
			String modID = entrypoint.getProvider().getMetadata().getId();
			IConfigProvider modConfig = entrypoint.getEntrypoint();

			return Pair.of(modID, ImmutableList.copyOf(modConfig.getConfigs()));
		}).collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));

		streamCommon().forEach(ConfigFileManager::loadTo);
		streamCommon().forEach(ConfigFileManager::saveFrom);
	}


	@Internal
	public static void initializeClient() {
		List<EntrypointContainer<IConfigProvider>> modConfigs = FabricLoader.getInstance().getEntrypointContainers("umu-config-client", IConfigProvider.class);
		CLIENT_CONFIGS = modConfigs.stream().map(entrypoint -> {
			String modID = entrypoint.getProvider().getMetadata().getId();
			IConfigProvider modConfig = entrypoint.getEntrypoint();

			return Pair.of(modID, ImmutableList.copyOf(modConfig.getConfigs()));
		}).collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));

		streamClient().forEach(ConfigFileManager::loadTo);
		streamClient().forEach(ConfigFileManager::saveFrom);
	}

	public static void sendToServer(IConfigContainer config) {
		PacketByteBuf packet = PacketByteBufs.create();
		NbtCompound values = new NbtCompound();
		config.saveTo(values);
		packet.writeIdentifier(config.getID());
		packet.writeNbt(values);

		if (!ModUtils.isInMultiplayServer()) {
			ClientPlayNetworking.send(CHANNEL_SYNC_SINGLEPLAY_CONFIG, packet);
		} else {
			ClientPlayNetworking.send(CHANNEL_SYNC_MULTIPLAY_CONFIG, packet);
		}
	}

	public static void sendToClient(ServerPlayerEntity player, IConfigContainer config) {
		PacketByteBuf packet = PacketByteBufs.create();
		NbtCompound values = new NbtCompound();
		config.saveTo(values);
		packet.writeIdentifier(config.getID());
		packet.writeNbt(values);

		ServerPlayNetworking.send(player, CHANNEL_SYNC_CONFIG_TO_CLIENT, packet);
	}

	@Internal
	@SuppressWarnings("unused")
	public static void initializeServer() {
	}


	public static Optional<Screen> openConfigScreen(Screen parent, String modID) {
		List<IConfigContainer> config = Stream.concat(byModIDCommon(modID).stream(), byModIDClient(modID).stream()).toList();
		if (config.isEmpty()) return Optional.empty();

		if (config.size() == 1) {
			return Optional.of(new ClientConfigScreen(parent, config.get(0)));
		} else {
			return Optional.of(new ConfigsScreen(parent, modID));
		}
	}

	public static List<IConfigContainer> byModIDCommon(String modID) {
		@Nullable List<IConfigContainer> configs = CONFIGS.get(modID);

		return configs == null ? List.of() : new ArrayList<>(configs);
	}


	public static List<IConfigContainer> byModIDClient(String modID) {
		@Nullable List<IConfigContainer> configs = CLIENT_CONFIGS.get(modID);

		return configs == null ? List.of() : new ArrayList<>(configs);
	}

	public static Optional<IConfigContainer> byNameCommon(Identifier ID) {
		return streamCommon()
				.filter(config -> config.getID().equals(ID))
				.findFirst();
	}

	public static Stream<IConfigContainer> streamCommon() {
		return CONFIGS.values().stream()
				.flatMap(Collection::stream);
	}


	public static Stream<IConfigContainer> streamClient() {
		return CLIENT_CONFIGS.values().stream()
				.flatMap(Collection::stream);
	}
}
