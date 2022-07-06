package net.draimcido.draimcord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import net.draimcido.draimcord.config.Settings;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class DraimCordCommand extends Command
{

    public DraimCordCommand()
    {
        super( "draimcord", null, "dcord" );
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if ( sender instanceof ProxiedPlayer )
        {
            sendStat( sender );
            return;
        }
        if ( args.length == 0 )
        {
            sender.sendMessage( " §6DraimCord §cv" + Settings.IMP.DRAIMCORD_VERSION + "" );
            sender.sendMessage( "§r> §2draimcord reload §7- §fПерезагружить конфиг" );
            sender.sendMessage( "§r> §2draimcord stat §7- §fПоказать статистику" );
            sender.sendMessage( "§r> §2draimcord export §7- §fВыгрузить список игроков, которые прошли проверку" );
            sender.sendMessage( "§r> §2draimcord protection on/off §7- §fВключить или выключить ручной режим 'под атакой'" );
        } else if ( args[0].equalsIgnoreCase( "reload" ) )
        {
            BungeeCord.getInstance().getDraimCord().disable();
            BungeeCord.getInstance().setDraimCord( new DraimCord( false ) );
            sender.sendMessage( "§fКоманда выполнена" );
        } else if ( args[0].equalsIgnoreCase( "stat" ) || args[0].equalsIgnoreCase( "stats" ) || args[0].equalsIgnoreCase( "info" ) )
        {
            sendStat( sender );
        } else if ( args[0].equalsIgnoreCase( "export" ) )
        {
            export( sender, args );
            sender.sendMessage( "§fКоманда выполнена" );
        } else if ( args[0].equalsIgnoreCase( "protection" ) )
        {
            if ( args.length >= 2 )
            {
                boolean enable = args[1].equalsIgnoreCase( "on" );
                BungeeCord.getInstance().getDraimCord().setForceProtectionEnabled( enable );
                sender.sendMessage( "§fЗащита " + ( enable ? "включена" : "§cотключена" ) );
            }
        }
    }

    private void sendStat(CommandSender sender)
    {
        DraimCord draimCord = BungeeCord.getInstance().getDraimCord();
        sender.sendMessage( "§6DraimCord §cv" + Settings.IMP.DRAIMCORD_VERSION + " " );
        sender.sendMessage( "§fОбнаружена атака: " + ( draimCord.isUnderAttack() ? "§cДа" : "§aНет" ) + "§fБотов на проверке: " + draimCord.getOnlineOnFilter() + "§fПрошло проверку: " + draimCord.getUsersCount() );
    }

    private void export(CommandSender sender, String[] args)
    {
        DraimCord draimCord = BungeeCord.getInstance().getDraimCord();

        if ( args.length == 1 )
        {
            sender.sendMessage( "§2draimcord export [TIME_IN_SECONDS] §7- §fвыгрузить список тех, кто прошёл"
                    + " проверку за указаное время. укажите ALL чтобы получить за всё время." );
            sender.sendMessage( "§2draimcord export [TIME_IN_SECONDS] JOIN §7- §fвыгрузить список тех,"
                    + " кто зашёл на сервер за указанное время (Учитывает и тех кто  также и прошёл проверку)." );
            return;
        }
        if ( args[1].equalsIgnoreCase( "all" ) )
        {
            List<String> out = new ArrayList<>( draimCord.getUsersCount() );
            draimCord.getUserCache().values().forEach( value ->
                    out.add( value.getName() + "|" + value.getIp() + "|" + value.getLastCheck() + "|" + value.getLastJoin() )
            );
            exportToFile( out, args.length >= 3 && args[2].equalsIgnoreCase( "join" ) );
            return;
        }
        try
        {
            int seconds = Integer.parseInt( args[1] );
            boolean join = args.length >= 3 && args[2].equalsIgnoreCase( "join" );
            Calendar calendar = Calendar.getInstance();
            calendar.add( Calendar.SECOND, -seconds );
            long until = calendar.getTimeInMillis();

            List<String> out = new ArrayList<>( draimCord.getUsersCount() );
            draimCord.getUserCache().values().forEach( value ->
                    {
                        if ( join )
                        {
                            if ( value.getLastJoin() >= until )
                            {
                                out.add( value.getName() + "|" + value.getIp() + "|" + value.getLastCheck() + "|" + value.getLastJoin() );
                            }
                        } else if ( value.getLastCheck() >= until )
                        {
                            out.add( value.getName() + "|" + value.getIp() + "|" + value.getLastCheck() + "|" + value.getLastJoin() );
                        }
                    }
            );
            exportToFile( out, join );
        } catch ( Exception e )
        {
            sender.sendMessage( "§fУкажите число" );
        }
    }

    private void exportToFile(List<String> out, boolean join)
    {
        Path outFile = new File( "DraimCord", "whitelist.out." + ( join ? "join" : "" ) + ".txt" ).toPath();
        try
        {
            Files.write( outFile, out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
        } catch ( IOException e )
        {
            BungeeCord.getInstance().getLogger().log( Level.WARNING, "§8[§6DraimCord§8] §fCould not export ip's to file", e );
        }
    }

}
