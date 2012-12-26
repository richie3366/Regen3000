package fr.richie.Regen3000;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.v1_4_6.ConvertProgressUpdater;
import net.minecraft.server.v1_4_6.Convertable;
import net.minecraft.server.v1_4_6.EntityTracker;
import net.minecraft.server.v1_4_6.EnumGamemode;
import net.minecraft.server.v1_4_6.MinecraftServer;
import net.minecraft.server.v1_4_6.ServerNBTManager;
import net.minecraft.server.v1_4_6.WorldLoaderServer;
import net.minecraft.server.v1_4_6.WorldManager;
import net.minecraft.server.v1_4_6.WorldServer;
import net.minecraft.server.v1_4_6.WorldSettings;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.v1_4_6.CraftServer;
import org.bukkit.generator.ChunkGenerator;


public class RMUtils {
	static World createWorld(String wName, boolean forceemerald){

		WorldCreator creator = WorldCreator.name(wName.toLowerCase()).environment(Environment.NORMAL).seed(new Random().nextLong()).generateStructures(false);

		CraftServer cs = (CraftServer)Bukkit.getServer();

		MinecraftServer console = cs.getServer();


		String name = creator.name();
		ChunkGenerator generator = creator.generator();
		File folder = new File(cs.getWorldContainer(), name);
		World world = cs.getWorld(name);
		net.minecraft.server.v1_4_6.WorldType type = net.minecraft.server.v1_4_6.WorldType.getType(creator.type().getName());
		boolean generateStructures = creator.generateStructures();

		if (world != null) {
			return null; //world;
		}

		if ((folder.exists()) && (!folder.isDirectory())) {
			throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");

		}

		if (generator == null) {
			generator = cs.getGenerator(name);
		}

		Convertable converter = new WorldLoaderServer(cs.getWorldContainer());
		if (converter.isConvertable(name)) {
			cs.getLogger().info("Converting world '" + name + "'");
			converter.convert(name, new ConvertProgressUpdater(console));
		}

		int dimension = 10 + console.worlds.size();
		boolean used = false;
		do
			for (WorldServer server : console.worlds) {
				used = server.dimension == dimension;
				if (used) {
					dimension++;
					break;
				}
			}
		while (used);
		boolean hardcore = false;

		WorldServer internal = new WorldServer(console, new ServerNBTManager(cs.getWorldContainer(), name, true), name, dimension, new WorldSettings(creator.seed(), EnumGamemode.a(cs.getDefaultGameMode().getValue()), generateStructures, hardcore, type), console.methodProfiler, creator.environment(), generator);

		//cs.getHandle().getServer().getServer()
		
		//cs.getW
		
		if (cs.getWorld(name.toLowerCase())==null) {
			return null;
		}

		if(forceemerald){
			internal.worldProvider.d = new RMWorldChunkManager(internal);
		}
		
		internal.worldMaps = ((WorldServer)console.worlds.get(0)).worldMaps;

		internal.tracker = new EntityTracker(internal);
		internal.addIWorldAccess(new WorldManager(console, internal));
		internal.difficulty = 1;
		internal.setSpawnFlags(true, true);
		console.worlds.add(internal);

		if (generator != null) {
			internal.getWorld().getPopulators().addAll(generator.getDefaultPopulators(internal.getWorld()));
		}

		//Bukkit.getPluginManager().callEvent(new WorldInitEvent(internal.getWorld()));
		System.out.print("NOT Preparing start region for level " + (console.worlds.size() - 1) + " (Seed: " + internal.getSeed() + ")");


		//Bukkit.getPluginManager().callEvent(new WorldLoadEvent(internal.getWorld()));
		World created = internal.getWorld();

		return created;
	}


	static void removeWorld(String wName){
		World w = Bukkit.getWorld(wName);
		if(w!=null){

			//File folder = w.getWorldFolder();

			//Bukkit.broadcastMessage(folder.getAbsolutePath());

			Bukkit.getServer().unloadWorld(w, false);

			//forceRemoveWorld(wName);


			//folder.
			//delete(folder);
			//folder.renameTo(new File(Bukkit.getServer().getWorldContainer(), "corbeille/"+wName+"_"+(new Random().nextInt())));
		}

	}

	static void delete(File f) {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
		//if (!
		f.delete();
		//);
	}

