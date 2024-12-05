package me.Mastervrunner.LavaBubble;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.airbending.Suffocate;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.earthbending.EarthGrab;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.waterbending.blood.Bloodbending;

/*
 * Implements Listener so that the server knows this is checking for events.
 */
public class LavaBubbleListener implements Listener {
	
	
	@EventHandler
	public void onSwing(PlayerAnimationEvent event) {

	    Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		
		if (Suffocate.isBreathbent(player)) {
			event.setCancelled(true);
			return;
		} else if ((Bloodbending.isBloodbent(player) && !bPlayer.getBoundAbilityName().equalsIgnoreCase("AvatarState"))) {
			event.setCancelled(true);
			return;
		} else if (MovementHandler.isStopped(player)) {
			if (player.hasMetadata("movement:stop")) {
				final CoreAbility abil = (CoreAbility) player.getMetadata("movement:stop").get(0).value();
				if (!(abil instanceof EarthGrab)) {
					event.setCancelled(true);
					return;
				}
			}
		} else if (bPlayer.isChiBlocked()) {
			event.setCancelled(true);
			return;
		}
		
		if (event.isCancelled() || bPlayer == null) {
			return;
		} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase(null)) {
			return;

		}
		
		final CoreAbility coreAbil = bPlayer.getBoundAbility();
		String abil = bPlayer.getBoundAbilityName();
		
		Boolean enabled = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Mastervrunner.Lava.LavaBubble.Enabled");
		
		if (coreAbil instanceof LavaAbility && bPlayer.isElementToggled(Element.LAVA) == true && bPlayer.isElementToggled(Element.EARTH) == true && enabled == true) {
			if (bPlayer.canCurrentlyBendWithWeapons()) {
				
				if (abil.equalsIgnoreCase("LavaBubble")) {
					new LavaBubble(player, false);
				} 
			}
		}
		
	}
}