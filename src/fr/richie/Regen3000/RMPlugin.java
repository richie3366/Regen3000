package fr.richie.Regen3000;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RMPlugin extends JavaPlugin{
	
	private File historyFile;
	private FileConfiguration history;
	
	public WorldEditPlugin worldEdit;
	public WorldGuardPlugin worldGuard;
	
	public HashMap<String, RMAction> actionList = new HashMap<String, RMAction>();

	public boolean hooked = false;
	
	public Material defaultWallBlockMaterial = null;
	public boolean defaultBroadcast;
	public boolean defaultNoLava;
	
	public void onEnable(){
		
		RMUtils.cleanRegenWorlds();
		
		initConfig();
		initHistoryFile();
		
		Bukkit.getPluginManager().registerEvents(new RMPluginListener(this), this);
		
	}
	
	private void initHistoryFile(){
		
		historyFile = new File(getDataFolder(), "history.yml");
		history = YamlConfiguration.loadConfiguration(historyFile);
		
		saveHistoryFile();
		
	}
	
	public void saveHistoryFile(){
		
		try {
			history.save(historyFile);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}
	
	public FileConfiguration getHistoryFile(){
		return history;
	}
	
	private void initConfig() {
		
		if(!getConfig().isSet("defaultWallBlockID")){
			getConfig().set("defaultWallBlockID", Material.BEDROCK.getId());
		}
		
		if(!getConfig().isSet("defaultBroadcast")){
			getConfig().set("defaultBroadcast", false);
		}

		if(!getConfig().isSet("defaultNoLava")){
			getConfig().set("defaultNoLava", false);
		}

		
		defaultWallBlockMaterial = Material.getMaterial(getConfig().getInt("defaultWallBlockID"));
		defaultBroadcast = getConfig().getBoolean("defaultBroadcast", false);
		defaultNoLava = getConfig().getBoolean("defaultNoLava", false);
		
		if(defaultWallBlockMaterial == null) defaultWallBlockMaterial = Material.BEDROCK;
		if(!defaultWallBlockMaterial.isBlock())defaultWallBlockMaterial = Material.BEDROCK;
		
		getConfig().set("defaultWallBlockID", defaultWallBlockMaterial.getId());
		
		System.out.println("[Regen3000] Config loaded. [defaultWallBlockMaterial="+defaultWallBlockMaterial.toString()+"]");
		
		saveConfig();
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		
		if(sender instanceof Player){
			final Player p = (Player)sender;
			
			String cmd = "/"+label;
			
			if(cmd.equals("//rm") || cmd.startsWith("/regenmine") || cmd.startsWith("/regen3000") || cmd.startsWith("/r3000")){

				if(!worldEdit.getPermissionsResolver().hasPermission(p.getName(), "regen3000.command") && !p.isOp()){
					p.sendMessage(ChatColor.RED + "Pas les perms pour ça ! =P");
					return true;
				}

				if(args.length < 1){
					
					sendCommands(p, cmd);
					
					return true;
				}

				if(args[0].toLowerCase().startsWith("reg")){
					
					if(this.actionList.containsKey(p.getName())){
						p.sendMessage(ChatColor.RED + "Vous avez déjà entré une commande de regen.");
						p.sendMessage(ChatColor.RED + "Pour annuler le précédent regen : "+cmd+" cancel");
						return true;
					}
					
					if(args.length < 2){
						sendMainCommand(p, cmd);
						return true;
					}
					
					String arg_wall = null;
					String arg_ydiff = null;
					String arg_nolava = null;
					String arg_bcast = null;
					String arg_period = null;
					String arg_forceemerald = null;
					
					for(int i = 2; i < args.length; i++){
						
						String arg = args[i];
						String[] split = arg.replace("=", ":").split(":");
						
						if(split.length == 2){
							String argname = split[0].toLowerCase().replace("-", "").replace("_", "");
							
							if(argname.startsWith("wall")){
								arg_wall = split[1];
							}else if(argname.equals("ydiff")){
								arg_ydiff = split[1];
							}else if(argname.equals("nolava")){
								arg_nolava = split[1];
							}else if(argname.equals("forceemerald")){
								arg_forceemerald = split[1];
							}else if(argname.startsWith("period")){
								arg_period = split[1];
							}else if(argname.endsWith("cast") || argname.equals("bc")){
								arg_bcast = split[1];
							}
							
						}
					}
					
					
					
					Material wallBlockMaterial = this.defaultWallBlockMaterial;

					if(arg_wall != null){
						try{

							wallBlockMaterial = Material.getMaterial(Integer.valueOf(arg_wall));


							if(wallBlockMaterial == null){
								p.sendMessage(ChatColor.RED + "Valeur wall_ID incorrecte !");
								return true;
							}

							if(!wallBlockMaterial.isBlock() && !wallBlockMaterial.equals(Material.AIR)){
								p.sendMessage(ChatColor.RED + wallBlockMaterial.toString() + " n'est pas un block.");
								return true;
							}


						}catch(Exception ex){
							p.sendMessage(ChatColor.RED + "Valeur wallBlockID incorrecte !");
							return true;
						}
					}



					int ydiff = 0;

					if(arg_ydiff != null){
						try{
							ydiff = Integer.valueOf(arg_ydiff);
						}catch(Exception ex){

							p.sendMessage(ChatColor.RED + "Valeur Y-Diff incorrecte !");
							return true;
						}
					}
					
					boolean nolava = this.defaultNoLava;
					
					if(arg_nolava != null){
						try{
							nolava = Boolean.valueOf(arg_nolava.replace("on", "true").replace("off", "false"));
						}catch(Exception ex){
							
							p.sendMessage(ChatColor.RED + "Valeur No-Lava incorrecte !");
							return true;
						}
					}
					
					boolean forceemerald = false;
					
					if(arg_forceemerald != null){
						try{
							forceemerald = Boolean.valueOf(arg_forceemerald.replace("on", "true").replace("off", "false"));
						}catch(Exception ex){
							
							p.sendMessage(ChatColor.RED + "Valeur Force-Emerald incorrecte !");
							return true;
						}
					}
					
					
					boolean bcast = this.defaultBroadcast;
					
					if(arg_bcast != null){
						try{
							bcast = Boolean.valueOf(arg_bcast.replace("on", "true").replace("off", "false"));
						}catch(Exception ex){
							
							p.sendMessage(ChatColor.RED + "Valeur broadcast incorrecte !");
							return true;
						}
					}
					
					String period = null;
					Long periodInMinutes = null;
					
					if(arg_period != null){
						
						try {
							periodInMinutes = RMUtils.getNbMinutesFromTime(arg_period);
							
							if(periodInMinutes != null && periodInMinutes < 5){
								p.sendMessage(ChatColor.RED + "La période doit durer 5 minutes minimum.");
								return true;
							}else{
								period = arg_period;
							}
							
						} catch (Exception e1) {
							
							p.sendMessage(ChatColor.RED + "Valeur period incorrecte : ["+e1.getMessage()+"]");
							return true;
						}
					}
					
					RegionLoader loadedRegion = null;
					
					try{
						loadedRegion = RegionLoader.loadRegion(this, p, p.getWorld(), args[1]);
					}catch(Exception ex){
						p.sendMessage(ChatColor.RED + "Erreur : "+ex.getMessage());
					}
					
					
					if(loadedRegion.realMinPoint.getBlockY() - ydiff < 0){
						p.sendMessage(ChatColor.RED + "Y-Diff ne doit pas être supérieur à la couche la plus basse.");
						return true;
					}


					RMAction action = new RMAction(this, loadedRegion, p, p.getWorld(), wallBlockMaterial, period, ydiff, nolava, bcast, forceemerald);
					
					action.sendRecap(cmd);
					
					return true;
				}

				if(args[0].toLowerCase().startsWith("res")){
					
					String historyNode = "regens."+p.getWorld().getName()+"."+args[1];
					
					if(!getHistoryFile().isSet(historyNode)){
						p.sendMessage(ChatColor.RED + "Cette mine n'a jamais été régénérée !");
						return true;
					}
					
					getHistoryFile().set(historyNode+".nextRegen", -1);
					getHistoryFile().set(historyNode+".period", null);
					
					saveHistoryFile();
					
					p.sendMessage(ChatColor.GREEN + "Le regen auto a été désactivé pour la mine '"+args[1]+"'");
					
					return true;
				}
				if(args[0].toLowerCase().startsWith("redo")){
					
					if(this.actionList.containsKey(p.getName())){
						p.sendMessage(ChatColor.RED + "Vous avez déjà entré une commande de regen.");
						p.sendMessage(ChatColor.RED + "Pour annuler le précédent regen : "+cmd+" cancel");
						return true;
					}
					
					if(args.length < 2){
						p.sendMessage(ChatColor.RED + cmd + " redo <region_name>");
						return true;
					}
					
					String historyNode = "regens."+p.getWorld().getName()+"."+args[1];
					
					if(!getHistoryFile().isSet(historyNode)){
						p.sendMessage(ChatColor.RED + "Cette mine n'a jamais été régénérée !");
						return true;
					}
					
					/* Hésitation d'empêcher un redo si last regen pas fini ...
					if(!getHistoryFile().getBoolean(historyNode+".finished", false)){
						p.sendMessage(ChatColor.RED + "La dernière régénération ne s'est pas terminée, redo impossible.");
						return true;
					}
					*/
					
					RMAction action = redoAction(historyNode, p, p.getWorld(), args[1]);

					action.sendRecap(cmd);
					
					return true;
				}
				
				if(args[0].toLowerCase().startsWith("sta")){
					
					
					if(this.actionList.containsKey(p.getName())){
						
						RMAction action = this.actionList.get(p.getName());
						
						if(action != null && !action.started){
							
							p.sendMessage(ChatColor.AQUA + "Regen lancé, pour l'annuler : '"+ChatColor.YELLOW+cmd+" cancel"+ChatColor.AQUA+"'.");
							
							action.init();
							return true;
						}
						
						if(action != null && action.started){
							p.sendMessage(ChatColor.RED + "Regen déjà lancé ...");
							return true;
						}
						
					}
					
					p.sendMessage(ChatColor.RED + "Vous n'avez pas planifié de regen.");
					return true;
				}
				
				if(args[0].toLowerCase().startsWith("c") || args[0].toLowerCase().startsWith("sto")){
					
					if(this.actionList.containsKey(p.getName())){
						
						RMAction action = this.actionList.get(p.getName());
						
						if(action != null){
							
							action.stop();
							return true;
						}
						
					}
					
					p.sendMessage(ChatColor.RED + "Vous n'avez pas planifié/lancé de regen.");
					return true;
					
				}
				
				
				sendCommands(p, cmd);
				
				
			}
			
		}else{
			sender.sendMessage("Regen3000 ne peut être utilisé que par un joueur, vu que les régions sont propres au monde du joueur.");
		}
		
		return false;
	}

	private RMAction redoAction(String historyNode, Player p, World world, String regionName) {
		
		Material wallBlockMaterial = Material.getMaterial(getHistoryFile().getInt(historyNode+".wallID", defaultWallBlockMaterial.getId()));
		String period = getHistoryFile().getString(historyNode+".period");
		int ydiff =  getHistoryFile().getInt(historyNode+".ydiff", 0);
		boolean nolava = getHistoryFile().getBoolean(historyNode+".nolava", defaultNoLava);
		boolean bcast = getHistoryFile().getBoolean(historyNode+".bcast", defaultBroadcast);
		boolean forceemerald = getHistoryFile().getBoolean(historyNode+".forceemerald", false);
		
		if(p == null) bcast = true;
		
		RegionLoader loadedRegion = null;
		try{
			loadedRegion = RegionLoader.loadRegion(this, p, world, regionName);
		}catch(Exception ex){
			if(p != null)
				p.sendMessage(ChatColor.RED + "Erreur : "+ex.getMessage());
			else
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[GRAVE][Regen3000] Erreur lors du régen auto de la mine '"+regionName+"' : "+ex.getMessage());
		}
		
		RMAction action = new RMAction(this, loadedRegion, p, world, wallBlockMaterial, period, ydiff, nolava, bcast, forceemerald);
		
		return action;
	}

	private void sendMainCommand(Player p, String cmd) {
		p.sendMessage(ChatColor.RED + cmd + " regen <region_name> [wall_ID:"+this.defaultWallBlockMaterial.getId()+"] [y-diff:0] [no-lava:"+this.defaultNoLava+"] [broadcast:"+this.defaultBroadcast+"] [period:1w3d10h40m]");
	}


	private void sendCommands(Player p, String cmd) {
		sendMainCommand(p, cmd);
		p.sendMessage(ChatColor.RED + cmd + " reset <region_name> {permet d'annuler un regen périodique}");
		p.sendMessage(ChatColor.RED + cmd + " redo <region_name> {permet de refaire un regen avec les derniers params}");
		p.sendMessage(ChatColor.RED + cmd + " start {démarre la dernière commande de regen}");
		p.sendMessage(ChatColor.RED + cmd + " cancel {annule la dernière commande de regen}");
	}
	
	
	public static class RegionLoader{
				
		public ProtectedRegion WGregion;
		public Region regionContainer;
		public BlockVector2D chunkMin;
		public BlockVector2D chunkMax;
		public BlockVector realMinPoint;
		public BlockVector realMaxPoint;
		
		public int sizeX;
		public int sizeY;
		public int sizeZ;
		
		public RegionLoader(ProtectedRegion WGregion, Region regionContainer, BlockVector2D chunkMin, BlockVector2D chunkMax, BlockVector realMinPoint, BlockVector realMaxPoint){
			this.WGregion = WGregion;
			this.regionContainer = regionContainer;
			this.chunkMin = chunkMin;
			this.chunkMax = chunkMax;
			this.realMinPoint = realMinPoint;
			this.realMaxPoint = realMaxPoint;
			
			sizeX = realMaxPoint.getBlockX() - realMinPoint.getBlockX() + 1;
			sizeY = realMaxPoint.getBlockY() - realMinPoint.getBlockY() + 1;
			sizeZ = realMaxPoint.getBlockZ() - realMinPoint.getBlockZ() + 1;
			
		}
		
		static RegionLoader loadRegion(RMPlugin plugin, Player p, World w, String regionParam) throws Exception{
			ProtectedRegion region = plugin.worldGuard.getRegionManager(w).getRegion(regionParam);

			if(region == null){
				throw new Exception("Nom de région invalide !");
			}

			Selection selection;
			
			if ((region instanceof ProtectedCuboidRegion)) {
				ProtectedCuboidRegion cuboid = (ProtectedCuboidRegion)region;
				Vector pt1 = cuboid.getMinimumPoint();
				Vector pt2 = cuboid.getMaximumPoint();
				selection = new CuboidSelection(w, pt1, pt2);
				if(p != null) plugin.worldEdit.setSelection(p, selection);
			} else if ((region instanceof ProtectedPolygonalRegion)) {
				ProtectedPolygonalRegion poly2d = (ProtectedPolygonalRegion)region;
				selection = new Polygonal2DSelection(w, poly2d.getPoints(), poly2d.getMinimumPoint().getBlockY(), poly2d.getMaximumPoint().getBlockY());

				if(p != null) plugin.worldEdit.setSelection(p, selection);
			} else {

				throw new Exception("Région invalide à cause de sa forme... Seuls les cuboids et poly sont supportés.");

			}
			
			Region regionContainer = null;
			
			try {
				
				RegionSelector rs = selection.getRegionSelector();				
				regionContainer = rs.getRegion();
				
			} catch (IncompleteRegionException e1) {
				throw new Exception("Erreur 'impossible' numéro 1.");
			}

			if(regionContainer==null){
				throw new Exception("Erreur 'impossible' numéro 2.");
			}


			BlockVector minPoint = region.getMinimumPoint();
			BlockVector maxPoint = region.getMaximumPoint();



			int xMin = minPoint.getBlockX() < maxPoint.getBlockX() ? minPoint.getBlockX() : maxPoint.getBlockX();
			int xMax = minPoint.getBlockX() > maxPoint.getBlockX() ? minPoint.getBlockX() : maxPoint.getBlockX();

			int zMin = minPoint.getBlockZ() < maxPoint.getBlockZ() ? minPoint.getBlockZ() : maxPoint.getBlockZ();
			int zMax = minPoint.getBlockZ() > maxPoint.getBlockZ() ? minPoint.getBlockZ() : maxPoint.getBlockZ();

			int yMin = minPoint.getBlockY() < maxPoint.getBlockY() ? minPoint.getBlockY() : maxPoint.getBlockY();
			int yMax = minPoint.getBlockY() > maxPoint.getBlockY() ? minPoint.getBlockY() : maxPoint.getBlockY();





			BlockVector realMinPoint = new BlockVector(xMin, yMin, zMin);
			BlockVector realMaxPoint = new BlockVector(xMax, yMax, zMax);

			BlockVector2D chunkMin = new BlockVector2D(xMin >> 4, zMin >> 4);
			BlockVector2D chunkMax = new BlockVector2D(xMax >> 4, zMax >> 4);
			
			return new RegionLoader(region, regionContainer, chunkMin, chunkMax, realMinPoint, realMaxPoint);
		}
		
	}
	
	public static class CronRunnable implements Runnable{

		private RMPlugin plugin;
		private FileConfiguration h;


		public CronRunnable(RMPlugin plugin){
			this.plugin = plugin;
			this.h = plugin.getHistoryFile();
		}
		
		
		@Override
		public void run() {
			
			long thisTime = System.currentTimeMillis();
			
			ConfigurationSection cs = h.getConfigurationSection("regens");
			
			if(cs == null) return;
			
			for(String world : cs.getKeys(false)){
				
				ConfigurationSection cs2 = h.getConfigurationSection("regens."+world);
				
				for(String rName : cs2.getKeys(false)){
					
					String node = "regens."+world+"."+rName;
					
					long nextRegen = h.getLong(node+".nextRegen", -1);
					boolean finished = h.getBoolean(node+".finished", false);
					
					if(nextRegen != -1 && nextRegen < thisTime && finished){
						if(Bukkit.getWorld(world) != null){
							final RMAction action = plugin.redoAction(node, null, Bukkit.getWorld(world), rName);
							
							Bukkit.broadcastMessage(ChatColor.AQUA+"[Regen3000] Régénération auto de la mine '"+rName+"' initialisée...");
							
							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){

								@Override
								public void run() {
									action.init();
								}});
							
						}
						
					}else if(nextRegen != -1 && nextRegen <= (thisTime+5*60*1000) && finished){
						
						 double remainingTime = (nextRegen-thisTime)/1000;
						 
						 long nbMin = Math.round(Math.ceil(remainingTime/60D));
						 
						 Bukkit.broadcastMessage(ChatColor.GOLD + "[Regen3000] Régen auto de la mine '"+rName+"' dans "+nbMin+" minute"+(nbMin>1 ? "s" : "")+" ! Quittez la zone avant qu'il ne soit trop tard !");
						 
					}
					
				}
				
			}
			
		}
		
	}
	
}
