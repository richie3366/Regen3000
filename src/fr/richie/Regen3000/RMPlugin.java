package fr.richie.Regen3000;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import fr.richie.Regen3000.RMUtils.Period;

public class RMPlugin extends JavaPlugin{
	
	
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
		
		Bukkit.getPluginManager().registerEvents(new RMPluginListener(this), this);
		
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

				if(args[0].toLowerCase().startsWith("r")){
					
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
							}else if(argname.equals("period")){
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
					
					Period per = null;
					// TODO : archivage dernier regen même si pas periode, possibilité de redo sans param
					// TODO : supprimer l'objet period, faire le cron min-by-min, etc..
					// TODO : faire l'archivage A LA FIN ou mettre une variable started/finished dans le node
					
					if(arg_period != null){
						
						try {
							per = Period.valueOf(arg_period);
						} catch (Exception e1) {
							
							p.sendMessage(ChatColor.RED + "Valeur period incorrecte : ["+e1.getMessage()+"]");
							return true;
						}
						
					}
					
					
					ProtectedRegion region = this.worldGuard.getRegionManager(p.getWorld()).getRegion(args[1]);

					if(region == null){
						p.sendMessage(ChatColor.RED + "Nom de région invalide !");
						return true;
					}


					if ((region instanceof ProtectedCuboidRegion)) {
						ProtectedCuboidRegion cuboid = (ProtectedCuboidRegion)region;
						Vector pt1 = cuboid.getMinimumPoint();
						Vector pt2 = cuboid.getMaximumPoint();
						CuboidSelection selection = new CuboidSelection(p.getWorld(), pt1, pt2);
						this.worldEdit.setSelection(p, selection);
					} else if ((region instanceof ProtectedPolygonalRegion)) {
						ProtectedPolygonalRegion poly2d = (ProtectedPolygonalRegion)region;
						Polygonal2DSelection selection = new Polygonal2DSelection(p.getWorld(), poly2d.getPoints(), poly2d.getMinimumPoint().getBlockY(), poly2d.getMaximumPoint().getBlockY());

						this.worldEdit.setSelection(p, selection);
					} else {

						p.sendMessage(ChatColor.RED + "Région invalide à cause de sa forme... Seuls les cuboids et poly sont supportés.");
						return true;

					}

					LocalSession sess = this.worldEdit.getSession(p);

					Region laregion = null;

					try {
						laregion = sess.getSelection(sess.getSelectionWorld());
					} catch (IncompleteRegionException e1) {
						p.sendMessage(ChatColor.RED + "Erreur 'impossible' numéro 1.");
						return true;
					}

					if(laregion==null){
						p.sendMessage(ChatColor.RED + "Erreur 'impossible' numéro 2.");
						return true;
					}


					BlockVector minPoint = region.getMinimumPoint();
					BlockVector maxPoint = region.getMaximumPoint();



					int xMin = minPoint.getBlockX() < maxPoint.getBlockX() ? minPoint.getBlockX() : maxPoint.getBlockX();
					int xMax = minPoint.getBlockX() > maxPoint.getBlockX() ? minPoint.getBlockX() : maxPoint.getBlockX();

					int zMin = minPoint.getBlockZ() < maxPoint.getBlockZ() ? minPoint.getBlockZ() : maxPoint.getBlockZ();
					int zMax = minPoint.getBlockZ() > maxPoint.getBlockZ() ? minPoint.getBlockZ() : maxPoint.getBlockZ();

					int yMin = minPoint.getBlockY() < maxPoint.getBlockY() ? minPoint.getBlockY() : maxPoint.getBlockY();
					int yMax = minPoint.getBlockY() > maxPoint.getBlockY() ? minPoint.getBlockY() : maxPoint.getBlockY();


					if(yMin - ydiff < 0){
						p.sendMessage(ChatColor.RED + "Y-Diff ne doit pas être supérieur à la couche la plus basse.");
					}


					BlockVector realMinPoint = new BlockVector(xMin, yMin, zMin);
					BlockVector realMaxPoint = new BlockVector(xMax, yMax, zMax);

					BlockVector2D chunkMin = new BlockVector2D(xMin >> 4, zMin >> 4);
					BlockVector2D chunkMax = new BlockVector2D(xMax >> 4, zMax >> 4);


					RMAction action = new RMAction(this, region, laregion, p, p.getWorld(), chunkMin, chunkMax, realMinPoint, realMaxPoint, wallBlockMaterial, per, ydiff, nolava, bcast);
					
					String complement = "sans mettre de murs autour.";
					
					if(action.wallBlockMaterial.getId()>0)
						complement = "en mettant un mur de "+action.wallBlockMaterial.toString()+" autour.";
					
					p.sendMessage(ChatColor.GOLD + "Vous avez l'intention de regénérer la mine contenue par la region WG '"+region.getId()+"', d'une taille de "+(xMax-xMin+1)+"x"+(yMax-yMin+1)+"x"+(zMax-zMin+1)+" [yDiff="+action.ydiff+", no-lava="+nolava+", broadcast="+bcast+"], "+complement);
					p.sendMessage(ChatColor.AQUA + "Pour lancer le regen, entrez la commande '"+ChatColor.YELLOW+cmd+" start"+ChatColor.AQUA+"'.");
					
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
	
}
