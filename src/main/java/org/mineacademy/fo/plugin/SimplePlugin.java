/**
 * 	(c) 2013 - 2019 - All rights reserved.
 *
 *	Do not share, copy, reproduce or sell any part of this library
 *	unless you have written permission from MineAcademy.org.
 *	All infringements will be prosecuted.
 *
 *	If you are the personal owner of the MineAcademy.org End User License
 *	then you may use it for your own use in plugins but not for any other purpose.
 */
package org.mineacademy.fo.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.SimpleBungee;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.event.SimpleListener;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuListener;
import org.mineacademy.fo.menu.tool.Rocket;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolsListener;
import org.mineacademy.fo.metrics.Metrics;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.EnchantmentListener;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleEnchantment;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.settings.YamlStaticConfig;
import org.mineacademy.fo.update.SpigotUpdater;
import org.mineacademy.fo.visualize.VisualizerListener;

import lombok.Getter;

/**
 * Represents a basic Java plugin using enhanced library functionality
 */
public abstract class SimplePlugin extends JavaPlugin implements Listener {

	// ----------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------

	/**
	 * An internal flag to indicate that the plugin is being reloaded.
	 */
	@Getter
	private static volatile boolean reloading = false;

	/**
	 * The instance of this plugin
	 */
	private static volatile SimplePlugin instance;

	/**
	 * Returns the instance of {@link SimplePlugin}.
	 *
	 * It is recommended to override this in your own {@link SimplePlugin}
	 * implementation so you will get the instance of that, directly.
	 * @param <T>
	 *
	 * @return this instance
	 */
	public static SimplePlugin getInstance() {
		if (instance == null) {
			instance = SimplePlugin.getPlugin(SimplePlugin.class);

			Objects.requireNonNull(instance, "Cannot get a new instance! Have you reloaded?");
		}

		return instance;
	}

	/**
	 * Shortcut for getDescription().getVersion()
	 *
	 * @return plugin's version
	 */
	public static final String getVersion() {
		return getInstance().getDescription().getVersion();
	}

	/**
	 * Shortcut for getName()
	 *
	 * @return plugin's name
	 */
	public static final String getNamed() {
		return hasInstance() ? getInstance().getName() : "No instance yet";
	}

	/**
	 * Shortcut for getFile()
	 *
	 * @return plugin's jar file
	 */
	public static final File getSource() {
		return getInstance().getFile();
	}

	/**
	 * Shortcut for getDataFolder()
	 *
	 * @return plugins' data folder in plugins/
	 */
	public static final File getData() {
		return getInstance().getDataFolder();
	}

	/**
	 * Return true if the {@link #getMainCommand()} is registered and its label
	 * equals to the given label
	 *
	 * @param label
	 * @return
	 */
	public static final boolean isMainCommand(String label) {
		return getInstance().getMainCommand() != null && getInstance().getMainCommand().getLabel().equals(label);
	}

	/**
	 * Return the BungeeCord setup or null if not present
	 *
	 * @return
	 */
	public static final SimpleBungee getBungee() {
		return getInstance().getBungeeCord();
	}

	/**
	 * Get if the instance that is used across the library has been set. Normally it
	 * is always set, except for testing.
	 *
	 * @return if the instance has been set.
	 */
	public static final boolean hasInstance() {
		return instance != null;
	}

	// ----------------------------------------------------------------------------------------
	// Instance specific
	// ----------------------------------------------------------------------------------------

	/**
	 * Is this plugin enabled? Checked for after {@link #onPluginPreStart()}
	 */
	protected boolean isEnabled = true;

	/**
	 * For your convenience, event listeners and timed tasks may be set here to stop/unregister
	 * them automatically on reload
	 */
	private final Reloadables reloadables = new Reloadables();

	/**
	 * An internal flag to indicate whether we are calling the {@link #onReloadablesStart()}
	 * block. We register things using {@link #reloadables} during this block
	 */
	private boolean startingReloadables = false;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onLoad() {

		// Set the instance
		getInstance();

		// Call parent
		onPluginLoad();
	}