	@Deprecated
	static void forceRemoveWorld(String wName){

		//Bukkit.broadcastMessage("rm -Rf "+Bukkit.getServer().getWorldContainer().getAbsolutePath()+"/"+wName);

		if(wName.equals(".") || wName.equals("..") || wName.equals("") || wName.contains("/")) return;


		try {
			if(File.separator.equals("/")){
				Runtime.getRuntime().exec(new String[]{"rm", "-Rf", Bukkit.getServer().getWorldContainer().getAbsolutePath()+"/"+wName });
			}else{
				Runtime.getRuntime().exec("CMD /C DEL /F /S /Q \""+Bukkit.getServer().getWorldContainer().getAbsolutePath()+"\\"+wName+"\"");				
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	public static void cleanRegenWorlds() {

		File worldContainer = Bukkit.getWorldContainer();

		if (worldContainer.isDirectory()) {
			for (File c : worldContainer.listFiles()){
				if(c.isDirectory() && c.getName().toLowerCase().startsWith("rmregen_")){
					delete(c);
				}
			}
		}
	}

	public static long getNbMinutesFromTime(String str) throws Exception{
		Pattern timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?(?:([0-9]+)\\s*(?:s[a-z]*)?)?", 2);

		Matcher m = timePattern.matcher(str);
		int years = 0;
		int months = 0;
		int weeks = 0;
		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		boolean found = false;
		while (m.find())
		{
			if ((m.group() == null) || (m.group().isEmpty()))
			{
				continue;
			}
			for (int i = 0; i < m.groupCount(); i++)
			{
				if ((m.group(i) == null) || (m.group(i).isEmpty()))
					continue;
				found = true;
				break;
			}

			if (!found)
				continue;
			if ((m.group(1) != null) && (!m.group(1).isEmpty()))
			{
				years = Integer.parseInt(m.group(1));
			}
			if ((m.group(2) != null) && (!m.group(2).isEmpty()))
			{
				months = Integer.parseInt(m.group(2));
			}
			if ((m.group(3) != null) && (!m.group(3).isEmpty()))
			{
				weeks = Integer.parseInt(m.group(3));
			}
			if ((m.group(4) != null) && (!m.group(4).isEmpty()))
			{
				days = Integer.parseInt(m.group(4));
			}
			if ((m.group(5) != null) && (!m.group(5).isEmpty()))
			{
				hours = Integer.parseInt(m.group(5));
			}
			if ((m.group(6) != null) && (!m.group(6).isEmpty()))
			{
				minutes = Integer.parseInt(m.group(6));
			}
			if ((m.group(7) == null) || (m.group(7).isEmpty()))
				break;
			seconds = Integer.parseInt(m.group(7));
		}

		if (!found)
		{
			throw new Exception("Format incorrect");
		}

		if(seconds > 0){
			throw new Exception("Secondes interdites");
		}

		if(years > 0){
			throw new Exception("Années interdites");
		}			

		if(months > 0){
			throw new Exception("Mois interdits");
		}

		int nbMinutes = (weeks*7+days)*24*60+hours*60+minutes;

		return nbMinutes;

	}

	public static long getFutureTime(String time) throws Exception
	{
		boolean future = true;

		Pattern timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?(?:([0-9]+)\\s*(?:s[a-z]*)?)?", 2);

		Matcher m = timePattern.matcher(time);
		int years = 0;
		int months = 0;
		int weeks = 0;
		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		boolean found = false;
		while (m.find())
		{
			if ((m.group() == null) || (m.group().isEmpty()))
			{
				continue;
			}
			for (int i = 0; i < m.groupCount(); i++)
			{
				if ((m.group(i) == null) || (m.group(i).isEmpty()))
					continue;
				found = true;
				break;
			}

			if (!found)
				continue;
			if ((m.group(1) != null) && (!m.group(1).isEmpty()))
			{
				years = Integer.parseInt(m.group(1));
			}
			if ((m.group(2) != null) && (!m.group(2).isEmpty()))
			{
				months = Integer.parseInt(m.group(2));
			}
			if ((m.group(3) != null) && (!m.group(3).isEmpty()))
			{
				weeks = Integer.parseInt(m.group(3));
			}
			if ((m.group(4) != null) && (!m.group(4).isEmpty()))
			{
				days = Integer.parseInt(m.group(4));
			}
			if ((m.group(5) != null) && (!m.group(5).isEmpty()))
			{
				hours = Integer.parseInt(m.group(5));
			}
			if ((m.group(6) != null) && (!m.group(6).isEmpty()))
			{
				minutes = Integer.parseInt(m.group(6));
			}
			if ((m.group(7) == null) || (m.group(7).isEmpty()))
				break;
			seconds = Integer.parseInt(m.group(7));
		}

		if (!found)
		{
			throw new Exception("Format incorrect");
		}


		Calendar c = new GregorianCalendar();
		if (years > 0)
		{
			c.add(1, years * (future ? 1 : -1));
		}
		if (months > 0)
		{
			c.add(2, months * (future ? 1 : -1));
		}
		if (weeks > 0)
		{
			c.add(3, weeks * (future ? 1 : -1));
		}
		if (days > 0)
		{
			c.add(5, days * (future ? 1 : -1));
		}
		if (hours > 0)
		{
			c.add(11, hours * (future ? 1 : -1));
		}
		if (minutes > 0)
		{
			c.add(12, minutes * (future ? 1 : -1));
		}
		if (seconds > 0)
		{
			c.add(13, seconds * (future ? 1 : -1));
		}
		return c.getTimeInMillis();


	}
}
