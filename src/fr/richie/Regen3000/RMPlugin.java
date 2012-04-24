package fr.richie.Regen3000;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RMPlugin extends JavaPlugin{
	
	private File historyFile;
	private FileConfiguration history;
	
	public WorldEditPlugin worldEdit;
	public WorldGuardPlugin worldGuard;
	//public PermissionManager pManager;
	
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
					// TODO : faire le cron min-by-min
					
					if(arg_period != null){
						
						try {
							periodInMinutes = RMUtils.getNbMinutesFromTime(arg_period);
							
							if(periodInMinutes != null && periodInMinutes < 5){
								p.sendMessage(ChatColor.RED + "La période doit durer 5 minutes minimum.");
								return true;
							}
							
						} catch (Exception e1) {
							
							p.sendMessage(ChatColor.RED + "Valeur period incorrecte : ["+e1.getMessage()+"]");
							return true;
						}
					}
					
					RegionLoader loadedRegion = null;
					
					try{
						loadedRegion = RegionLoader.loadRegion(this, p, args[1]);
					}catch(Exception ex){
						p.sendMessage(ChatColor.RED + "Erreur : "+ex.getMessage());
					}
					
					
					if(loadedRegion.realMinPoint.getBlockY() - ydiff < 0){
						p.sendMessage(ChatColor.RED + "Y-Diff ne doit pas être supérieur à la couche la plus basse.");
						return true;
					}


					RMAction action = new RMAction(this, loadedRegion.WGregion, loadedRegion.regionContainer, p, p.getWorld(), loadedRegion.chunkMin, loadedRegion.chunkMax, loadedRegion.realMinPoint, loadedRegion.realMaxPoint, wallBlockMaterial, period, ydiff, nolava, bcast);
					
					action.sendRecap(loadedRegion, cmd);
					
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
					
					Material wallBlockMaterial = Material.getMaterial(getHistoryFile().getInt(historyNode+".wallID", defaultWallBlockMaterial.getId()));
					String period = getHistoryFile().getString(historyNode+".period");
					int ydiff =  getHistoryFile().getInt(historyNode+".ydiff", 0);
					boolean nolava = getHistoryFile().getBoolean(historyNode+".nolava", defaultNoLava);
					boolean bcast = getHistoryFile().getBoolean(historyNode+".bcast", defaultBroadcast);
					
					RegionLoader loadedRegion = null;
					try{
						loadedRegion = RegionLoader.loadRegion(this, p, args[1]);
					}catch(Exception ex){
						p.sendMessage(ChatColor.RED + "Erreur : "+ex.getMessage());
					}
					
					RMAction action = new RMAction(this, loadedRegion.WGregion, loadedRegion.regionContainer, p, p.getWorld(), loadedRegion.chunkMin, loadedRegion.chunkMax, loadedRegion.realMinPoint, loadedRegion.realMaxPoint, wallBlockMaterial, period, ydiff, nolava, bcast);
					
					action.sendRecap(loadedRegion, cmd);
					
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
			sender.sendMessage("Regen3000 ne peut être utilisé que par un joueur, pour des raisons pratiques.");
		}
		
		return false;
	}

	private void sendMainCommand(Player p, String cmd) {
		p.sendMessage(ChatColor.RED + cmd + " regen <region_name> [wall_ID:"+this.defaultWallBlockMaterial.getId()+"] [y-diff:0] [no-lava:"+this.defaultNoLava+"] [broadcast:"+this.defaultBroadcast+"] [period:1w3d10h40m]");
	}


	private void sendCommands(Player p, String cmd) {
		sendMainCommand(p, cmd);
		p.sendMessage(ChatColor.RED + cmd + " unschedule <region_name> [PAS ENCORE DISPO!]");
		p.sendMessage(ChatColor.RED + cmd + " start");
		p.sendMessage(ChatColor.RED + cmd + " cancel");
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
		
		static RegionLoader loadRegion(RMPlugin plugin, Player p, String regionParam) throws Exception{
			ProtectedRegion region = plugin.worldGuard.getRegionManager(p.getWorld()).getRegion(regionParam);

			if(region == null){
				throw new Exception("Nom de région invalide !");
			}


			if ((region instanceof ProtectedCuboidRegion)) {
				ProtectedCuboidRegion cuboid = (ProtectedCuboidRegion)region;
				Vector pt1 = cuboid.getMinimumPoint();
				Vector pt2 = cuboid.getMaximumPoint();
				CuboidSelection selection = new CuboidSelection(p.getWorld(), pt1, pt2);
				plugin.worldEdit.setSelection(p, selection);
			} else if ((region instanceof ProtectedPolygonalRegion)) {
				ProtectedPolygonalRegion poly2d = (ProtectedPolygonalRegion)region;
				Polygonal2DSelection selection = new Polygonal2DSelection(p.getWorld(), poly2d.getPoints(), poly2d.getMinimumPoint().getBlockY(), poly2d.getMaximumPoint().getBlockY());

				plugin.worldEdit.setSelection(p, selection);
			} else {

				throw new Exception("Région invalide à cause de sa forme... Seuls les cuboids et poly sont supportés.");

			}

			LocalSession sess = plugin.worldEdit.getSession(p);

			Region regionContainer = null;

			try {
				regionContainer = sess.getSelection(sess.getSelectionWorld());
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
	
}
