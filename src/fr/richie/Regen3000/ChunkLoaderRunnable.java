package fr.richie.Regen3000;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class ChunkLoaderRunnable implements Runnable{
	
	RMPlugin plugin;
	
	int ydiff = 0;
	
	int minX;
	int maxX;
	int minZ;
	int maxZ;
	
	int currX;
	int currZ;
	
	int toLoad = 0;
	int nbLoaded = 0;
	int lastPercent = -1;
	
	int speed = 1;
	
	World world;

	private long startTime = 0;
	
	CommandSender cs;
	
	RMAction a;
	
	public ChunkLoaderRunnable(RMAction a){
		this.a = a;
		
		this.minX = a.chunkMin.getBlockX();
		this.minZ = a.chunkMin.getBlockZ();
		this.maxX = a.chunkMax.getBlockX();
		this.maxZ = a.chunkMax.getBlockZ();
		
		this.toLoad = (maxX-minX+1)*(maxZ-minZ+1);
		
		if(a.step==0){
			this.world = a.wdest;
		}else{
			this.world = a.wsource;
		}
		
		this.currX = minX;
		this.currZ = minZ;
		
		this.plugin = a.plugin;
		
		this.speed = 8;
		
		this.cs = a.player;
		
	}
	
	
	@Override
	public void run() {
		
		if(startTime == 0){
			if(a.step != 0)
			world.setAutoSave(false);
			startTime = System.currentTimeMillis();
		}
		
		
		for(int i=0; i<speed; i++){
			world.getChunkAt(currX, currZ);
			nbLoaded++;

			currX++;

			if(currX>maxX){
				currZ++;
				currX=minX;
			}

			
			if(lastPercent<((int)((((double)nbLoaded)/((double)toLoad))*100.0/10.0))){
				lastPercent = (int) ((((double)nbLoaded)/((double)toLoad))*100.0/10.0);
				
				if(a.step==0)
					a.sendMessage(ChatColor.GRAY+"Chargement chunks monde actuel : "+lastPercent+"0% ("+nbLoaded+"/"+toLoad+")");
				else
					a.sendMessage(ChatColor.GRAY+"Génération chunks monde source : "+lastPercent+"0% ("+nbLoaded+"/"+toLoad+")");
				
			}	
			
			if(a.stopped){
				a.notifStop();
				return;
			}
			
			if(currZ>maxZ){
				
				if(a.step==0){
				
					a.sendMessage(ChatColor.GREEN+"Chargement des "+nbLoaded+" chunks fini ! (en "+((System.currentTimeMillis()-startTime)/1000)+" secondes)");
					a.genSourceWorld();
					
				}else{
					
					a.sendMessage(ChatColor.GREEN+"Génération des "+nbLoaded+" chunks finie ! (en "+((System.currentTimeMillis()-startTime)/1000)+" secondes)");
					a.copyBlocks();
					
				}
				
				
				
				// Ce return est très important :
				return;
			}
		}
		
		
		a.runningTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, 2);

	}

}
