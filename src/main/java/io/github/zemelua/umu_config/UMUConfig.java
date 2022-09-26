package io.github.zemelua.umu_config;

import io.github.zemelua.umu_config.config.ConfigManager;
import io.github.zemelua.umu_config.config.IConfigProvider;
import io.github.zemelua.umu_config.config.IConfigValue;
import io.github.zemelua.umu_config.config.container.ConfigContainer;
import io.github.zemelua.umu_config.config.container.IConfigContainer;
import io.github.zemelua.umu_config.config.value.BooleanConfigValue;
import io.github.zemelua.umu_config.config.value.IntegerConfigValue;
import io.github.zemelua.umu_config.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class UMUConfig implements ModInitializer, IConfigProvider {
	public static final String MOD_ID = "umu_config";
	public static final Logger LOGGER = LogManager.getLogger("UMU-Config");

	public static final IConfigValue<Integer> INT_CONFIG_VALUE_EXAMPLE = new IntegerConfigValue(
			"int_config_value_example",
			0,
			10,
			0,
			i -> Text.literal(String.valueOf(i)));

	public static final IConfigContainer TEST_BASIC_CONFIG = new ConfigContainer("test_basic",
			new BooleanConfigValue("test_basicBool", false),
			INT_CONFIG_VALUE_EXAMPLE);

	@Override
	public void onInitialize() {
		NetworkHandler.initialize();
		ConfigManager.initialize();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ConfigManager.stream()
				.forEach(config -> ConfigManager.sendToClient(handler.getPlayer(), config)));
	}

	public static Identifier identifier(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public List<IConfigContainer> getConfigs() {
		return List.of(TEST_BASIC_CONFIG);
	}
}
