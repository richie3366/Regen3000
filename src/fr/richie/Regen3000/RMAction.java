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
	
	public EditSession es;

	public Material wallBlockMaterial;
	
	public int runningTask = -1;

	public long startTime = 0;

	public boolean nolava = false;

	public boolean bcast = false;

	public String period;
	
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
		
		String wToCreate = "RMregen_"+region.getId().toLowerCase()+"_"+(new Random().nextInt(1000000));
		
		if(bcast){
			sendMessage(ChatColor.AQUA + "[Regen3000] Régénération de la mine "+region.getId()+" démarrée.");
			// TODO : message plus parlant ?
		}
		
		
		sendMessage(ChatColor.GRAY+"Création du monde source ...");

		wsource = RMUtils.createWorld(wToCreate);

		if(wsource == null){
			sendMessage(ChatColor.RED + "Erreur : monde non créé.");
			return;
		}
		
		sendMessage(ChatColor.GRAY+"Monde source créé !");

		
		
		this.runningTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ChunkLoaderRunnable(this));
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
		
		if(bcast){
			Bukkit.broadcastMessage(message);
		}else{
			player.sendMessage(message);
		}
		
	}
	
}
