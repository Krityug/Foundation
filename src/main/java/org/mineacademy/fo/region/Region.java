package org.mineacademy.fo.region;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Represents a cuboid region
 */
@Getter
@Setter
public class Region implements ConfigSerializable {

	/**
	 * The name of the region, or null if not given
	 */
	@Nullable
	private String name;

	/**
	 * The primary region position
	 */
	private Location primary;

	/**
	 * The secondary region position
	 */
	private Location secondary;

	/**
	 * Create a new region
	 *
	 * @param primary
	 * @param secondary
	 */
	public Region(@Nullable Location primary, @Nullable Location secondary) {
		this(null, primary, secondary);
	}

	/**
	 * Create a new named region
	 *
	 * @param name
	 * @param primary
	 * @param secondary
	 */
	public Region(@Nullable String name, @Nullable Location primary, @Nullable Location secondary) {
		this.name = name;

		if (primary != null) {
			Valid.checkNotNull(primary.getWorld(), "Primary location lacks a world!");

			this.primary = primary;
		}

		if (secondary != null) {
			Valid.checkNotNull(secondary.getWorld(), "Primary location lacks a world!");

			this.secondary = secondary;
		}

		if (primary != null && secondary != null) {
			Valid.checkBoolean(primary.getWorld().getName().equals(secondary.getWorld().getName()), "Points must be in one world! Primary: " + primary + " != secondary: " + secondary);

			final int x1 = primary.getBlockX(), x2 = secondary.getBlockX(),
					y1 = primary.getBlockY(), y2 = secondary.getBlockY(),
					z1 = primary.getBlockZ(), z2 = secondary.getBlockZ();

			this.primary = primary.clone();
			this.secondary = secondary.clone();

			this.primary.setX(Math.min(x1, x2));
			this.primary.setY(Math.min(y1, y2));
			this.primary.setZ(Math.min(z1, z2));

			this.secondary.setX(Math.max(x1, x2));
			this.secondary.setY(Math.max(y1, y2));
			this.secondary.setZ(Math.max(z1, z2));
		}
	}

	/**
	 * Calculate a rough location of the center of this region
	 *
	 * @return
	 */
	public final Location getCenter() {
		Valid.checkBoolean(isWhole(), "Cannot perform getCenter on a non-complete region: " + toString());

		return new Location(primary.getWorld(),
				(primary.getX() + secondary.getX()) / 2,
				(primary.getY() + secondary.getY()) / 2,
				(primary.getZ() + secondary.getZ()) / 2);
	}

	/**
	 * Count all blocks within this region
	 *
	 * @return
	 */
	public final List<Block> getBlocks() {
		Valid.checkBoolean(isWhole(), "Cannot perform getBlocks on a non-complete region: " + toString());

		return BlockUtil.getBlocks(primary, secondary);
	}

	/**
	 * Count all entities within this region
	 *
	 * @return
	 */
	public final List<Entity> getEntities() {
		Valid.checkBoolean(isWhole(), "Cannot perform getEntities on a non-complete region: " + toString());

		final List<Entity> found = new LinkedList<>();

		final int xMin = (int) primary.getX() >> 4;
		final int xMax = (int) secondary.getX() >> 4;
		final int zMin = (int) primary.getZ() >> 4;
		final int zMax = (int) secondary.getZ() >> 4;

		for (int cx = xMin; cx <= xMax; ++cx)
			for (int cz = zMin; cz <= zMax; ++cz)
				for (final Entity en : getWorld().getChunkAt(cx, cz).getEntities())
					if (en.isValid() && en.getLocation() != null && isWithin(en.getLocation()))
						found.add(en);

		return found;
	}

	/**
	 * Get world for this region
	 *
	 * @return
	 */
	public final World getWorld() {
		if (!isWhole())
			return null;

		if (primary != null && secondary == null)
			return primary.getWorld();

		if (secondary != null && primary == null)
			return secondary.getWorld();

		Valid.checkBoolean(primary.getWorld().equals(secondary.getWorld()), "Worlds of this region not the same: " + primary.getWorld() + " != " + secondary.getWorld());
		return primary.getWorld();
	}

	/**
	 * Return true if the given point is within this region
	 *
	 * @param loc
	 * @return
	 */
	public final boolean isWithin(@NonNull Location loc) {
		Valid.checkBoolean(isWhole(), "Cannot perform isWithin on a non-complete region: " + toString());

		if (!loc.getWorld().equals(primary.getWorld()))
			return false;

		final int x = (int) loc.getX();
		final int y = (int) loc.getY();
		final int z = (int) loc.getZ();

		return x >= primary.getX() && x <= secondary.getX()
				&& y >= primary.getY() && y <= secondary.getY()
				&& z >= primary.getZ() && z <= secondary.getZ();
	}

	/**
	 * Return true if both region points are set
	 *
	 * @return
	 */
	public final boolean isWhole() {
		return primary != null && secondary != null;
	}

	/**
	 * Sets a new primary and secondary locations,
	 * preserving old keys if the new are not given
	 *
	 * @param primary
	 * @param secondary
	 */
	public final void updateLocationsWeak(Location primary, Location secondary) {
		if (primary != null)
			this.primary = primary;

		if (secondary != null)
			this.secondary = secondary;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "{name=" + name + ",location=" + Common.shortLocation(primary) + " - " + Common.shortLocation(secondary) + "}";
	}

	/**
	 * Saves the region data into a map you can save in your yaml or json file
	 */
	@Override
	public final SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExist("Name", name);
		map.putIfExist("Primary", primary);
		map.putIfExist("Secondary", secondary);

		return map;
	}

	/**
	 * Converts a saved map from your yaml/json file into a region if it contains Primary and Secondary keys
	 *
	 * @param map
	 * @return
	 */
	public static Region deserialize(SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = map.getLocation("Primary");
		final Location sec = map.getLocation("Secondary");

		return new Region(name, prim, sec);
	}
}