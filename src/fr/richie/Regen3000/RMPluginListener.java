package fr.richie.Regen3000;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginManager;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class RMPluginListener implements Listener{
	
	
	private RMPlugin plugin;
	private PluginManager pm;

	public RMPluginListener(RMPlugin p){
		this.plugin = p;
		
		this.pm = Bukkit.getPluginManager();
		
	}
	
	@EventHandler
	public void onPluginEnable(PluginEnableEvent e){
		
		
		if(!plugin.hooked && pm.isPluginEnabled("WorldEdit") && pm.isPluginEnabled("WorldGuard")){
			plugin.worldEdit = (WorldEditPlugin)pm.getPlugin("WorldEdit");
			plugin.worldGuard = (WorldGuardPlugin)pm.getPlugin("WorldGuard");
			
			System.out.println("[Regen3000] Hooked with WE/WG ! Plugin activated.");
			
			pm.registerEvents(new RMListener(plugin), plugin);
			
			plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new RMPlugin.CronRunnable(plugin), 60*20L, 60*20L);
			
			plugin.hooked = true;
		}
		
	}
	
}
