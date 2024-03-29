package org.mineacademy.fo.model;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.ReflectionException;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.event.RegionScanCompleteEvent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.spigotmc.WatchdogThread;

/**
 * A class that has ability to scan saved regions on the disk and execute
 * actions for each saved chunk.
 */
public abstract class OfflineRegionScanner {

	/**
	 * Folders that will be scanned
	 */
	private static final String[] FOLDERS = { "region", "DIM-1/region", "DIM1/region" };

	/**
	 * A valid file pattern
	 */
	private static final Pattern FILE_PATTERN = Pattern.compile("r\\.(.+)\\.(.+)\\.mca");

	/**
	 * Seconds between each file processing operation.
	 */
	private static int OPERATION_WAIT_SECONDS = 1;

	/**
	 * Changing flag: How many files processed out of total?
	 */
	private int done = 0;

	/**
	 * Changing flag: The total amount of region files to scan
	 */
	private int totalFiles = 0;

	/**
	 * Changing flag: The world we are scanning
	 */
	private World world;

	/**
	 * Changing flag: Last time an operation was done successfully
	 */
	private long lastTick = System.currentTimeMillis();

	/**
	 * Starts the scan for the given world (warning: this operation is blocking
	 * and takes long time, see {@link #getEstimatedWaitTimeSec(World)})
	 *
	 * @param world
	 */
	public final void scan(World world) {
		scan0(world);
	}

	private final void scan0(World world) {
		Thread watchdog = null;

		try {

			// Disable to prevent lag warnings since we scan chunks on the main thread
			try {
				final Field f = WatchdogThread.class.getDeclaredField("instance");
				f.setAccessible(true);

				watchdog = (WatchdogThread) f.get(null);
				watchdog.suspend();

			} catch (final Throwable t) {
				System.out.println("FAILED TO DISABLE WATCHDOG, ABORTING! See below and report to us. NO DATA WERE MANIPULATED.");
				Common.callEvent(new RegionScanCompleteEvent(world));

				t.printStackTrace();
				return;
			}
		} catch (final NoClassDefFoundError err) {
			//
		}

		System.out.println(Common.consoleLine());
		System.out.println("Scanning regions in " + world.getName());
		System.out.println(Common.consoleLine());

		LagCatcher.start("Region scanner for " + world.getName());

		final File[] files = getRegionFiles(world);

		if (files == null) {
			Common.log("Unable to locate the region files for: " + world.getName());

			return;
		}

		final Queue<File> queue = new LimitedQueue<>(files.length + 1);
		queue.addAll(Arrays.asList(files));

		this.totalFiles = files.length;
		this.world = world;

		// Start the schedule
		schedule(queue);

		if (watchdog != null)
			watchdog.resume();

		LagCatcher.end("Region scanner for " + world.getName(), 0);
	}

	/**
	 * Self-repeating cycle of loading chunks from the disk until
	 * we reach the end of the queue
	 *
	 * @param queue
	 */
	private final void schedule(Queue<File> queue) {
		new BukkitRunnable() {

			@Override
			public void run() {
				final File file = queue.poll();

				// Queue finished
				if (file == null) {
					System.out.println(Common.consoleLine());
					System.out.println("Region scanner finished.");
					System.out.println(Common.consoleLine());

					Common.callEvent(new RegionScanCompleteEvent(world));
					onScanFinished();

					cancel();
					return;
				}

				scanFile(file);

				Common.runLater(20 * OPERATION_WAIT_SECONDS, () -> schedule(queue));
			}
		}.runTask(SimplePlugin.getInstance());
	}

