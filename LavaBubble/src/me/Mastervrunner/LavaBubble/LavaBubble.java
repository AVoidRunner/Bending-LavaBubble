package me.Mastervrunner.LavaBubble;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempBlock;

public class LavaBubble extends LavaAbility {

	@Attribute("Click" + Attribute.DURATION)
	private long clickDuration;
	@Attribute(Attribute.RADIUS)
	private double maxRadius;
	@Attribute(Attribute.SPEED)
	private double speed;
	@Attribute("RequireAir")
	private boolean requireAir;

	private boolean isShift;
	private double radius;
	private boolean removing = false; // Is true when the radius is shrinking.
	private final Map<Block, BlockState> waterOrigins = new ConcurrentHashMap<>();
	private Location location;
	private long lastActivation; // When the last click happened.

	private long Cooldown;

	public LavaBubble(final Player player, final boolean isShift) {
		super(player);

		this.setFields();

		if (CoreAbility.hasAbility(player, this.getClass())) {
			final LavaBubble bubble = CoreAbility.getAbility(player, this.getClass());

			if (bubble.location.getWorld().equals(player.getWorld())) {
				if (bubble.location.distanceSquared(player.getLocation()) < this.maxRadius * this.maxRadius) {
					if (bubble.removing) {
						bubble.removing = false;
					}

					bubble.location = player.getLocation();
					bubble.isShift = isShift;
					bubble.lastActivation = System.currentTimeMillis();
					return;
				}
			}
			bubble.removing = true;
		} else if (this.requireAir && !(!player.getEyeLocation().getBlock().getType().isSolid() && !player.getEyeLocation().getBlock().isLiquid())) {
			return;
		}

		if (!this.bPlayer.canBend(this)) {
			return;
		}

		this.radius = 0;
		this.isShift = isShift;
		this.location = player.getLocation();
		this.lastActivation = System.currentTimeMillis();

		this.start();
	}

	public void setFields() {
		this.clickDuration = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.Mastervrunner.Lava.LavaBubble.ClickDuration");
		this.maxRadius = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.Mastervrunner.Lava.LavaBubble.Radius");
		this.speed = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.Mastervrunner.Lava.LavaBubble.Speed");
		this.requireAir = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Mastervrunner.Lava.LavaBubble.MustStartAboveWater");
		
		this.Cooldown = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.Mastervrunner.Water.LavaBubble.Cooldown");
	}

	@Override
	public String getName() {
		return "LavaBubble";
	}

	@Override
	public void progress() {
		if (!this.bPlayer.canBend(this) || (this.isShift && !this.player.isSneaking()) || !this.location.getWorld().equals(this.player.getWorld())) {
			this.removing = true;
		}

		if (System.currentTimeMillis() - this.lastActivation > this.clickDuration && !this.isShift) {
			this.removing = true;
		}

		if (this.removing) {
			this.radius -= this.speed;

			if (this.radius <= 0.1) {
				this.radius = 0.1;
				this.remove();
			}
		} else {
			this.radius += this.speed;

			if (this.radius > this.maxRadius) {
				this.radius = this.maxRadius;
			}
		}

		final List<Block> list = new ArrayList<Block>();

		if (this.radius < this.maxRadius || !this.location.getBlock().equals(this.player.getLocation().getBlock())) {

			for (double x = -this.radius; x < this.radius; x += 0.5) {
				for (double y = -this.radius; y < this.radius; y += 0.5) {
					for (double z = -this.radius; z < this.radius; z += 0.5) {
						if (x * x + y * y + z * z <= this.radius * this.radius) {
							final Block b = this.location.add(x, y, z).getBlock();

							if (!this.waterOrigins.containsKey(b)) {
								if (isWater(b)) {
									if (!TempBlock.isTempBlock(b)) {
										this.waterOrigins.put(b, b.getState());
										if (b.getBlockData() instanceof Waterlogged) {
											final Waterlogged logged = (Waterlogged) b.getBlockData();
											logged.setWaterlogged(false);
											b.setBlockData(logged);
										} else if (isWater(b.getType())) {
											b.setType(Material.AIR);
										}
									}
								}
							}
							list.add(b); // Store it to say that it should be there.
							this.location.subtract(x, y, z);
						}
					}
				}
			}

			// Remove all blocks that shouldn't be there.
			final Set<Block> set = new HashSet<Block>();
			set.addAll(this.waterOrigins.keySet());
			set.removeAll(list);

			for (final Block b : set) {
				if (b.getBlockData() instanceof Waterlogged) {
					final Waterlogged logged = (Waterlogged) b.getBlockData();
					logged.setWaterlogged(true);
					b.setBlockData(logged);
				} else if (ElementalAbility.isAir(b.getType())) {
					b.setType(this.waterOrigins.get(b).getType());
					b.setBlockData(this.waterOrigins.get(b).getBlockData());
				}
				this.waterOrigins.remove(b);
			}
		}

		this.location = this.player.getLocation();
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public long getCooldown() {
		//return 0;
		return this.Cooldown;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}
	
	public void load() {
		/*
		 * Grabs information from the AbilityListener class so it knows when to start.
		 */
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new LavaBubbleListener(), ProjectKorra.plugin);
		
		ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Lava.LavaBubble.Enabled", true);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Lava.LavaBubble.Cooldown", 1000L);
		
		
		ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Lava.LavaBubble.ClickDuration", 2000L);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Lava.LavaBubble.Radius", 4.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Lava.LavaBubble.Speed", 0.5);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Lava.LavaBubble.MustStartAboveWater", false);
		
		
		/*
		 * Log message that appears when the ability is loaded.
		 */
		ProjectKorra.log.info("Successfully enabled " + getName() + " by " + " Mastervrunnner");
	}

	/*
	 * This method is run whenever the ability is disabled from a server.
	 * Restart/reload
	 */

	public void stop() {
		/*
		 * Log message that appears when the ability is disabled.
		 */
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + " Mastervrunner");
		
		/*
		 * When the server stops or reloads, the ability will stop what it's doing and remove.
		 */
		super.remove();
	}

	@Override
	public void remove() {
		super.remove();

		for (final Block b : this.waterOrigins.keySet()) {
			if (b.getBlockData() instanceof Waterlogged) {
				final Waterlogged logged = (Waterlogged) b.getBlockData();
				logged.setWaterlogged(true);
				b.setBlockData(logged);
			} else if (ElementalAbility.isAir(b.getType())) {
				b.setType(this.waterOrigins.get(b).getType());
				b.setBlockData(this.waterOrigins.get(b).getBlockData());
			}
		}

		this.waterOrigins.clear();
	}

	/**
	 * Returns whether the block provided is one of the air blocks used by
	 * WaterBubble
	 *
	 * @param block The block being tested
	 * @return True if it's in use
	 */
	public static boolean isAir(final Block block) {
		for (final LavaBubble bubble : CoreAbility.getAbilities(LavaBubble.class)) {
			if (bubble.waterOrigins.containsKey(block)) {
				return true;
			}
		}
		return false;
	}

	
	
}