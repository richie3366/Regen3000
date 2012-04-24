package fr.richie.Regen3000;

import java.util.Collection;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class RMListener implements Listener {

	private RMPlugin plugin;

	public RMListener(RMPlugin plugin){
		this.plugin = plugin;
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onChunkUnload(ChunkUnloadEvent e){

		if(plugin.actionList.size()>0){

			Collection<RMAction> aList = plugin.actionList.values();

			for(RMAction a : aList){
				Chunk c = e.getChunk();

				if(c.getX() >= a.chunkMin.getBlockX() && c.getX() <= a.chunkMax.getBlockX()
						&& c.getZ() >= a.chunkMin.getBlockZ() && c.getZ() <= a.chunkMax.getBlockZ()){
					e.setCancelled(true);
				}

			}
		}

	}
}
