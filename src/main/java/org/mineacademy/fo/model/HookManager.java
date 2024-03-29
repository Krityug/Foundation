package org.mineacademy.fo.model;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.HookManager.PAPIPlaceholder;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketListener;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.IUser;
import com.earth2me.essentials.User;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.gmail.nossr50.datatypes.chat.ChatMode;
import com.gmail.nossr50.datatypes.party.Party;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.util.player.UserManager;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import fr.xephi.authme.api.v3.AuthMeApi;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import me.crafter.mc.lockettepro.LocketteProAPI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

/**
 * Our main class hooking into different plugins, providing you
 * convenience access to their methods
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HookManager {

	// ------------------------------------------------------------------------------------------------------------
	// Store hook classes separately for below, avoiding no such method/field errors
	// ------------------------------------------------------------------------------------------------------------

	private static AuthMeHook authMe;
	private static EssentialsHook essentialsxHook;
	private static MultiverseHook multiverseHook;
	private static ProtocolLibHook protocolLibHook;
	private static TownyHook townyHook;
	private static VaultHook vaultHook;
	private static PlaceholderAPIHook placeholderAPIHook;
	private static FactionsHook factionsHook;
	private static NickyHook nickyHook;
	private static MVdWPlaceholderHook MVdWPlaceholderHook;
	private static McMMOHook mcmmoHook;
	private static LWCHook lwcHook;
	private static LocketteProHook locketteProHook;
	private static ResidenceHook residenceHook;
	private static WorldEditHook worldeditHook;
	private static WorldGuardHook worldguardHook;
	private static PlotSquaredHook plotSquaredHook;
	private static CMIHook CMIHook;
	private static CitizensHook citizensHook;
	private static DiscordSRVHook discordSRVHook;
	private static boolean nbtAPIDummyHook = false;
	private static boolean nuVotifierDummyHook = false;

	// ------------------------------------------------------------------------------------------------------------
	// Main loading method
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Detect various plugins and load their methods into this library so you can use it later
	 */
	public static void loadDependencies() {
		if (Common.doesPluginExistSilently("AuthMe"))
			authMe = new AuthMeHook();

		if (Common.doesPluginExistSilently("Multiverse-Core"))
			multiverseHook = new MultiverseHook();

		if (Common.doesPluginExistSilently("Towny"))
			townyHook = new TownyHook();

		if (Common.doesPluginExistSilently("Vault"))
			vaultHook = new VaultHook();

		if (Common.doesPluginExistSilently("PlaceholderAPI"))
			placeholderAPIHook = new PlaceholderAPIHook();

		if (Common.doesPluginExistSilently("Nicky"))
			nickyHook = new NickyHook();

		if (Common.doesPluginExistSilently("MVdWPlaceholderAPI"))
			MVdWPlaceholderHook = new MVdWPlaceholderHook();

		if (Common.doesPluginExistSilently("LWC"))
			lwcHook = new LWCHook();

		if (Common.doesPluginExistSilently("Lockette"))
			locketteProHook = new LocketteProHook();

		if (Common.doesPluginExistSilently("Residence"))
			residenceHook = new ResidenceHook();

		if (Common.doesPluginExistSilently("WorldEdit"))
			worldeditHook = new WorldEditHook();

		if (Common.doesPluginExistSilently("WorldGuard"))
			worldguardHook = new WorldGuardHook(worldeditHook);

		if (Common.doesPluginExistSilently("mcMMO"))
			mcmmoHook = new McMMOHook();

		if (Common.doesPluginExistSilently("CMI"))
			CMIHook = new CMIHook();

		if (Common.doesPluginExistSilently("Citizens"))
			citizensHook = new CitizensHook();

		if (Common.doesPluginExistSilently("NBTAPI"))
			nbtAPIDummyHook = true;

		if (Common.doesPluginExistSilently("Votifier"))
			nuVotifierDummyHook = true;

		// DiscordSRV
		if (Common.doesPluginExistSilently("DiscordSRV")) {
			try {
				Class.forName("github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel");

				discordSRVHook = new DiscordSRVHook();

			} catch (final ClassNotFoundException ex) {
				Common.error(ex, "&c" + SimplePlugin.getNamed() + " failed to hook DiscordSRV because the plugin is outdated (1.18.x is supported)!");
			}
		}

		// EssentialsX
		if (Common.doesPluginExistSilently("Essentials")) {
			final boolean isEssentialsX = Bukkit.getPluginManager().getPlugin("Essentials").getDescription().getAuthors().contains("drtshock");

			if (isEssentialsX)
				essentialsxHook = new EssentialsHook();
			else
				Common.log("Detected old Essentials. We only support EssentialsX, see https://spigotmc.org/resources/9089");
		}

		// Plotsquared
		if (Common.doesPluginExistSilently("PlotSquared")) {
			final String ver = Bukkit.getPluginManager().getPlugin("PlotSquared").getDescription().getVersion();

			if (ver.startsWith("4."))
				plotSquaredHook = new PlotSquaredHook();
			else
				Common.log("&eCould not hook into PlotSquared, version 4.x required, you have " + ver);
		}

		// ProtocolLib
		if (Common.doesPluginExistSilently("ProtocolLib")) {
			protocolLibHook = new ProtocolLibHook();

			// Also check if the library is loaded properly
			try {
				if (MinecraftVersion.newerThan(V.v1_6))
					Class.forName("com.comphenix.protocol.wrappers.WrappedChatComponent");
			} catch (final Throwable t) {
				protocolLibHook = null;

				Common.throwError(t, "You are running an old and unsupported version of ProtocolLib, please update it.");
			}
		}

		// Various kinds of Faction plugins
		if (Common.doesPluginExistSilently("Factions")) {
			final String ver = Bukkit.getPluginManager().getPlugin("Factions").getDescription().getVersion();

			if (ver.startsWith("1.6")) {
				Common.log("Recognized and hooked FactionsUUID...");

				factionsHook = new FactionsUUID();

			} else if (ver.startsWith("2.")) {
				Class<?> mplayer = null;

				try {
					mplayer = Class.forName("com.massivecraft.factions.entity.MPlayer"); // only support the free version of the plugin
				} catch (final ClassNotFoundException ex) {
				}

				if (mplayer != null) {
					Common.log("Recognized and hooked MCore Factions...");

					factionsHook = new FactionsMassive();
				} else
					Common.log("&cRecognized MCore Factions, but not hooked! Check if you have the latest version!");

			}
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods for determining which plugins were loaded after you call the load method
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Is AuthMe Reloaded loaded? We only support the latest version
	 *
	 * @return
	 */
	public static boolean isAuthMeLoaded() {
		return authMe != null;
	}

	/**
	 * Is CMI loaded?
	 *
	 * @return
	 */
	public static boolean isCMILoaded() {
		return CMIHook != null;
	}

	/**
	 * Is Citizens loaded?
	 *
	 * @return
	 */
	public static boolean isCitizensLoaded() {
		return citizensHook != null;
	}

	/**
	 * Is EssentialsX loaded?
	 *
	 * @return
	 */
	public static boolean isEssentialsXLoaded() {
		return essentialsxHook != null;
	}

	/**
	 * Is Multiverse-Core loaded?
	 *
	 * @return
	 */
	public static boolean isMultiverseCoreLoaded() {
		return multiverseHook != null;
	}

	/**
	 * Is ProtocolLib loaded?
	 *
	 * This will not only check if the plugin is in plugins folder, but also if it's
	 * correctly loaded and working. (*Should* detect plugin's malfunction when
	 * out-dated.)
	 *
	 * @return
	 */
	public static boolean isProtocolLibLoaded() {
		return protocolLibHook != null;
	}

	/**
	 * Is Towny loaded?
	 *
	 * @return
	 */
	public static boolean isTownyLoaded() {
		return townyHook != null;
	}

	/**
	 * Is TownyChat loaded?
	 *
	 * @return
	 */
	public static boolean isTownyChatLoaded() {
		return townyHook != null && townyHook.hasChannelPlugin();
	}

	/**
	 * Is Vault loaded?
	 *
	 * @return
	 */
	public static boolean isVaultLoaded() {
		return vaultHook != null;
	}

	/**
	 * Is PlaceholderAPI loaded?
	 *
	 * @return
	 */
	public static boolean isPlaceholderAPILoaded() {
		return placeholderAPIHook != null;
	}

	/**
	 * Are Faction plugins loaded? We support UUID factions and free factions
	 *
	 * @return
	 */
	public static boolean isFactionsLoaded() {
		return factionsHook != null;
	}

	/**
	 * Is Nicky loaded?
	 *
	 * @return
	 */
	public static boolean isNickyLoaded() {
		return nickyHook != null;
	}

	/**
	 * Is MVdWPlaceholderAPI loaded?
	 *
	 * @return
	 */
	public static boolean isMVdWPlaceholderAPILoaded() {
		return MVdWPlaceholderHook != null;
	}

	/**
	 * Is mcMMO loaded?
	 *
	 * @return
	 */
	public static boolean isMcMMOLoaded() {
		return mcmmoHook != null;
	}

	/**
	 * Is LWC loaded?
	 *
	 * @return
	 */
	public static boolean isLWCLoaded() {
		return lwcHook != null;
	}

	/**
	 * Is Lockette Pro loaded
	 *
	 * @return
	 */
	public static boolean isLocketteProLoaded() {
		return locketteProHook != null;
	}

	/**
	 * Is Residence loaded?
	 *
	 * @return
	 */
	public static boolean isResidenceLoaded() {
		return residenceHook != null;
	}

	/**
	 * Is WorldEdit loaded?
	 *
	 * @return
	 */
	public static boolean isWorldEditLoaded() {
		return worldeditHook != null;
	}

	/**
	 * Is WorldGuard loaded?
	 *
	 * @return
	 */
	public static boolean isWorldGuardLoaded() {
		return worldguardHook != null;
	}

	/**
	 * Is PlotSquared loaded?
	 *
	 * @return
	 */
	public static boolean isPlotSquaredLoaded() {
		return plotSquaredHook != null;
	}

	/**
	 * Is DiscordSRV loaded?
	 *
	 * @return
	 */
	public static boolean isDiscordSRVLoaded() {
		return discordSRVHook != null;
	}

	/**
	 * Is NBTAPI loaded as a plugin?
	 *
	 * @return
	 */
	public static boolean isNbtAPILoaded() {
		return nbtAPIDummyHook;
	}

	/**
	 * Is nuVotifier loaded as a plugin?
	 *
	 * @return
	 */
	public static boolean isNuVotifierLoaded() {
		return nuVotifierDummyHook;
	}

	/**
	 * Is FastAsyncWorldEdit loaded?
	 *
	 * @return
	 */
	public static boolean isFAWELoaded() {
		final Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
		final String desc = worldEdit != null ? worldEdit.getDescription().getDescription() : null;

		return "Fast Async WorldEdit plugin".equals(desc);
	}

	// ------------------------------------------------------------------------------------------------------------
	//
	//
	// Delegate methods for use from other plugins
	//
	//
	// ------------------------------------------------------------------------------------------------------------

	// ------------------------------------------------------------------------------------------------------------
	// AuthMe
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if player is logged via AuthMe, or true if AuthMe is not installed
	 *
	 * @param player
	 * @return
	 */
	public static boolean isLogged(Player player) {
		return isAuthMeLoaded() ? authMe.isLogged(player) : true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// EssentialsX or CMI
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the given player is afk in EssentialsX or CMI, or false if neither plugin is present
	 *
	 * @param playerName
	 * @return
	 */
	public static boolean isAfk(Player player) {
		final boolean essAFK = isEssentialsXLoaded() ? essentialsxHook.isAfk(player.getName()) : false;
		final boolean cmiAFK = isCMILoaded() ? CMIHook.isAfk(player) : false;

		return essAFK || cmiAFK;
	}

	/**
	 * Return true if the given player is vanished in EssentialsX or CMI, or false if neither plugin is present
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(Player player) {
		final boolean essVanish = isEssentialsXLoaded() ? essentialsxHook.isVanished(player.getName()) : false;
		final boolean cmiVanish = isCMILoaded() ? CMIHook.isVanished(player) : false;

		return essVanish || cmiVanish;
	}

	/**
	 * Return true if the player is muted in EssentialsX or CMI, or false if neither plugin is present
	 *
	 * @param player
	 * @return
	 */
	public static boolean isMuted(Player player) {
		final boolean isEssMuted = isEssentialsXLoaded() ? essentialsxHook.isMuted(player.getName()) : false;
		final boolean isCMIMuted = isCMILoaded() ? CMIHook.isMuted(player) : false;

		return isEssMuted || isCMIMuted;
	}

	/**
	 * Toggles a god mode for player from EssentialsX or CMI
	 *
	 * @param player
	 * @param godMode
	 */
	public static void setGodMode(Player player, boolean godMode) {
		if (isEssentialsXLoaded())
			essentialsxHook.setGodMode(player, godMode);

		if (isCMILoaded())
			CMIHook.setGodMode(player, godMode);
	}

	/**
	 * Sets the last /back location for both EssentialsX and CMI
	 *
	 * @param player
	 * @param location
	 */
	public static void setBackLocation(Player player, Location location) {
		if (isEssentialsXLoaded())
			essentialsxHook.setBackLocation(player.getName(), location);

		if (isCMILoaded())
			CMIHook.setLastTeleportLocation(player, location);
	}

	/**
	 * Set EssentialsX and CMI ignored player
	 *
	 * @param player
	 * @param who
	 * @param ignore
	 */
	public static void setIgnore(String player, String who, boolean ignore) {
		if (isEssentialsXLoaded())
			essentialsxHook.setIgnore(player, who, ignore);

		if (isCMILoaded())
			CMIHook.setIgnore(player, who, ignore);
	}

	/**
	 * Return true if the player is ignoring another player in EssentialsX
	 *
	 * @param player
	 * @param who
	 * @return
	 */
	public static boolean isIgnoring(String player, String who) {
		Valid.checkBoolean(player != null && !player.isEmpty(), "Player to check ignore from cannot be null/empty");
		Valid.checkBoolean(who != null && !who.isEmpty(), "Player to check ignore to cannot be null/empty");

		return isEssentialsXLoaded() ? essentialsxHook.isIgnoring(player, who) : isCMILoaded() ? CMIHook.isIgnoring(player, who) : false;
	}

	/**
	 * Returns the nick for the given recipient from Essentials or Nicky, or if it's a console, their name
	 *
	 * @param sender
	 * @return
	 */
	public static String getNick(CommandSender sender) {
		final Player player = sender instanceof Player ? (Player) sender : null;

		if (player != null && isNPC(player)) {
			Common.log("&eWarn: Called getNick for NPC! Notify the developers to add an ignore check at " + Debugger.traceRoute(true));

			return player.getName();
		}

		if (player == null)
			return sender.getName();

		final String nickyNick = isNickyLoaded() ? nickyHook.getNick_(player) : null;
		final String essNick = isEssentialsXLoaded() ? essentialsxHook.getNick_(player.getName()) : null;
		final String cmiNick = isCMILoaded() ? CMIHook.getNick_(player) : null;

		return nickyNick != null ? nickyNick : cmiNick != null ? cmiNick : essNick != null ? essNick : sender.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// EssentialsX
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the reply recipient for the given player, or null if not exist
	 *
	 * @param player
	 * @return
	 */
	public static Player getReplyTo(Player player) {
		return isEssentialsXLoaded() ? essentialsxHook.getReplyTo(player.getName()) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Multiverse-Core
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the world name alias from Multiverse-Core
	 *
	 * @param world
	 * @return
	 */
	public static String getWorldAlias(World world) {
		return isMultiverseCoreLoaded() ? multiverseHook.getWorldAlias(world.getName()) : world.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Towny
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return players nation from Towny, or null if not loaded
	 *
	 * @param player
	 * @return
	 */
	public static String getNation(Player player) {
		return isTownyLoaded() ? townyHook.getNationName(player) : null;
	}

	/**
	 * Return players town name from Towny, or null if none
	 *
	 * @param player
	 * @return
	 */
	public static String getTownName(Player player) {
		return isTownyLoaded() ? townyHook.getTownName(player) : null;
	}

	/**
	 * Return the online residents in players town, or an empty list
	 *
	 * @param player
	 * @return
	 */
	public static Collection<? extends Player> getTownResidentsOnline(Player player) {
		return isTownyLoaded() ? townyHook.getTownResidentsOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the online nation players in players nation (Towny), or an empty list
	 *
	 * @param player
	 * @return
	 */
	public static Collection<? extends Player> getNationPlayersOnline(Player player) {
		return isTownyLoaded() ? townyHook.getNationPlayersOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the town owner name at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getTownOwner(Location location) {
		return isTownyLoaded() ? townyHook.getTownOwner(location) : null;
	}

	/**
	 * Return the town name at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getTown(Location location) {
		return isTownyLoaded() ? townyHook.getTown(location) : null;
	}

	/**
	 * Return a list of all loaded towns, or an empty list if none
	 *
	 * @return
	 */
	public static List<String> getTowns() {
		return isTownyLoaded() ? townyHook.getTowns() : new ArrayList<>();
	}

	/**
	 * Return the townychat town channel for player or null if none
	 *
	 * @param player
	 * @return
	 */
	public static String getTownChannel(Player player) {
		return isTownyChatLoaded() ? townyHook.getTownyChannel(player) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Vault
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the Vault player prefix or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerPrefix(Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerPrefix(player) : "";
	}

	/**
	 * Return the Vault player suffix or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerSuffix(Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerSuffix(player) : "";
	}

	/**
	 * Return the Vault player permission group or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerPermissionGroup(Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerGroup(player) : "";
	}

	/**
	 * Return the players balance from Vault (hooks into your economy plugin)
	 *
	 * @param player
	 * @return
	 */
	public static double getBalance(Player player) {
		return isVaultLoaded() ? vaultHook.getBalance(player) : 0;
	}

	/**
	 * Return the singular currency name, or null if not loaded
	 *
	 * @return
	 */
	public static String getCurrencySingular() {
		return isVaultLoaded() ? vaultHook.getCurrencyNameSG() : null;
	}

	/**
	 * Return the plural currency name, or null if not loaded
	 *
	 * @return
	 */
	public static String getCurrencyPlural() {
		return isVaultLoaded() ? vaultHook.getCurrencyNamePL() : null;
	}

	/**
	 * Takes money from the player if Vault is installed
	 *
	 * @param player
	 * @param amount
	 */
	public static void withdraw(Player player, double amount) {
		if (isVaultLoaded())
			vaultHook.withdraw(player, amount);
	}

	/**
	 * Gives money to the player if Vault is installed
	 *
	 * @param player
	 * @param amount
	 */
	public static void deposit(Player player, double amount) {
		if (isVaultLoaded())
			vaultHook.deposit(player, amount);
	}

	/**
	 * Check to see if this user has the given permission node <br>
	 * Note: Due to vault API Limitations, this will return false if the node is
	 * false or undefined. Some permissions plugins don't load superperms into their
	 * dataset, so this should not be relied on.
	 *
	 * @param online The player to check
	 * @param perm   Permission node
	 * @return true if permission is set and not denied
	 * @deprecated use {@link PlayerUtil#hasPerm(org.bukkit.permissions.Permissible, String)}
	 */
	@Deprecated
	public static boolean hasPermissionVault(Player online, String perm) {
		return online != null && online.getUniqueId() != null
				&& isVaultLoaded()
				&& vaultHook.hasPerm(online.getWorld().getName(), online.getName(), perm);
	}

	/**
	 * Checks if the given UUID has permission (uses Vault)
	 *
	 * @param id
	 * @param perm
	 * @return
	 * @deprecated use {@link PlayerUtil#hasPerm(org.bukkit.permissions.Permissible, String)}
	 */
	@Deprecated
	public static boolean hasPermissionUnsafe(UUID id, String perm) {
		final OfflinePlayer player = Remain.getOfflinePlayerByUUID(id);
		final Boolean has = player != null && isVaultLoaded() ? vaultHook.hasPerm(player.getName(), perm) : null;

		return hasPerm0(player, has);
	}

	/**
	 * Checks if the given player name has a certain permission using vault
	 *
	 * @param name
	 * @param perm
	 * @return
	 * @deprecated use {@link PlayerUtil#hasPerm(org.bukkit.permissions.Permissible, String)}
	 */
	@Deprecated
	public static boolean hasPermissionUnsafe(String name, String perm) {
		final OfflinePlayer player = Bukkit.getOfflinePlayer(name);
		final Boolean has = player != null && player.getName() != null && isVaultLoaded() ? vaultHook.hasPerm(player.getName(), perm) : null;

		return hasPerm0(player, has);
	}

	private static boolean hasPerm0(OfflinePlayer player, Boolean has) {
		if (has != null) {
			if (!has && player != null)
				return player.isOp();

			return has;
		}

		return player != null ? player.isOp() : false;
	}

	/**
	 * Returns the players primary permission group using Vault, or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerPrimaryGroup(Player player) {
		return isVaultLoaded() ? vaultHook.getPrimaryGroup(player) : "";
	}

	/**
	 * Return true if Vault could find a suitable chat plugin to hook to
	 *
	 * @return
	 */
	public static boolean isChatIntegrated() {
		return isVaultLoaded() ? vaultHook.isChatIntegrated() : false;
	}

	/**
	 * Return true if Vault could find a suitable economy plugin to hook to
	 *
	 * @return
	 */
	public static boolean isEconomyIntegrated() {
		return isVaultLoaded() ? vaultHook.isEconomyIntegrated() : false;
	}

	/**
	 * Updates Vault service providers
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void updateVaultIntegration() {
		if (isVaultLoaded())
			vaultHook.setIntegration();
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlaceholderAPI / MVdWPlaceholderAPI
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Uses PlaceholderAPI and MVdWPlaceholderAPI to replace placeholders in a message
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	public static String replacePlaceholders(Player player, String message) {
		if (message == null || "".equals(message.trim()))
			return message;

		message = isPlaceholderAPILoaded() ? placeholderAPIHook.replacePlaceholders(player, message) : message;
		message = isMVdWPlaceholderAPILoaded() ? MVdWPlaceholderHook.replacePlaceholders(player, message) : message;

		return message;
	}

	/**
	 * Uses PlaceholderAPI to replace relation placeholders in a message
	 *
	 * @param one
	 * @param two
	 * @param msg
	 * @return
	 */
	public static String replaceRelationPlaceholders(Player one, Player two, String msg) {
		if (msg == null || "".equals(msg.trim()))
			return msg;

		return isPlaceholderAPILoaded() ? placeholderAPIHook.replaceRelationPlaceholders(one, two, msg) : msg;
	}

	/**
	 * If PlaceholderAPI is loaded, registers a new placeholder within it
	 * with the given variable and value.
	 *
	 * The variable is automatically prepended with your plugin name, lowercased + _,
	 * such as chatcontrol_ or boss_ + your variable.
	 *
	 * Example if the variable is player health in ChatControl plugin: "chatcontrol_health"
	 *
	 * The value will be called against the given player and the variable you set initially
	 *
	 * NB: In your chat formatting plugin you can append your variable with a "+" sign
	 * to automatically insert a space after it in case it is not empty (NOT HERE, but in your
	 * chat control plugin)
	 *
	 * @param variable
	 * @param value
	 *
	 * @deprecated does not register the variable in PlaceholderAPI
	 */
	@Deprecated
	public static void addPlaceholder(String variable, BiFunction<Player, String, String> value) {
		if (isPlaceholderAPILoaded())
			placeholderAPIHook.addPlaceholder(new PAPIPlaceholder(variable, value), null);
	}

	/**
	 * If PlaceholderAPI is loaded, registers a new placeholder within it
	 * with the given variable and value.
	 *
	 * The variable is automatically prepended with your plugin name, lowercased + _,
	 * such as chatcontrol_ or boss_ + your variable.
	 *
	 * Example if the variable is player health in ChatControl plugin: "chatcontrol_health"
	 *
	 * The value will be called against the given player
	 *
	 * NB: In your chat formatting plugin you can append your variable with a "+" sign
	 * to automatically insert a space after it in case it is not empty (NOT HERE, but in your
	 * chat control plugin)
	 *
	 * @param variable
	 * @param value
	 */
	public static void addPlaceholder(String variable, Function<Player, String> value) {
		if (isPlaceholderAPILoaded())
			placeholderAPIHook.addPlaceholder(new PAPIPlaceholder(variable, (player, identifier) -> value.apply(player)), value);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Factions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get all loaded Factions or null if none
	 *
	 * @return
	 */
	public static Collection<String> getFactions() {
		return isFactionsLoaded() ? factionsHook.getFactions() : null;
	}

	/**
	 * Return the players faction or null if none
	 *
	 * @param player
	 * @return
	 */
	public static String getFaction(Player player) {
		return isFactionsLoaded() ? factionsHook.getFaction(player) : null;
	}

	/**
	 * Return players in players faction or null if none
	 *
	 * @param player
	 * @return
	 */
	public static Collection<? extends Player> getOnlineFactionPlayers(Player player) {
		return isFactionsLoaded() ? factionsHook.getSameFactionPlayers(player) : null;
	}

	/**
	 * Return a faction name at the given location, or null
	 *
	 * @param location
	 * @return
	 */
	public static String getFaction(Location location) {
		return isFactionsLoaded() ? factionsHook.getFaction(location) : null;
	}

	/**
	 * Return the faction owner name at the given location, or null
	 *
	 * @param location
	 * @return
	 */
	public static String getFactionOwner(Location location) {
		return isFactionsLoaded() ? factionsHook.getFactionOwner(location) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// ProtocolLib
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a {@link PacketAdapter} packet listener to ProtocolLib.
	 *
	 * If the plugin is missing, an error will be thrown
	 *
	 * @param adapter
	 */
	public static void addPacketListener(/*Uses object to prevent errors if plugin is not installed*/Object adapter) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Cannot add packet listeners if ProtocolLib isn't installed");

		protocolLibHook.addPacketListener(adapter);
	}

	/**
	 * Removes packet listeners from ProtocolLib for a plugin
	 *
	 * @param plugin
	 * @deprecated no need to call this manually as we call this in {@link SimplePlugin} when
	 * you restart or reload your plugin automatically
	 */
	@Deprecated
	public static void removePacketListeners(Plugin plugin) {
		if (isProtocolLibLoaded())
			protocolLibHook.removePacketListeners(plugin);
	}

	/**
	 * Send a {@link PacketContainer} to the given player
	 *
	 * @param player
	 * @param packetContainer
	 */
	public static void sendPacket(Player player, Object packetContainer) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Sending packets requires ProtocolLib installed and loaded");

		protocolLibHook.sendPacket(player, packetContainer);
	}

	// ------------------------------------------------------------------------------------------------------------
	// LWC
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the LWC owner of the block, or null
	 *
	 * @param block
	 * @return
	 */
	public static String getLWCOwner(Block block) {
		return isLWCLoaded() ? lwcHook.getOwner(block) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Lockette Pro
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return if the given player owns the given block from Lockette Pro
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	public static boolean isLocketteOwner(Block block, Player player) {
		return isLocketteProLoaded() ? locketteProHook.isOwner(block, player) : false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Residence
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a list of Residence residences at the given location or an empty list
	 *
	 * @return
	 */
	public static Collection<String> getResidences() {
		return isResidenceLoaded() ? residenceHook.getResidences() : new ArrayList<>();
	}

	/**
	 * Get the Residence name at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getResidence(Location location) {
		return isResidenceLoaded() ? residenceHook.getResidence(location) : null;
	}

	/**
	 * Get the Residence owner at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getResidenceOwner(Location location) {
		return isResidenceLoaded() ? residenceHook.getResidenceOwner(location) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// WorldGuard
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return WorldGuard list of regions at the given location or an empty list
	 *
	 * @param loc
	 * @return
	 */
	public static List<String> getRegions(Location loc) {
		return isWorldGuardLoaded() ? worldguardHook.getRegionsAt(loc) : new ArrayList<>();
	}

	/**
	 * Return WorldGuard list of loaded regions or an empty list
	 *
	 * @return
	 */
	public static List<String> getRegions() {
		return isWorldGuardLoaded() ? worldguardHook.getAllRegions() : new ArrayList<>();
	}

	/**
	 * Get our representation of a worldguard region by its name or null
	 *
	 * @param name
	 * @return
	 */
	public static Region getRegion(String name) {
		return isWorldGuardLoaded() ? worldguardHook.getRegion(name) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlotSquared
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get a list of players inside a PlotSquared plot, or null if not loaded
	 *
	 * @param players
	 * @return
	 */
	public static Collection<? extends Player> getPlotPlayers(Player players) {
		return isPlotSquaredLoaded() ? plotSquaredHook.getPlotPlayers(players) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// mcMMO
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the active mcMMO party chat
	 *
	 * @param player
	 * @return
	 */
	public static String getActivePartyChat(Player player) {
		return isMcMMOLoaded() ? mcmmoHook.getActivePartyChat(player) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Citizens
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the entity is a Citizens NPC
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isNPC(Entity entity) {
		return isCitizensLoaded() ? citizensHook.isNPC(entity) : false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// DiscordSRV
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return all linked Discord channels. You can link those in DiscordSRV config.yml file
	 *
	 * @return the linked channels or an empty set when DiscordSRV is not loaded
	 */
	public static Set<String> getDiscordChannels() {
		return isDiscordSRVLoaded() ? discordSRVHook.getChannels() : new HashSet<>();
	}

	/**
	 * Sends a message from the given sender to a certain channel on DiscordSRV
	 *
	 * @param senderName
	 * @param channel
	 * @param message
	 */
	public static void sendDiscordMessage(String senderName, String channel, String message) {
		if (isDiscordSRVLoaded())
			discordSRVHook.sendMessage(senderName, channel, message);
	}

	/**
	 * Sends a message from the given sender to a certain channel on Discord using DiscordSRV
	 *
	 * Enhanced functionality is available if the sender is a player
	 *
	 * @param sender
	 * @param channel
	 * @param message
	 */
	public static void sendDiscordMessage(CommandSender sender, String channel, String message) {
		if (isDiscordSRVLoaded())
			discordSRVHook.sendMessage(sender, channel, message);
	}

	/**
	 * Send a message to a Discord channel if DiscordSRV is installed
	 *
	 * @param channel
	 * @param message
	 */
	public static void sendDiscordMessage(String channel, String message) {
		if (isDiscordSRVLoaded())
			discordSRVHook.sendMessage(channel, message);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Class helpers
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a PlaceholderAPI placeholder replacer with the given
	 * variable (will be prepended with the name of your plugin, such as
	 *
	 * chatcontrol_ + this variable
	 *
	 * and the value that is callable so that you can return updated value each time.
	 */
	@Data
	static class PAPIPlaceholder {

		private final String variable;
		private final BiFunction<Player, String, String> value;
	}
}

// ------------------------------------------------------------------------------------------------------------
//
// Below are the individual classes responsible for hooking into third party plugins
// and getting data from them. Due to often changes we do not keep those documented.
//
// ------------------------------------------------------------------------------------------------------------

class AuthMeHook {

	boolean isLogged(Player player) {
		try {
			final AuthMeApi instance = AuthMeApi.getInstance();

			return instance.isAuthenticated(player);
		} catch (final Throwable t) {
			return false;
		}
	}
}

class EssentialsHook {

	private final Essentials ess;

	EssentialsHook() {
		ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}

	void setGodMode(Player player, boolean godMode) {
		final User user = getUser(player.getName());

		if (user != null)
			user.setGodModeEnabled(godMode);
	}

	void setIgnore(String player, String toIgnore, boolean ignore) {
		try {
			final com.earth2me.essentials.User user = ess.getUser(player);
			final com.earth2me.essentials.User toIgnoreUser = ess.getUser(toIgnore);

			if (ignore && user.isIgnoredPlayer(toIgnoreUser))
				return;

			user.setIgnoredPlayer(toIgnoreUser, ignore);

		} catch (final Throwable t) {
		}
	}

	boolean isIgnoring(String player, String ignoringPlayer) {
		try {
			final com.earth2me.essentials.User user = ess.getUser(player);
			final com.earth2me.essentials.User ignored = ess.getUser(ignoringPlayer);

			return user != null && ignored != null && user.isIgnoredPlayer(ignored);

		} catch (final Throwable t) {
			return false;
		}
	}

	boolean isAfk(String pl) {
		final IUser user = getUser(pl);

		return user != null ? user.isAfk() : false;
	}

	boolean isVanished(String pl) {
		final IUser user = getUser(pl);

		return user != null ? user.isVanished() : false;
	}

	boolean isMuted(String pl) {
		final com.earth2me.essentials.User user = getUser(pl);

		return user != null ? user.isMuted() : false;
	}

	Player getReplyTo(String recipient) {
		final User user = getUser(recipient);

		if (user == null)
			return null;

		try {
			final String replyPlayer = user.getReplyRecipient().getName();
			final Player bukkitPlayer = Bukkit.getPlayer(replyPlayer);

			if (bukkitPlayer != null && bukkitPlayer.isOnline())
				return bukkitPlayer;

		} catch (final Throwable ex) {
		}

		return null;
	}

	String getNick_(String player) {
		final User user = getUser(player);

		if (user == null) {
			Common.log("&cMalfunction getting Essentials user. Have you reloaded?");

			return player;
		}

		final String essNick = Common.getOrEmpty(user.getNickname());

		return "".equals(essNick) ? null : essNick;
	}

	void setBackLocation(String player, Location loc) {
		final User user = getUser(player);

		if (user != null)
			try {
				user.setLastLocation(loc);

			} catch (final Throwable t) {
			}
	}

	private User getUser(String name) {
		if (ess.getUserMap() == null)
			return null;

		User user = null;

		try {
			user = ess.getUserMap().getUser(name);
		} catch (final Throwable t) {
		}

		if (user == null)
			try {
				user = ess.getUserMap().getUserFromBukkit(name);
			} catch (final Throwable ex) {
			}
		return user;
	}

}

class MultiverseHook {

	private final MultiverseCore multiVerse;

	MultiverseHook() {
		multiVerse = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
	}

	String getWorldAlias(String world) {
		final MultiverseWorld mvWorld = multiVerse.getMVWorldManager().getMVWorld(world);

		if (mvWorld != null)
			return mvWorld.getColoredWorldString();

		return world;
	}
}

class TownyHook {
	private boolean hasChat;
	com.palmergames.bukkit.TownyChat.Chat townyChat = null;

	TownyHook() {
		if (hasChat = Common.doesPluginExistSilently("TownyChat")) {
			final Plugin p = Bukkit.getServer().getPluginManager().getPlugin("TownyChat");
			if (p instanceof com.palmergames.bukkit.TownyChat.Chat) {
				townyChat = (com.palmergames.bukkit.TownyChat.Chat) p;
			}
		}
	}

	Collection<? extends Player> getTownResidentsOnline(Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playersTown = getTownName(pl);

		if (!playersTown.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playersTown.equals(getTownName(online)))
					recipients.add(online);

		return recipients;
	}

	Collection<? extends Player> getNationPlayersOnline(Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playerNation = getNationName(pl);

		if (!playerNation.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playerNation.equals(getNationName(online)))
					recipients.add(online);

		Debugger.debug("towny", "Players in " + pl.getName() + "'s nation '" + playerNation + "': " + recipients);
		return recipients;
	}

	String getTownName(Player pl) {
		final Town t = getTown(pl);

		return t != null ? t.getName() : "";
	}

	String getNationName(Player pl) {
		final Nation n = getNation(pl);

		return n != null ? n.getName() : "";
	}

	List<String> getTowns() {
		try {
			return Common.convert(TownyUniverse.getDataSource().getTowns(), (t) -> t.getName());

		} catch (final Throwable e) {
			return new ArrayList<>();
		}
	}

	String getTown(Location loc) {
		try {
			return TownyUniverse.getTownName(loc);

		} catch (final Throwable e) {
			return null;
		}
	}

	String getTownOwner(Location loc) {
		try {
			return TownyUniverse.getDataSource().getTown(TownyUniverse.getTownName(loc)).getMayor().getName();

		} catch (final Throwable e) {
			return null;
		}
	}

	boolean hasChannelPlugin() {
		return hasChat;
	}

	String getTownyChannel(Player pl) {
		try {
			// towny chat doesn't have a nice channel manager

			// this is used for sending directly to a channel, and it calls the async chat event
			if (townyChat.getTownyPlayerListener().directedChat.containsKey(pl)) {
				final com.palmergames.bukkit.TownyChat.channels.Channel channel = townyChat.getChannelsHandler().getChannel(pl, townyChat.getTownyPlayerListener().directedChat.get(pl));
				if (channel != null) {
					return channel.getName();
				}
			}

			for (final com.palmergames.bukkit.TownyChat.channels.Channel channel : townyChat.getChannelsHandler().getAllChannels().values()) {
				if (townyChat.getTowny().hasPlayerMode(pl, channel.getName())) {
					return channel.getName();
				}
			}

			final com.palmergames.bukkit.TownyChat.channels.Channel channel = townyChat.getChannelsHandler().getActiveChannel(pl,
					com.palmergames.bukkit.TownyChat.channels.channelTypes.GLOBAL);
			return channel == null ? null : channel.getName();

		} catch (final Throwable ex) {
		}
		return null;
	}

	private Nation getNation(Player pl) {
		final Town town = getTown(pl);

		try {
			return town.getNation();

		} catch (final Throwable ex) {
			return null;
		}
	}

	private Town getTown(Player pl) {
		final Resident res = getResident(pl);

		try {
			return res.getTown();

		} catch (final Throwable ex) {
			return null;
		}
	}

	private Resident getResident(Player pl) {
		try {
			return TownyUniverse.getDataSource().getResident(pl.getName());

		} catch (final Throwable e) {
			return null;
		}
	}
}

class ProtocolLibHook {

	private final ProtocolManager manager;

	ProtocolLibHook() {
		manager = ProtocolLibrary.getProtocolManager();
	}

	final void addPacketListener(Object listener) {
		Valid.checkBoolean(listener instanceof PacketListener, "Listener must extend or implements PacketListener or PacketAdapter");
		manager.addPacketListener((PacketListener) listener);
	}

	final void removePacketListeners(Plugin plugin) {
		manager.removePacketListeners(plugin);
	}

	final void sendPacket(PacketContainer packet) {
		for (final Player player : Remain.getOnlinePlayers())
			sendPacket(player, packet);
	}

	final void sendPacket(Player player, Object packet) {
		Valid.checkNotNull(player);
		Valid.checkBoolean(packet instanceof PacketContainer, "Packet must be instance of PacketContainer from ProtocolLib");

		try {
			manager.sendServerPacket(player, (PacketContainer) packet);

		} catch (final InvocationTargetException e) {
			Common.error(e, "Failed to send " + ((PacketContainer) packet).getType() + " packet to " + player.getName());
		}
	}
}

class VaultHook {

	private Chat chat;
	private Economy economy;
	private Permission permissions;

	VaultHook() {
		setIntegration();
	}

	void setIntegration() {
		final RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		final RegisteredServiceProvider<Chat> chatProvider = Bukkit.getServicesManager().getRegistration(Chat.class);
		final RegisteredServiceProvider<Permission> permProvider = Bukkit.getServicesManager().getRegistration(Permission.class);

		if (economyProvider != null)
			economy = economyProvider.getProvider();

		if (chatProvider != null)
			chat = chatProvider.getProvider();

		if (permProvider != null)
			permissions = permProvider.getProvider();
	}

	boolean isChatIntegrated() {
		return chat != null;
	}

	boolean isEconomyIntegrated() {
		return economy != null;
	}

	// ------------------------------------------------------------------------------
	// Economy
	// ------------------------------------------------------------------------------

	String getCurrencyNameSG() {
		return economy != null ? Common.getOrEmpty(economy.currencyNameSingular()) : "Money";
	}

	String getCurrencyNamePL() {
		return economy != null ? Common.getOrEmpty(economy.currencyNamePlural()) : "Money";
	}

	double getBalance(Player player) {
		return economy != null ? economy.getBalance(player) : -1;
	}

	void withdraw(Player player, double amount) {
		if (economy != null)
			economy.withdrawPlayer(player, amount);
	}

	void deposit(Player player, double amount) {
		if (economy != null)
			economy.depositPlayer(player, amount);
	}

	// ------------------------------------------------------------------------------
	// Permissions
	// ------------------------------------------------------------------------------

	Boolean hasPerm(@NonNull String player, String perm) {
		return permissions != null ? perm != null ? permissions.has((String) null, player, perm) : true : null;
	}

	Boolean hasPerm(@NonNull String world, @NonNull String player, String perm) {
		return permissions != null ? perm != null ? permissions.has(world, player, perm) : true : null;
	}

	String getPrimaryGroup(Player player) {
		return permissions != null ? permissions.getPrimaryGroup(player) : "";
	}

	// ------------------------------------------------------------------------------
	// Prefix / Suffix
	// ------------------------------------------------------------------------------

	enum Chcem {
		PREFIX,
		SUFFIX,
		GROUP,
	}

	String getPlayerPrefix(Player pl) {
		return lookupVault(pl, Chcem.PREFIX);
	}

	String getPlayerSuffix(Player pl) {
		return lookupVault(pl, Chcem.SUFFIX);
	}

	String getPlayerGroup(Player pl) {
		return lookupVault(pl, Chcem.GROUP);
	}

	private String lookupVault(Player pl, Chcem co) {
		if (chat == null)
			return "";

		final String[] skupiny = chat.getPlayerGroups(pl);
		String fallback = co == Chcem.PREFIX ? chat.getPlayerPrefix(pl) : co == Chcem.SUFFIX ? chat.getPlayerSuffix(pl) : skupiny != null && skupiny.length > 0 ? skupiny[0] : "";

		if (fallback == null)
			fallback = "";

		if (co == Chcem.PREFIX && !SimplePlugin.getInstance().vaultMultiPrefix() || co == Chcem.SUFFIX && !SimplePlugin.getInstance().vaultMultiSuffix())
			return fallback;

		final List<String> list = new ArrayList<>();

		if (!fallback.isEmpty())
			list.add(fallback);

		if (skupiny != null)
			for (final String group : skupiny) {
				final String part = co == Chcem.PREFIX ? chat.getGroupPrefix(pl.getWorld(), group) : co == Chcem.SUFFIX ? chat.getGroupSuffix(pl.getWorld(), group) : group;

				if (part != null && !part.isEmpty() && !list.contains(part))
					list.add(part);
			}

		return StringUtils.join(list, co == Chcem.GROUP ? ", " : "");
	}
}

class PlaceholderAPIHook {

	private final Set<PAPIPlaceholder> placeholders = new HashSet<>();

	PlaceholderAPIHook() {
		new VariablesInjector().register();
	}

	final void addPlaceholder(PAPIPlaceholder placeholder, Function<Player, String> replacer) {
		placeholders.add(placeholder);

		if (replacer != null)
			PlaceholderAPI.registerPlaceholderHook(SimplePlugin.getNamed().toLowerCase(), new PlaceholderHook() {
				@Override
				public String onPlaceholderRequest(Player player, String params) {

					if (params.equals(placeholder.getVariable()))
						return replacer.apply(player);

					return null;
				}
			});
	}

	final String replacePlaceholders(Player pl, String msg) {
		try {
			return setPlaceholders(pl, msg);

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace variables!",
					"Player: " + pl.getName(),
					"Message: " + msg,
					"Error: %error");

			return msg;
		}
	}

	private final String setPlaceholders(Player player, String text) {
		final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();

		if (hooks.isEmpty())
			return text;

		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(text);

		while (matcher.find()) {
			final String format = matcher.group(1);
			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index).toLowerCase();
			final String params = format.substring(index + 1);

			if (hooks.containsKey(identifier)) {
				final String value = hooks.get(identifier).onRequest(player, params);

				if (value != null)
					text = text.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement(Common.colorize(value)));
			}
		}

		return text;
	}

	final String replaceRelationPlaceholders(Player one, Player two, String msg) {
		try {
			return setRelationalPlaceholders(one, two, msg);

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace relation variables!",
					"Player one: " + one,
					"Player two: " + two,
					"Message: " + msg,
					"Error: %error");

			return msg;
		}
	}

	private final String setRelationalPlaceholders(Player one, Player two, String text) {
		final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();

		if (hooks.isEmpty())
			return text;

		final Matcher m = Variables.BRACKET_REL_PLACEHOLDER_PATTERN.matcher(text);

		while (m.find()) {
			final String format = m.group(2);
			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index).toLowerCase();
			final String params = format.substring(index + 1);

			if (hooks.containsKey(identifier)) {
				if (!(hooks.get(identifier) instanceof Relational))
					continue;

				final Relational rel = (Relational) hooks.get(identifier);
				final String value = one != null && two != null ? rel.onPlaceholderRequest(one, two, params) : "";

				if (value != null)
					text = text.replaceAll(Pattern.quote(m.group()), Matcher.quoteReplacement(Common.colorize(value)));
			}
		}

		return text;
	}

	private class VariablesInjector extends PlaceholderExpansion {

		/**
		 * Because this is an internal class,
		 * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
		 * PlaceholderAPI is reloaded
		 *
		 * @return true to persist through reloads
		 */
		@Override
		public boolean persist() {
			return true;
		}

		/**
		 * Because this is a internal class, this check is not needed
		 * and we can simply return {@code true}
		 *
		 * @return Always true since it's an internal class.
		 */
		@Override
		public boolean canRegister() {
			return true;
		}

		/**
		 * The name of the person who created this expansion should go here.
		 * <br>For convienience do we return the author from the plugin.yml
		 *
		 * @return The name of the author as a String.
		 */
		@Override
		public String getAuthor() {
			return SimplePlugin.getInstance().getDescription().getAuthors().toString();
		}

		/**
		 * The placeholder identifier should go here.
		 * <br>This is what tells PlaceholderAPI to call our onRequest
		 * method to obtain a value if a placeholder starts with our
		 * identifier.
		 * <br>This must be unique and can not contain % or _
		 *
		 * @return The identifier in {@code %<identifier>_<value>%} as String.
		 */
		@Override
		public String getIdentifier() {
			return SimplePlugin.getNamed().toLowerCase().replace("%", "").replace(" ", "").replace("_", "");
		}

		/**
		 * This is the version of the expansion.
		 * <br>You don't have to use numbers, since it is set as a String.
		 *
		 * For convenience do we return the version from the plugin.yml
		 *
		 * @return The version as a String.
		 */
		@Override
		public String getVersion() {
			return SimplePlugin.getInstance().getDescription().getVersion();
		}

		/**
		 * This is the method called when a placeholder with our identifier
		 * is found and needs a value.
		 * <br>We specify the value identifier in this method.
		 * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
		 *
		 * @param  player
		 *         A {@link org.bukkit.Player Player}.
		 * @param  identifier
		 *         A String containing the identifier/value.
		 *
		 * @return possibly-null String of the requested identifier.
		 */
		@Override
		public String onPlaceholderRequest(Player player, String identifier) {

			if (player == null)
				return "";

			final boolean insertSpace = identifier.endsWith("+");
			identifier = insertSpace ? identifier.substring(0, identifier.length() - 1) : identifier;

			for (final PAPIPlaceholder replacer : placeholders)
				if (identifier.equalsIgnoreCase(replacer.getVariable()))
					try {
						final String value = Common.getOrEmpty(replacer.getValue().apply(player, identifier));

						return value + (!value.isEmpty() && insertSpace ? " " : "");

					} catch (final Exception e) {
						Common.error(e, "Failed to replace your '" + identifier + "' variable for " + player.getName());
					}

			// We return null if an invalid placeholder (f.e. %someplugin_placeholder3%) was provided
			return null;
		}
	}
}

class NickyHook {

	NickyHook() {
	}

	String getNick_(Player player) {
		final Constructor<?> nickConstructor = ReflectionUtil.getConstructor("io.loyloy.nicky.Nick", Player.class);
		final Object nick = ReflectionUtil.instantiate(nickConstructor, player);
		String nickname = ReflectionUtil.invoke("get", nick);

		if (nickname != null) {
			final Method formatMethod = ReflectionUtil.getMethod(nick.getClass(), "format", String.class);

			nickname = ReflectionUtil.invoke(formatMethod, nick, nickname);
		}

		return nickname != null && !nickname.isEmpty() ? nickname : null;
	}
}

class MVdWPlaceholderHook {

	MVdWPlaceholderHook() {
	}

	String replacePlaceholders(Player player, String message) {
		try {
			final String replaced = be.maximvdw.placeholderapi.PlaceholderAPI.replacePlaceholders(player, message);

			return replaced == null ? "" : replaced;

		} catch (final IllegalArgumentException ex) {
			if (!Common.getOrEmpty(ex.getMessage()).contains("Illegal group reference"))
				ex.printStackTrace();

		} catch (final Throwable t) {
			Common.error(t,
					"MvdWPlaceholders placeholders failed!",
					"Player: " + player.getName(),
					"Message: '" + message + "'",
					"Consider writing to developer of that library",
					"first as this may be a bug we cannot handle!",
					"",
					"Your chat message will appear without replacements.");
		}

		return message;
	}
}

class LWCHook {

	String getOwner(Block block) {
		if (!LWC.ENABLED)
			return null;

		final Protection protection = LWC.getInstance().findProtection(block);

		if (protection != null) {
			final String uuid = protection.getOwner();

			if (uuid != null) {
				final OfflinePlayer opl = Remain.getOfflinePlayerByUUID(UUID.fromString(uuid));

				if (opl != null)
					return opl.getName();
			}
		}

		return null;
	}
}

class LocketteProHook {

	boolean isOwner(Block block, Player player) {
		return LocketteProAPI.isProtected(block) ? LocketteProAPI.isOwner(block, player) : false;
	}
}

class ResidenceHook {

	public Collection<String> getResidences() {
		return Residence.getInstance().getResidenceManager().getResidences().keySet();
	}

	public String getResidence(Location loc) {
		final ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);

		if (res != null)
			return res.getName();

		return null;
	}

	public String getResidenceOwner(Location loc) {
		final ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);

		if (res != null)
			return res.getOwner();

		return null;
	}
}

class WorldEditHook {

	public final boolean legacy;

	public WorldEditHook() {
		boolean ok = false;
		try {
			Class.forName("com.sk89q.worldedit.world.World");
			ok = true;
		} catch (final ClassNotFoundException e) {
		}

		legacy = !ok;
	}
}

class WorldGuardHook {

	private final boolean legacy;

	public WorldGuardHook(WorldEditHook we) {
		final Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");

		legacy = !wg.getDescription().getVersion().startsWith("7") || we != null && we.legacy;
	}

	public List<String> getRegionsAt(Location loc) {
		final List<String> list = new ArrayList<>();

		getApplicableRegions(loc).forEach((reg) -> {
			final String name = Common.stripColors(reg.getId());

			if (!name.startsWith("__"))
				list.add(name);
		});

		return list;
	}

	public Region getRegion(String name) {
		for (final World w : Bukkit.getWorlds()) {
			final Object rm = getRegionManager(w);
			if (legacy) {
				try {

					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;

						if (Common.stripColors(((ProtectedRegion) regObj).getId()).equals(name)) {

							final Class<? extends Object> clazz = regObj.getClass();
							final Method getMax = clazz.getMethod("getMaximumPoint");
							final Method getMin = clazz.getMethod("getMinimumPoint");

							final Object regMax = getMax.invoke(regObj);
							final Object regMin = getMin.invoke(regObj);

							final Class<?> vectorClass = Class.forName("com.sk89q.worldedit.BlockVector");
							final Method getX = vectorClass.getMethod("getX");
							final Method getY = vectorClass.getMethod("getY");
							final Method getZ = vectorClass.getMethod("getZ");

							Location locMax, locMin;
							locMax = new Location(w, (Double) getX.invoke(regMax), (Double) getY.invoke(regMax), (Double) getZ.invoke(regMax));
							locMin = new Location(w, (Double) getX.invoke(regMin), (Double) getY.invoke(regMin), (Double) getZ.invoke(regMin));

							return new Region(name, locMin, locMax);
						}
					}

				} catch (final Throwable t) {
					t.printStackTrace();

					throw new FoException("Failed WorldEdit 6 legacy hook, see above & report");
				}
			} else {
				for (final ProtectedRegion reg : ((com.sk89q.worldguard.protection.managers.RegionManager) rm).getRegions().values()) {
					if (reg != null && reg.getId() != null && Common.stripColors(reg.getId()).equals(name)) {
						//if(reg instanceof com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion) {
						// just going to pretend that everything is a cuboid..
						Location locMax, locMin;
						final com.sk89q.worldedit.math.BlockVector3 regMax = reg.getMaximumPoint();
						final com.sk89q.worldedit.math.BlockVector3 regMin = reg.getMinimumPoint();

						locMax = new Location(w, regMax.getX(), regMax.getY(), regMax.getZ());
						locMin = new Location(w, regMin.getX(), regMin.getY(), regMin.getZ());

						return new Region(name, locMin, locMax);
					}
				}
			}
		}
		return null;
	}

	public List<String> getAllRegions() {
		final List<String> list = new ArrayList<>();

		for (final World w : Bukkit.getWorlds()) {
			final Object rm = getRegionManager(w);
			if (legacy) {
				try {
					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					Method getId = null;
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;
						if (getId == null)
							getId = regObj.getClass().getMethod("getId");

						final String name = Common.stripColors(getId.invoke(regObj).toString());

						if (!name.startsWith("__"))
							list.add(name);
					}
				} catch (final Throwable t) {
					t.printStackTrace();

					throw new FoException("Failed WorldEdit 6 legacy hook, see above & report");
				}
			} else
				((com.sk89q.worldguard.protection.managers.RegionManager) rm)
						.getRegions().values().forEach((reg) -> {
							if (reg == null || reg.getId() == null)
								return;

							final String name = Common.stripColors(reg.getId());

							if (!name.startsWith("__"))
								list.add(name);
						});
		}

		return list;
	}

	private Iterable<ProtectedRegion> getApplicableRegions(Location loc) {
		final Object rm = getRegionManager(loc.getWorld());

		if (legacy)
			try {
				return (Iterable<ProtectedRegion>) rm.getClass().getMethod("getApplicableRegions", Location.class).invoke(rm, loc);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldEdit 6 legacy hook, see above & report");
			}

		return ((com.sk89q.worldguard.protection.managers.RegionManager) rm)
				.getApplicableRegions(com.sk89q.worldedit.math.BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
	}

	private Object getRegionManager(World w) {
		if (legacy)
			try {
				return Class.forName("com.sk89q.worldguard.bukkit.WGBukkit").getMethod("getRegionManager", World.class).invoke(null, w);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldGuard 6 legacy hook, see above & report");
			}

		// causes class errors..
		//return com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(new com.sk89q.worldedit.bukkit.BukkitWorld(w));
		// dynamically load modern WE
		try {

			final Class<?> bwClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld");
			final Constructor<?> bwClassNew = bwClass.getConstructor(World.class);

			Object t = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null);
			t = t.getClass().getMethod("getPlatform").invoke(t);
			t = t.getClass().getMethod("getRegionContainer").invoke(t);
			return t.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World")).invoke(t, bwClassNew.newInstance(w));

		} catch (final Throwable t) {
			t.printStackTrace();

			throw new FoException("Failed WorldGuard hook, see above & report");
		}
	}
}

abstract class FactionsHook {

	/** Get all loaded factions */
	abstract Collection<String> getFactions();

	/** Get faction of the player */
	abstract String getFaction(Player pl);

	/** Get faction in the location */
	abstract String getFaction(Location loc);

	/** Get faction owner at the specific location */
	abstract String getFactionOwner(Location loc);

	/** Get all players being in the same faction, used for party chat. */
	final Collection<? extends Player> getSameFactionPlayers(Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playerFaction = getFaction(pl);

		if (playerFaction != null && !"".equals(playerFaction))
			Remain.getOnlinePlayers().forEach((online) -> {
				if (playerFaction.equals(getFaction(online)))
					recipients.add(online);
			});

		return recipients;
	}
}

final class FactionsMassive extends FactionsHook {

	FactionsMassive() {
	}

	@Override
	public Collection<String> getFactions() {
		return Common.convert(com.massivecraft.factions.entity.FactionColl.get().getAll(), object -> Common.stripColors(object.getName()));
	}

	@Override
	public String getFaction(Player pl) {
		try {
			return MPlayer.get(pl.getUniqueId()).getFactionName();
		} catch (final Exception ex) {
			return null;
		}
	}

	@Override
	public String getFaction(Location loc) {
		final Faction f = BoardColl.get().getFactionAt(PS.valueOf(loc));

		if (f != null)
			return f.getName();

		return null;
	}

	@Override
	public String getFactionOwner(Location loc) {
		final Faction f = BoardColl.get().getFactionAt(PS.valueOf(loc));

		if (f != null)
			return f.getLeader() != null ? f.getLeader().getName() : null;

		return null;
	}
}

final class FactionsUUID extends FactionsHook {

	@Override
	public Collection<String> getFactions() {
		try {
			final Object i = instance();
			final Set<String> tags = (Set<String>) i.getClass().getMethod("getFactionTags").invoke(i);

			return tags;
		} catch (final Throwable t) {
			t.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFaction(Player pl) {
		try {
			final Object fplayers = fplayers();
			final Object fpl = fplayers.getClass().getMethod("getByPlayer", Player.class).invoke(fplayers, pl);
			final Object f = fpl != null ? fpl.getClass().getMethod("getFaction").invoke(fpl) : null;
			final Object name = f != null ? f.getClass().getMethod("getTag").invoke(f) : null;

			return name != null ? name.toString() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFaction(Location loc) {
		final Object f = findFaction(loc);

		try {
			return f != null ? f.getClass().getMethod("getTag").invoke(f).toString() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFactionOwner(Location loc) {
		final Object faction = findFaction(loc);

		try {
			return faction != null ? ((com.massivecraft.factions.FPlayer) faction.getClass().getMethod("getFPlayerAdmin").invoke(faction)).getName() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	private Object findFaction(Location loc) {
		final Class<com.massivecraft.factions.Board> b = com.massivecraft.factions.Board.class;

		try {
			return b.getMethod("getFactionAt", com.massivecraft.factions.FLocation.class).invoke(b.getMethod("getInstance").invoke(null), new com.massivecraft.factions.FLocation(loc));
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	private Object instance() {
		try {
			return Class.forName("com.massivecraft.factions.Factions").getDeclaredMethod("getInstance").invoke(null);
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			throw new FoException(ex);
		}
	}

	private Object fplayers() {
		try {
			return Class.forName("com.massivecraft.factions.FPlayers").getDeclaredMethod("getInstance").invoke(null);
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			throw new FoException(ex);
		}
	}
}

class McMMOHook {
	// Empty

	String getActivePartyChat(Player player) {
		final McMMOPlayer mcplayer = UserManager.getPlayer(player);

		if (mcplayer == null)
			return null;

		final Party party = mcplayer.getParty();

		return mcplayer.isChatEnabled(ChatMode.PARTY) && party != null ? party.getName() : null;
	}
}

class PlotSquaredHook {

	List<Player> getPlotPlayers(Player player) {
		final List<Player> players = new ArrayList<>();

		final PlotPlayer plotPlayer = PlotPlayer.wrap(player);
		Valid.checkNotNull(plotPlayer, "Failed to convert player " + player.getName() + " to PlotPlayer!");

		final Plot currentPlot = plotPlayer.getCurrentPlot();

		if (currentPlot != null)
			for (final PlotPlayer playerInPlot : currentPlot.getPlayersInPlot()) {
				final UUID id = playerInPlot.getUUID();

				final Player online = Bukkit.getPlayer(id);

				if (online != null && online.isOnline()) // PlotSquared sometimes returns offline players as well, so just ignore them
					players.add(online);
			}

		return players;
	}
}

class CMIHook {

	boolean isVanished(Player player) {
		final CMIUser user = getUser(player);

		return user == null ? false : user.isVanished();
	}

	boolean isAfk(Player player) {
		final CMIUser user = getUser(player);

		return user == null ? false : user.isAfk();
	}

	boolean isMuted(Player player) {
		final CMIUser user = getUser(player);

		return user == null ? false : user.isMuted();
	}

	String getNick_(Player player) {
		final CMIUser user = getUser(player);
		final String nick = user == null ? null : user.getNickName();

		return nick == null || "".equals(nick) ? null : nick;
	}

	void setGodMode(Player player, boolean godMode) {
		final CMIUser user = getUser(player);

		user.setGod(godMode);
	}

	void setLastTeleportLocation(Player player, Location location) {
		final CMIUser user = getUser(player);

		user.setLastTeleportLocation(location);
	}

	void setIgnore(String player, String who, boolean ignore) {
		final CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
		final OfflinePlayer ignoredPlayer = Bukkit.getOfflinePlayer(who);

		if (ignoredPlayer != null)
			if (ignore)
				user.addIgnore(ignoredPlayer.getUniqueId(), true /* save now (?) */);
			else
				user.removeIgnore(ignoredPlayer.getUniqueId());
	}

	boolean isIgnoring(String player, String who) {
		try {
			final CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
			final OfflinePlayer ignoredPlayer = Bukkit.getOfflinePlayer(who);

			return user.isIgnoring(ignoredPlayer.getUniqueId());
		} catch (final NullPointerException ex) {
			return false;
		}
	}

	private CMIUser getUser(Player player) {
		return CMI.getInstance().getPlayerManager().getUser(player);
	}
}

class CitizensHook {

	boolean isNPC(Entity entity) {
		final NPCRegistry reg = CitizensAPI.getNPCRegistry();

		return reg != null ? reg.isNPC(entity) : false;
	}
}

class DiscordSRVHook implements Listener {

	Set<String> getChannels() {
		return DiscordSRV.getPlugin().getChannels().keySet();
	}

	boolean sendMessage(String sender, String channel, String message) {
		final DiscordSender discordSender = new DiscordSender(sender);

		return sendMessage(discordSender, channel, message);
	}

	boolean sendMessage(String channel, String message) {
		return sendMessage((CommandSender) null, channel, message);
	}

	boolean sendMessage(CommandSender sender, String channel, String message) {
		LagCatcher.start("Minecraft to Discord");

		final TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// Channel not configured in DiscordSRV config.yml, ignore
		if (textChannel == null) {
			Debugger.debug("discord", "[MC->Discord] Could not find Discord channel '" + channel + "'. Available: " + String.join(", ", getChannels()) + ". Not sending: " + message);

			LagCatcher.end("Minecraft to Discord", Debugger.isDebugged("discord") ? 0 : SimpleSettings.LAG_THRESHOLD_MILLIS, "Minecraft > Discord sending ended due to no channel. Took {time} ms");
			return false;
		}

		if (sender instanceof Player) {
			Debugger.debug("discord", "[MC->Discord] " + sender.getName() + " send message to '" + channel + "' channel. Message: '" + message + "'");

			final DiscordSRV instance = DiscordSRV.getPlugin(DiscordSRV.class);

			// Dirty: We have to temporarily unset value in DiscordSRV to enable the processChatMessage
			// method to function
			final File file = new File(SimplePlugin.getData().getParent(), "DiscordSRV/config.yml");

			if (file.exists()) {
				final FileConfiguration discordConfig = YamlConfiguration.loadConfiguration(file);

				if (discordConfig != null) {
					final String outMessageKey = "DiscordChatChannelMinecraftToDiscord";
					final boolean outMessageOldValue = discordConfig.getBoolean(outMessageKey);

					discordConfig.set(outMessageKey, true);

					try {
						instance.processChatMessage((Player) sender, message, channel, false);
					} finally {
						discordConfig.set(outMessageKey, outMessageOldValue);
					}
				}
			}

		} else {
			Debugger.debug("discord", "[MC->Discord] " + (sender == null ? "No given sender " : sender.getName() + " (generic)") + "sent message to '" + channel + "' channel. Message: '" + message + "'");

			DiscordUtil.sendMessage(textChannel, message);
		}

		LagCatcher.end("Minecraft to Discord", Debugger.isDebugged("discord") ? 0 : SimpleSettings.LAG_THRESHOLD_MILLIS, "Minecraft > Discord sending took {time} ms");
		return true;
	}
}