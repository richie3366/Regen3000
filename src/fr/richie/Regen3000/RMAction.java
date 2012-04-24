package fr.richie.Regen3000;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
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
	
	public EditSession es;

	public Material wallBlockMaterial;
	
	public int runningTask = -1;

	public Long startTime = null;
	public Long finishTime = null;

	public boolean nolava = false;
	public boolean bcast = false;

	public String period;

	private String historyNode;
	
	public RMAction(RMPlugin plugin, ProtectedRegion region, Region weRegion, Player player,
			World wsource, BlockVector2D chunkMin, BlockVector2D chunkMax,
			BlockVector minPoint, BlockVector maxPoint, Material wallBlockMaterial, String period, int ydiff, boolean nolava, boolean bcast) {
		this.plugin = plugin;
		this.region = region;
		this.player = player;
		this.wsource = wsource;
		this.wdest = player.getWorld();
		this.chunkMin = chunkMin;
		this.chunkMax = chunkMax;
		this.minPoint = minPoint;
		this.maxPoint = maxPoint;
		this.ydiff = ydiff;
		this.nolava  = nolava;
		this.weRegion = weRegion;
		this.bcast  = bcast;
		
		this.period = period;
		
		this.wallBlockMaterial = wallBlockMaterial;
		
		this.es = plugin.worldEdit.createEditSession(player);
		
		plugin.actionList.put(player.getName(), this);
		
	}


	public void init() {
		
		this.started = true;
		this.startTime = System.currentTimeMillis();
		
		historyNode = "regens."+wsource.getName()+"."+region.getId();
		
		saveToHistoryFile();
		
		String wToCreate = "RMregen_"+region.getId().toLowerCase()+"_"+(new Random().nextInt(1000000));
		
		if(bcast){
			sendMessage(ChatColor.AQUA + "[Regen3000] R�g�n�ration de la mine "+region.getId()+" d�marr�e.");
		}
		
		
		sendMessage(ChatColor.GRAY+"Cr�ation du monde source ...");

		wsource = RMUtils.createWorld(wToCreate);

		if(wsource == null){
			sendMessage(ChatColor.RED + "Erreur : monde non cr��.");
			return;
		}
		
		sendMessage(ChatColor.GRAY+"Monde source cr�� !");

		
		
		this.runningTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ChunkLoaderRunnable(this));
	}
	
	private void saveToHistoryFile() {

		plugin.getHistoryFile().set(historyNode+".playerName", player.getName());
		plugin.getHistoryFile().set(historyNode+".period", period);
		plugin.getHistoryFile().set(historyNode+".wallID", wallBlockMaterial.getId());
		plugin.getHistoryFile().set(historyNode+".ydiff", ydiff);
		plugin.getHistoryFile().set(historyNode+".nolava", nolava);
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
		
		plugin.actionList.remove(player.getName());
		
		
	}


	public void stop() {
		if(this.started){
			this.stopped = true;
		}else{
			this.stopped = true;
			
			sendMessage(ChatColor.AQUA + "Regen annul�.");
			
			this.finish();
		}
	}


	public void notifStop() {
		
		player.sendMessage(ChatColor.AQUA + "Regen annul�. En esp�rant que vous l'ayez fait � temps.");
		
		this.finish();
	}
	
	public void sendMessage(String message){
		
		if(bcast){
			Bukkit.broadcastMessage(message);
		}else{
			player.sendMessage(message);
		}
		
	}


	public void sendRecap(RegionLoader loadedRegion, String cmd) {
		String complement = "sans mettre de murs autour.";
		
		if(wallBlockMaterial.getId()>0)
			complement = "en mettant un mur de "+wallBlockMaterial.toString()+" autour.";
		
		player.sendMessage(ChatColor.GOLD + "Vous avez l'intention de reg�n�rer la mine contenue par la region WG '"+loadedRegion.WGregion.getId()+"', d'une taille de "+(loadedRegion.sizeX)+"x"+(loadedRegion.sizeY)+"x"+(loadedRegion.sizeZ)+" [yDiff="+ydiff+", no-lava="+nolava+", broadcast="+bcast+"], "+complement);
		player.sendMessage(ChatColor.AQUA + "Pour lancer le regen, entrez la commande '"+ChatColor.YELLOW+cmd+" start"+ChatColor.AQUA+"'.");
		
	}
	
}