	@Override
	public final void onEnable() {

		// Check if Foundation is correctly moved
		checkShading();

		if (!isEnabled)
			return;

		// Before all, check if necessary libraries and the minimum required MC version
		if (!checkLibraries0() || !checkServerVersions0()) {
			isEnabled = false;
			setEnabled(false);

			return;
		}

		// Load debug mode early
		Debugger.detectDebugMode();

		// Disable logging prefix if logo is set
		if (getStartupLogo() != null)
			Common.ADD_LOG_PREFIX = false;

		// --------------------------------------------
		// Call the main pre start method
		// --------------------------------------------
		onPluginPreStart();
		// --------------------------------------------

		// Return if plugin pre start indicated a fatal problem
		if (!isEnabled || !isEnabled())
			return;

		if (getStartupLogo() != null) {
			final boolean hadLogPrefix = Common.ADD_LOG_PREFIX;
			Common.ADD_LOG_PREFIX = false;
			Common.log(getStartupLogo());
			Common.ADD_LOG_PREFIX = hadLogPrefix;
		}

		try {

			// Load our main static settings classes
			if (getSettings() != null) {
				YamlStaticConfig.load(getSettings());
				Valid.checkBoolean(SimpleSettings.isSettingsCalled() != null && SimpleLocalization.isLocalizationCalled() != null, "Developer forgot to call Settings or Localization");
			}

			if (!isEnabled || !isEnabled())
				return;

			// Register classes
			checkSingletons();

			// Load our dependency system
			HookManager.loadDependencies();

			if (!isEnabled || !isEnabled())
				return;

			// Load legacy permanent metadata store
			CompMetadata.MetadataFile.getInstance();

			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------
			startingReloadables = true;
			onReloadablesStart();
			startingReloadables = false;

			onPluginStart();
			// --------------------------------------------

			// Return if plugin start indicated a fatal problem
			if (!isEnabled || !isEnabled())
				return;

			// Register main command if it is set
			if (getMainCommand() != null) {
				Valid.checkBoolean(!SimpleSettings.MAIN_COMMAND_ALIASES.isEmpty(), "Please make a settings class extending SimpleSettings and specify Command_Aliases in your settings file.");

				getMainCommand().register(SimpleSettings.MAIN_COMMAND_ALIASES);
			}

			// Register BungeeCord when used
			registerBungeeCord();

			// Start update check
			if (getUpdateCheck() != null)
				getUpdateCheck().run();

			// Register our listeners
			registerEvents(this); // For convenience
			registerEvents(new MenuListener());
			registerEvents(new FoundationListener());
			registerEvents(new ToolsListener());
			registerEvents(new VisualizerListener());
			registerEvents(new EnchantmentListener());

			// Register our packet listener
			FoundationPacketListener.addPacketListener();

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded())
				reloadables.registerEvents(new DiscordListener.DiscordListenerImpl());

			// Load variables if enabled
			if (areScriptVariablesEnabled())
				Variables.loadScriptVariables();

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			// Finish off by starting metrics (currently bStats)
			new Metrics(this);

		} catch (final Throwable t) {
			displayError0(t);
		}
	}

	/**
	 * Convenience method for registering channels to BungeeCord
	 *
	 * @param channel
	 */
	private final void registerBungeeCord() {
		final Messenger messenger = getServer().getMessenger();
		final SimpleBungee bungee = getBungeeCord();

		if (bungee != null) {
			messenger.registerIncomingPluginChannel(this, bungee.getChannel(), bungee.getListener());
			messenger.registerOutgoingPluginChannel(this, bungee.getChannel());

			reloadables.registerEvents(bungee.getListener());
			Debugger.debug("bungee", "Registered BungeeCord listener for " + bungee.getChannel());
		}
	}

	/**
	 * Scans your plugin and if your {@link Tool} or {@link SimpleEnchantment} class implements {@link Listener}
	 * and has "instance" method to be a singleton, your events are registered there automatically
	 *
	 * If not, we only call the instance constructor in case there is any underlying registration going on
	 */
	private static void checkSingletons() {

		try (final JarFile file = new JarFile(SimplePlugin.getSource())) {
			for (final Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements();) {
				final JarEntry jar = entry.nextElement();
				final String name = jar.getName().replace("/", ".");

				if (name.endsWith(".class")) {
					final String className = name.substring(0, name.length() - 6);
					Class<?> clazz = null;

					try {
						clazz = SimplePlugin.class.getClassLoader().loadClass(className);
					} catch (final NoClassDefFoundError | ClassNotFoundException error) {
						continue;
					}

					final boolean isTool = Tool.class.isAssignableFrom(clazz) && !Tool.class.equals(clazz) && !Rocket.class.equals(clazz);
					final boolean isEnchant = SimpleEnchantment.class.isAssignableFrom(clazz) && !SimpleEnchantment.class.equals(clazz);

					if (isTool || isEnchant) {

						if (isEnchant && MinecraftVersion.olderThan(V.v1_13)) {
							System.out.println("**** WARNING ****");
							System.out.println("SimpleEnchantment requires Minecraft 1.13.2 or greater. The following class will not be registered: " + clazz.getName());

							continue;
						}

						try {
							Field instanceField = null;

							for (final Field field : clazz.getDeclaredFields()) {
								if ((Tool.class.isAssignableFrom(field.getType()) || Enchantment.class.isAssignableFrom(field.getType()))
										&& Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
									instanceField = field;
							}

							if (SimpleEnchantment.class.isAssignableFrom(clazz))
								Valid.checkNotNull(instanceField, "Your enchant class " + clazz.getSimpleName() + " must be a singleton and have static 'instance' field and private constructors!");

							if (instanceField != null) {
								instanceField.setAccessible(true);

								final Object instance = instanceField.get(null);

								// Enforce private constructors
								for (final Constructor<?> con : instance.getClass().getDeclaredConstructors())
									Valid.checkBoolean(Modifier.isPrivate(con.getModifiers()), "Constructor " + con + " not private! Did you put '@NoArgsConstructor(access = AccessLevel.PRIVATE)' in your tools class?");

								// Finally register events
								if (instance instanceof Listener)
									Common.registerEvents((Listener) instance);
							}

						} catch (final NoSuchFieldError ex) {
							// Ignore if no field is present

						} catch (final Throwable t) {
							final String error = Common.getOrEmpty(t.getMessage());

							if (t instanceof NoClassDefFoundError && error.contains("org/bukkit/entity")) {
								System.out.println("**** WARNING ****");

								if (error.contains("DragonFireball"))
									System.out.println("Your Minecraft version does not have DragonFireball class, we suggest replacing it with a Fireball instead in: " + clazz);
								else
									System.out.println("Your Minecraft version does not have " + error + " class you call in: " + clazz);
							} else
								Common.error(t, "Failed to register events in " + clazz.getSimpleName() + " class " + clazz);
						}
					}
				}
			}
		} catch (final Throwable t) {
			Common.error(t, "Failed to auto register events using Foundation!");
		}
	}

	/**
	 * A dirty way of checking if Foundation has been shaded correctly
	 */
	private final void checkShading() {
		try {
			throw new ShadingException();
		} catch (final Throwable t) {
		}
	}

	/**
	 * The exception enabling us to check if for some reason {@link SimplePlugin}'s instance
	 * does not match this class' instance, which is most likely caused by wrong repackaging
	 * or no repackaging at all (two plugins using Foundation must both have different packages
	 * for their own Foundation version).
	 *
	 * Or, this is caused by a PlugMan, and we have no mercy for that.
	 */
	private final class ShadingException extends Throwable {
		private static final long serialVersionUID = 1L;

		public ShadingException() {
			if (!SimplePlugin.getNamed().equals(getDescription().getName())) {
				System.out.println(Common.consoleLine());
				System.out.println("We have a class path problem in the Foundation library");
				System.out.println("preventing " + getDescription().getName() + " from loading correctly!");
				System.out.println("");
				System.out.println("This is likely caused by two plugins having the");
				System.out.println("same Foundation library paths - make sure you");
				System.out.println("relocale the package! If you are testing using");
				System.out.println("Ant, only test one plugin at the time.");
				System.out.println("");
				System.out.println("Possible cause: " + SimplePlugin.getNamed());
				System.out.println("Foundation package: " + SimplePlugin.class.getPackage().getName());
				System.out.println(Common.consoleLine());

				isEnabled = false;
			}
		}
	}

	/**
	 * Check if both md5 chat and gson libraries are present,
	 * or suggest an additional plugin to fix their lack
	 *
	 * @return
	 */
	private final boolean checkLibraries0() {
		boolean md_5 = false;
		boolean gson = false;

		try {
			Class.forName("net.md_5.bungee.api.chat.BaseComponent");
			md_5 = true;
		} catch (final ClassNotFoundException ex) {
		}

		try {
			Class.forName("com.google.gson.JsonSyntaxException");
			gson = true;

		} catch (final ClassNotFoundException ex) {
		}

		if (!md_5 || !gson) {
			System.out.println(Common.consoleLine());
			System.out.println("Your Minecraft version (" + MinecraftVersion.getCurrent() + ")");
			System.out.println("lacks libraries " + getName() + " needs:");
			System.out.println("JSON Chat (by md_5) found: " + md_5);
			System.out.println("Gson (by Google) found: " + gson);
			System.out.println(" ");
			System.out.println("To fix that, please install BungeeChatAPI:");
			System.out.println("https://www.spigotmc.org/resources/38379/");
			System.out.println(Common.consoleLine());

			return false;
		}

		return true;
	}

	/**
	 * Check if the minimum required MC version is installed
	 *
	 * @return
	 */
	private final boolean checkServerVersions0() {

		// Call the static block to test compatibility early
		if (!MinecraftVersion.getCurrent().isTested()) {
			System.out.println(Common.consoleLine());
			System.out.println("** Your Minecraft version " + MinecraftVersion.getCurrent() + " has not yet");
			System.out.println("been officialy tested with the Foundation");
			System.out.println("library that powers the " + SimplePlugin.getNamed() + " plugin.");
			System.out.println(" ");
			System.out.println("For your safety, the plugin is now disabled.");
			System.out.println(Common.consoleLine());

			return false;
		}

		// Check min version
		final V minimumVersion = getMinimumVersion();

		if (minimumVersion != null && MinecraftVersion.olderThan(minimumVersion)) {
			Common.logFramed(false,
					getName() + " requires Minecraft " + minimumVersion + " or newer to run.",
					"Please upgrade your server.");

			return false;
		}

		// Check max version
		final V maximumVersion = getMaximumVersion();

		if (maximumVersion != null && MinecraftVersion.newerThan(maximumVersion)) {
			Common.logFramed(false,
					getName() + " requires Minecraft " + maximumVersion + " or older to run.",
					"Please downgrade your server or",
					"wait for the new version.");

			return false;
		}

		return true;
	}

	/**
	 * Handles various startup problems
	 *
	 * @param throwable
	 */
	protected final void displayError0(Throwable throwable) {
		Debugger.printStackTrace(throwable);

		Common.log(
				"&c  _   _                       _ ",
				"&c  | | | | ___   ___  _ __  ___| |",
				"&c  | |_| |/ _ \\ / _ \\| '_ \\/ __| |",
				"&c  |  _  | (_) | (_) | |_) \\__ \\_|",
				"&4  |_| |_|\\___/ \\___/| .__/|___(_)",
				"&4                    |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + getDescription().getName() + " v" + getDescription().getVersion() + ", plugin is disabled!",
				" &cRunning on " + getServer().getBukkitVersion() + " (" + MinecraftVersion.getServerVersion() + ") & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		if (throwable instanceof InvalidConfigurationException) {
			Common.log(" &cSeems like your config is not a valid YAML.");
			Common.log(" &cUse online services like");
			Common.log(" &chttp://yaml-online-parser.appspot.com/");
			Common.log(" &cto check for syntax errors!");

		} else if (throwable instanceof UnsupportedOperationException || throwable.getCause() != null && throwable.getCause() instanceof UnsupportedOperationException)
			if (getServer().getBukkitVersion().startsWith("1.2.5"))
				Common.log(" &cSorry but Minecraft 1.2.5 is no longer supported!");
			else {
				Common.log(" &cUnable to setup reflection!");
				Common.log(" &cYour server is either too old or");
				Common.log(" &cthe plugin broke on the new version :(");
			}

		{
			while (throwable.getCause() != null)
				throwable = throwable.getCause();

			String error = "Unable to get the error message, search above.";
			if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
				error = throwable.getMessage();

			Common.log(" &cError: " + error);
		}
		Common.log("&4!-----------------------------------------------------!");

		getPluginLoader().disablePlugin(this);
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {

		// If the early startup was interrupted, do not call shutdown methods
		if (!isEnabled)
			return;

		try {
			onPluginStop();
		} catch (final Throwable t) {
			Common.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		unregisterReloadables();

		try {
			for (final Player online : Remain.getOnlinePlayers())
				SimpleScoreboard.clearBoardsFor(online);

		} catch (final Throwable t) {
			Common.log("Error clearing scoreboards for players..");

			t.printStackTrace();
		}

		try {
			for (final Player online : Remain.getOnlinePlayers()) {
				final Menu menu = Menu.getMenu(online);

				if (menu != null)
					online.closeInventory();
			}
		} catch (final Throwable t) {
			Common.log("Error closing menu inventories for players..");

			t.printStackTrace();
		}

		Objects.requireNonNull(instance, "Instance of " + getName() + " already nulled!");
		instance = null;
	}

	// ----------------------------------------------------------------------------------------
	// Delegate methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Called before the plugin is started, see {@link JavaPlugin#onLoad()}
	 */
	protected void onPluginLoad() {
	}

	/**
	 * Called before we start loading the plugin, but after {@link #onPluginLoad()}
	 */
	protected void onPluginPreStart() {
	}

	/**
	 * The main loading method, called when we are ready to load
	 */
	protected abstract void onPluginStart();

	/**
	 * The main method called when we are about to shut down
	 */
	protected void onPluginStop() {
	}

	/**
	 * Invoked before settings were reloaded.
	 */
	protected void onPluginPreReload() {
	}

	/**
	 * Invoked after settings were reloaded.
	 */
	protected void onPluginReload() {
	}

	/**
	 * Register your commands, events, tasks and files here.
	 *
	 * This is invoked when you start the plugin, call /reload, or the {@link #reload()}
	 * method.
	 */
	protected void onReloadablesStart() {
	}

	// ----------------------------------------------------------------------------------------
	// Reload
	// ----------------------------------------------------------------------------------------

	/**
	 * Attempts to reload the plugin
	 */
	public final void reload() {
		final boolean hadLogPrefix = Common.ADD_LOG_PREFIX;
		Common.ADD_LOG_PREFIX = false;

		Common.log(Common.consoleLineSmooth());
		Common.log(" ");
		Common.log("Reloading plugin " + this.getName() + " v" + getVersion());
		Common.log(" ");

		reloading = true;

		try {
			Debugger.detectDebugMode();

			unregisterReloadables();

			onPluginPreReload();
			reloadables.reload();

			YamlConfig.clearLoadedFiles();

			if (getSettings() != null)
				YamlStaticConfig.load(getSettings());

			if (areScriptVariablesEnabled())
				Variables.reloadScriptVariables();

			FoundationPacketListener.addPacketListener();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);
			onPluginReload();

			startingReloadables = true;
			onReloadablesStart();
			startingReloadables = false;

			if (getMainCommand() != null)
				getMainCommand().register(SimpleSettings.MAIN_COMMAND_ALIASES);

			registerBungeeCord();

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + getName() + " " + getVersion());

		} finally {
			Common.log(Common.consoleLineSmooth());

			Common.ADD_LOG_PREFIX = hadLogPrefix;
			reloading = false;
		}
	}

	private final void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		if (getMainCommand() != null && getMainCommand().isRegistered())
			getMainCommand().unregister();

		try {
			HookManager.removePacketListeners(this);
		} catch (final NoClassDefFoundError ex) {
		}

		getServer().getMessenger().unregisterIncomingPluginChannel(this);
		getServer().getMessenger().unregisterOutgoingPluginChannel(this);

		getServer().getScheduler().cancelTasks(this);
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly registering events if the condition is met
	 *
	 * @param listener
	 * @param condition
	 */
	protected final void registerEventsIf(Listener listener, boolean condition) {
		if (condition)
			if (startingReloadables)
				reloadables.registerEvents(listener);
			else
				registerEvents(listener);
	}

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listener
	 */
	protected final void registerEvents(Listener listener) {
		if (startingReloadables)
			reloadables.registerEvents(listener);
		else
			getServer().getPluginManager().registerEvents(listener, this);
	}

	/**
	 * Convenience method for quickly registering an event if the condition is met
	 *
	 * @param listener
	 * @param condition
	 */
	protected final void registerEventsIf(SimpleListener<? extends Event> listener, boolean condition) {
		if (condition)
			if (startingReloadables)
				reloadables.registerEvents(listener);
			else
				registerEvents(listener);
	}

	/**
	 * Convenience method for quickly registering a single event
	 *
	 * @param listener
	 */
	protected final void registerEvents(SimpleListener<? extends Event> listener) {
		if (startingReloadables)
			reloadables.registerEvents(listener);
		else
			listener.register();
	}

	/**
	 * Convenience method for registering a bukkit command
	 *
	 * @param command
	 */
	protected final void registerCommand(Command command) {
		Remain.registerCommand(command);
	}

	/**
	 * Convenience shortcut for calling the register method in {@link SimpleCommand}
	 *
	 * @param command
	 */
	protected final void registerCommand(SimpleCommand command) {
		command.register();
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register(StrictList)}
	 *
	 * @param label
	 * @param group
	 */
	protected final void registerCommands(String label, SimpleCommandGroup group) {
		registerCommands(label, null, group);
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register(StrictList)}
	 *
	 * @param label
	 * @param aliases
	 * @param group
	 */
	protected final void registerCommands(String label, List<String> aliases, SimpleCommandGroup group) {
		if (getMainCommand() != null && getMainCommand().getLabel().equals(label))
			throw new FoException("Your main command group is registered automatically!");

		group.register(label, aliases);
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register(StrictList)}
	 *
	 * @param labelAndAliases
	 * @param group
	 */
	protected final void registerCommands(StrictList<String> labelAndAliases, SimpleCommandGroup group) {
		Valid.checkBoolean(!labelAndAliases.isEmpty(), "Must specify at least label for command group: " + group);

		if (getMainCommand() != null && getMainCommand().getLabel().equals(labelAndAliases.get(0)))
			throw new FoException("Your main command group is registered automatically!");

		group.register(labelAndAliases);
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * The start-up fancy logo
	 *
	 * @return null by default
	 */
	protected String[] getStartupLogo() {
		return null;
	}

	/**
	 * The the minimum MC version to run
	 *
	 * We will prevent loading it automatically if the server's version is
	 * below the given one
	 *
	 * @return
	 */
	public MinecraftVersion.V getMinimumVersion() {
		return null;
	}

	/**
	 * The maximum MC version for this plugin to load
	 *
	 * We will prevent loading it automatically if the server's version is
	 * above the given one
	 *
	 * @return
	 */
	public MinecraftVersion.V getMaximumVersion() {
		return null;
	}

	/**
	 * Return your main setting classes extending {@link YamlStaticConfig}.
	 *
	 * TIP: Extend {@link SimpleSettings} and {@link SimpleLocalization}
	 *
	 * @return
	 */
	public List<Class<? extends YamlStaticConfig>> getSettings() {
		return null;
	}

	/**
	 * Get your main command group, e.g. for ChatControl it's /chatcontrol
	 *
	 * @return
	 */
	public SimpleCommandGroup getMainCommand() {
		return null;
	}

	/**
	 * Get the year of foundation displayed in {@link #getMainCommand()}
	 *
	 * @return -1 by default, or the founded year
	 */
	public int getFoundedYear() {
		return -1;
	}

	/**
	 * Get your automatic update check
	 *
	 * @return
	 */
	public SpigotUpdater getUpdateCheck() {
		return null;
	}

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 *
	 * @return time limit in milliseconds for processing regular expression
	 */
	public int getRegexTimeout() {
		throw new FoException("Must override getRegexTimeout()");
	}

	/**
	 * Strip colors from checked message while checking it against a regex?
	 *
	 * @return
	 */
	public boolean regexStripColors() {
		return true;
	}

	/**
	 * Shall we apply Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
	 * flags when compiling regex? May impose a slight performancy penaulty
	 * but increases catches.
	 *
	 * @return
	 */
	public boolean regexCaseInsensitive() {
		return true;
	}

	/**
	 * Return the bungee suite if you want this plugin
	 * to send and receive messages from BungeeCord
	 *
	 * @return
	 */
	public SimpleBungee getBungeeCord() {
		return null;
	}

	/**
	 * Return if the JavaScript variables are enabled?
	 *
	 * Will load them from variables/javascript.txt file from your plugins folder.
	 *
	 * See https://github.com/kangarko/chatcontrol-pro/wiki/JavaScript-in-Bukkit for help
	 *
	 * @return
	 */
	public boolean areScriptVariablesEnabled() {
		return false;
	}

	/**
	 * Should we replace variables in {@link Variables} also in the javascript code?
	 *
	 * @return
	 */
	public boolean replaceVariablesInCustom() {
		return false;
	}

	/**
	 * Should we replace script variables in {@link Variables} also in the
	 * javascript code?
	 *
	 * @return
	 */
	public boolean replaceScriptVariablesInCustom() {
		return false;
	}

	/**
	 * If vault is enabled, it will return player's prefix from each of their group
	 *
	 * @return
	 */
	public boolean vaultMultiPrefix() {
		return false;
	}

	/**
	 * If vault is enabled, it will return player's suffix from each of their group
	 *
	 * @return
	 */
	public boolean vaultMultiSuffix() {
		return false;
	}

	/**
	 * Should every message be divided by \n by an own method (tends to work more
	 * then split("\n"))
	 *
	 * @return
	 */
	public boolean enforeNewLine() {
		return false;
	}

	// ----------------------------------------------------------------------------------------
	// Prevention
	// ----------------------------------------------------------------------------------------

	/**
	 * Get the plugins jar file
	 */
	@Override
	protected final File getFile() {
		return super.getFile();
	}

	/**
	 * @deprecated 	DO NOT USE
	 * 				Use {@link SimpleCommand#register()} instead for your commands
	 */
	@Deprecated
	@Override
	public final PluginCommand getCommand(String name) {
		return super.getCommand(name);
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		throw unsupported("onCommand");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		throw unsupported("onTabComplete");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final FileConfiguration getConfig() {
		throw unsupported("getConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void saveConfig() {
		throw unsupported("saveConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void saveDefaultConfig() {
		throw unsupported("saveDefaultConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void reloadConfig() {
		throw new FoException("Cannot call reloadConfig in " + getName() + ", use reload()!");
	}

	private final FoException unsupported(String method) {
		return new FoException("Cannot call " + method + " in " + getName() + ", use YamlConfig or SimpleCommand classes in Foundation for that!");
	}
}
