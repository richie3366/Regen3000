package fr.richie.Regen3000;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.regions.Region;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import fr.richie.Regen3000.RMPlugin.RegionLoader;

public class RMAction {
	
	public RMPlugin plugin;
	
	public ProtectedRegion region;
	public Region weRegion;
	public Player player;
	
	public World wsource;
	public World wdest;
	
	public BlockVector2D chunkMin;
	public BlockVector2D chunkMax;
	
	public BlockVector minPoint;
	public BlockVector maxPoint;
	
	public int ydiff;
	public int step = 0;
	
	public boolean started = false;
	public boolean stopped = false;
	public boolean finished = false;
	
	public Material wallBlockMaterial;
	
	public int runningTask = -1;

	public Long startTime = null;
	public Long finishTime = null;

	public boolean nolava = false;
	public boolean bcast = false;
	public boolean forceemerald = false;

	public String period;

	private String historyNode;

	private long nextRegen = -1;

	private RegionLoader loadedRegion;
	
	public RMAction(RMPlugin plugin, RegionLoader loadedRegion, Player player,
			World wdest, Material wallBlockMaterial, String period, int ydiff, boolean nolava, boolean bcast, boolean forceemerald) {
		this.plugin = plugin;
		this.region = loadedRegion.WGregion;
		this.loadedRegion = loadedRegion;
		this.player = player;
		//this.wsource = wsource;
		this.wdest = wdest;
		this.chunkMin = loadedRegion.chunkMin;
		this.chunkMax = loadedRegion.chunkMax;
		this.minPoint = loadedRegion.realMinPoint;
		this.maxPoint = loadedRegion.realMaxPoint;
		this.ydiff = ydiff;
		this.nolava  = nolava;
		this.forceemerald = forceemerald;
		this.weRegion = loadedRegion.regionContainer;
		this.bcast  = bcast;
		
		this.period = period;
		
		this.wallBlockMaterial = wallBlockMaterial;
				
		if(player != null)
			plugin.actionList.put(player.getName(), this);
		
	}
	
	
	
	public void init() {
		
		this.started = true;
		this.startTime = System.currentTimeMillis();
		
		if(period != null){
			try {
				this.nextRegen = RMUtils.getFutureTime(period);
			} catch (Exception e) {
				this.nextRegen = -1;
			}
		}
		
		historyNode = "regens."+wdest.getName()+"."+region.getId();
		
		saveToHistoryFile();
		
		String wToCreate = "RMregen_"+region.getId().toLowerCase()+"_"+(new Random().nextInt(1000000));
		
		if(bcast){
			sendMessage(ChatColor.AQUA + "[Regen3000] Régénération de la mine "+region.getId()+" démarrée.");
		}
		
		
		sendMessage(ChatColor.GRAY+"Création du monde source ...");

		wsource = RMUtils.createWorld(wToCreate, this.forceemerald);

		if(wsource == null){
			sendMessage(ChatColor.RED + "Erreur : monde non créé.");
			return;
		}
		
		sendMessage(ChatColor.GRAY+"Monde source créé !");

		
		
		this.runningTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ChunkLoaderRunnable(this));
	}
	
	private void saveToHistoryFile() {

		if(player != null) plugin.getHistoryFile().set(historyNode+".playerName", player.getName());
		plugin.getHistoryFile().set(historyNode+".period", period);
		plugin.getHistoryFile().set(historyNode+".nextRegen", nextRegen);
		plugin.getHistoryFile().set(historyNode+".wallID", wallBlockMaterial.getId());
		plugin.getHistoryFile().set(historyNode+".ydiff", ydiff);
		plugin.getHistoryFile().set(historyNode+".nolava", nolava);
		plugin.getHistoryFile().set(historyNode+".forceemerald", forceemerald);
		plugin.getHistoryFile().set(historyNode+".bcast", bcast);
		plugin.getHistoryFile().set(historyNode+".started", started);
		plugin.getHistoryFile().set(historyNode+".startedTime", startTime);
		plugin.getHistoryFile().set(historyNode+".finished", finished);
		plugin.getHistoryFile().set(historyNode+".finishTime", finishTime);
		
		plugin.saveHistoryFile();
		
	}


	public void genSourceWorld(){
		
		this.step++;
		this.runningTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ChunkLoaderRunnable(this));
		
	}
	
	public void copyBlocks(){
		
		this.step++;
		
		this.runningTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new CopyRunnable(this));
		
		//plugin.actionList.remove(this);
	}
	
	public void finish() {
		this.step++;
		
		if(this.started)
			RMUtils.removeWorld(this.wsource.getName());
		
		finished = true;
		finishTime = System.currentTimeMillis();
		
		saveToHistoryFile();
		
		if(player != null)
		plugin.actionList.remove(player.getName());
		
		
	}


	public void stop() {
		if(this.started){
			this.stopped = true;
		}else{
			this.stopped = true;
			
			sendMessage(ChatColor.AQUA + "Regen annulé.");
			
			this.finish();
		}
	}


	public void notifStop() {
		
		player.sendMessage(ChatColor.AQUA + "Regen annulé. En espérant que vous l'ayez fait à temps.");
		
		this.finish();
	}
	
	public void sendMessage(String message){
		
		if(bcast || player == null){
			Bukkit.broadcastMessage(message);
		}else{
			player.sendMessage(message);
		}
		
	}


	public void sendRecap(String cmd) {
		String complement = "sans mettre de murs autour.";
		
		if(wallBlockMaterial.getId()>0)
			complement = "en mettant un mur de "+wallBlockMaterial.toString()+" autour.";
		
		player.sendMessage(ChatColor.GOLD + "Vous avez l'intention de regénérer la mine contenue par la region WG '"+loadedRegion.WGregion.getId()+"', d'une taille de "+(loadedRegion.sizeX)+"x"+(loadedRegion.sizeY)+"x"+(loadedRegion.sizeZ)+" [yDiff="+ydiff+", no-lava="+nolava+", broadcast="+bcast+"], "+complement);
		player.sendMessage(ChatColor.AQUA + "Pour lancer le regen, entrez la commande '"+ChatColor.YELLOW+cmd+" start"+ChatColor.AQUA+"'.");
		
	}
	
}