	/**
	 * Scans the given region file
	 *
	 * @param file
	 */
	private final void scanFile(File file) {
		final Matcher matcher = FILE_PATTERN.matcher(file.getName());

		if (!matcher.matches())
			return;

		final int regionX = Integer.parseInt(matcher.group(1));
		final int regionZ = Integer.parseInt(matcher.group(2));

		System.out.print("[" + Math.round(Double.valueOf((double) done++ / (double) totalFiles * 100)) + "%] Processing " + file);

		// Calculate time, collect memory and increase pauses in between if running out of memory
		if (System.currentTimeMillis() - lastTick > 4000) {
			final long free = Runtime.getRuntime().freeMemory() / 1_000_000;

			if (free < 200) {
				System.out.print(" [Low memory (" + free + "Mb)! Running GC and increasing delay between operations ..]");

				OPERATION_WAIT_SECONDS = +2;

				System.gc();
				Common.sleep(5_000);
			} else
				System.out.print(" [free memory = " + free + " mb]");

			lastTick = System.currentTimeMillis();
		}

		System.out.println();

		// Load the file
		final Object region = RegionAccessor.getRegionFile(file);

		// Load each chunk within that file
		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++) {
				final int chunkX = x + (regionX << 5);
				final int chunkZ = z + (regionZ << 5);

				if (RegionAccessor.isChunkSaved(region, x, z)) {
					final Chunk chunk = world.getChunkAt(chunkX, chunkZ);

					onChunkScan(chunk);
				}
			}

		// Save
		try {
			RegionAccessor.save(region);

		} catch (final Throwable t) {
			System.out.println("Failed to save region " + file + ", operation stopped.");
			Remain.sneaky(t);
		}
	}

	/**
	 * Called when a chunk is being scanned and loaded
	 *
	 * @param chunk
	 */
	protected abstract void onChunkScan(Chunk chunk);

	/**
	 * Called when the scan is finished, after {@link RegionScanCompleteEvent}
	 */
	protected void onScanFinished() {
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return all region files stored on the disk for the given world
	 *
	 * @param world
	 * @return
	 */
	public static File[] getRegionFiles(World world) {
		final File regionDir = getRegionDirectory(world);

		return regionDir == null ? null : regionDir.listFiles((FilenameFilter) (dir, name) -> name.toLowerCase().endsWith(".mca"));
	}

	/**
	 * Return the region directory for the given world
	 *
	 * @param world
	 * @return
	 */
	private static final File getRegionDirectory(World world) {
		for (final String f : FOLDERS) {
			final File file = new File(world.getWorldFolder(), f);

			if (file.isDirectory())
				return file;
		}

		return null;
	}

	/**
	 * Get how long scanning should take for the given world
	 * depending on its amount of region files
	 *
	 * @param world
	 * @return
	 */
	public static int getEstimatedWaitTimeSec(World world) {
		final File[] files = getRegionFiles(world);

		return (OPERATION_WAIT_SECONDS + 2) * files.length;
	}
}

/**
 * Reflection helper class for accessing region files
 */
class RegionAccessor {

	private static Constructor<?> regionFileConstructor;
	private static Method isChunkSaved;

	private static final String saveMethodName;

	static {
		saveMethodName = MinecraftVersion.atLeast(V.v1_13) ? "close" : "c";

		try {
			final Class<?> regionFileClass = ReflectionUtil.getNMSClass("RegionFile");
			regionFileConstructor = regionFileClass.getConstructor(File.class);
			isChunkSaved = MinecraftVersion.newerThan(V.v1_13) ? regionFileClass.getMethod("b", ReflectionUtil.getNMSClass("ChunkCoordIntPair")) : regionFileClass.getMethod(MinecraftVersion.atLeast(V.v1_13) ? "b" : "c", int.class, int.class);

		} catch (final ReflectiveOperationException ex) {
			Remain.sneaky(ex);
		}
	}

	static Object getRegionFile(File file) {
		try {
			return regionFileConstructor.newInstance(file);
		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Could not create region file from " + file, ex);
		}
	}

	static boolean isChunkSaved(Object region, int x, int z) {
		try {
			if (MinecraftVersion.newerThan(V.v1_13)) {
				final Object chunkCoordinates = ReflectionUtil.getNMSClass("ChunkCoordIntPair").getConstructor(int.class, int.class).newInstance(x, z);

				return (boolean) isChunkSaved.invoke(region, chunkCoordinates);
			}

			return (boolean) isChunkSaved.invoke(region, x, z);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Could not find if region file " + region + " has chunk at " + x + " " + z, ex);
		}
	}

	static void save(Object region) {
		try {
			region.getClass().getDeclaredMethod(saveMethodName).invoke(region);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Error saving region " + region, ex);
		}
	}
}
