package net.md_5.bungee;

import java.io.IOException;
import java.security.Security;
import java.util.Arrays;
import java.util.logging.Level;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.command.ConsoleCommandSender;

public class BungeeCordLauncher
{

    public static void main(String[] args) throws Exception
    {
        Security.setProperty( "networkaddress.cache.ttl", "30" );
        Security.setProperty( "networkaddress.cache.negative.ttl", "10" );
        // For JDK9+ we force-enable multi-release jar file support #3087
        if ( System.getProperty( "jdk.util.jar.enableMultiRelease" ) == null )
        {
            System.setProperty( "jdk.util.jar.enableMultiRelease", "force" );
        }
        System.setProperty( "java.awt.headless", "true" ); //BotFilter

        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.acceptsAll( Arrays.asList( "help" ), "Show the help" );
        parser.acceptsAll( Arrays.asList( "v", "version" ), "Print version and exit" );
        parser.acceptsAll( Arrays.asList( "noconsole" ), "Disable console input" );

        OptionSet options = parser.parse( args );

        if ( options.has( "help" ) )
        {
            parser.printHelpOn( System.out );
            return;
        }
        if ( options.has( "version" ) )
        {
            System.out.println( BungeeCord.class.getPackage().getImplementationVersion() );
            return;
        }

        BungeeCord bungee = new BungeeCord();
        ProxyServer.setInstance( bungee );
        printASCII();
        bungee.start();

        if ( !options.has( "noconsole" ) )
        {
            String line;
            while ( bungee.isRunning && ( line = bungee.getConsoleReader().readLine( ">" ) ) != null )
            {
                if ( !bungee.getPluginManager().dispatchCommand( ConsoleCommandSender.getInstance(), line ) )
                {
                    bungee.getConsole().sendMessage( new ComponentBuilder( "Command not found" ).color( ChatColor.RED ).create() ); //BotFilter
                }
            }
        }
    }

    public static void printASCII() throws Exception {
        BungeeCord bungee = new BungeeCord();
        bungee.getLogger().log(Level.WARNING, " §6_____     ______     ______     __     __    __     ______     ______     ______     _____    ");
        bungee.getLogger().log(Level.WARNING, " §6/\\  __-.  /\\  == \\   /\\  __ \\   /\\ \\   /\\ \"-./  \\   /\\  ___\\   /\\  __ \\   /\\  == \\   /\\  __-.  ");
        bungee.getLogger().log(Level.WARNING, " §6\\ \\ \\/\\ \\ \\ \\  __<   \\ \\  __ \\  \\ \\ \\  \\ \\ \\-./\\ \\  \\ \\ \\____  \\ \\ \\/\\ \\  \\ \\  __<   \\ \\ \\/\\ \\ ");
        bungee.getLogger().log(Level.WARNING, " §6 \\ \\____-  \\ \\_\\ \\_\\  \\ \\_\\ \\_\\  \\ \\_\\  \\ \\_\\ \\ \\_\\  \\ \\_____\\  \\ \\_____\\  \\ \\_\\ \\_\\  \\ \\____- ");
        bungee.getLogger().log(Level.WARNING, " §6  \\/____/   \\/_/ /_/   \\/_/\\/_/   \\/_/   \\/_/  \\/_/   \\/_____/   \\/_____/   \\/_/ /_/   \\/____/ ");
        bungee.getLogger().log(Level.WARNING, "                                                                                                ");
        bungee.getLogger().log(Level.WARNING, "                                                                §6by DraimGooSe        ");
        bungee.getLogger().log(Level.WARNING, "                                                                                               ");
    }
}
