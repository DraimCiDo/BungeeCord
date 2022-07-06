package net.draimcido.draimcord;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import net.draimcido.draimcord.caching.PacketUtils;
import net.draimcido.draimcord.caching.PacketsPosition;
import net.draimcido.draimcord.config.Settings;
import net.draimcido.draimcord.utils.FailedUtils;
import net.draimcido.draimcord.utils.ManyChecksUtils;
import net.md_5.bungee.BungeeCord;


/**
 * @author Leymooo
 */
public class DraimCordThread
{

    private static Thread thread;
    private static final HashSet<String> TO_REMOVE_SET = new HashSet<>();
    private static BungeeCord bungee = BungeeCord.getInstance();

    public static void start()
    {
        ( thread = new Thread( () ->
        {
            while ( sleep( 1000 ) )
            {
                try
                {
                    long currTime = System.currentTimeMillis();
                    for ( Map.Entry<String, Connector> entryset : bungee.getDraimCord().getConnectedUsersSet().entrySet() )
                    {
                        Connector connector = entryset.getValue();
                        if ( !connector.isConnected() )
                        {
                            TO_REMOVE_SET.add( entryset.getKey() );
                            continue;
                        }
                        DraimCord.CheckState state = connector.getState();
                        switch ( state )
                        {
                            case SUCCESSFULLY:
                            case FAILED:
                                TO_REMOVE_SET.add( entryset.getKey() );
                                continue;
                            default:
                                if ( ( currTime - connector.getJoinTime() ) >= Settings.IMP.TIME_OUT )
                                {
                                    connector.failed( PacketUtils.KickType.TIMED_OUT, state == DraimCord.CheckState.CAPTCHA_ON_POSITION_FAILED
                                            ? "Too long fall check" : "Captcha not entered" );
                                    TO_REMOVE_SET.add( entryset.getKey() );
                                    continue;
                                } else if ( state == DraimCord.CheckState.CAPTCHA_ON_POSITION_FAILED || state == DraimCord.CheckState.ONLY_POSITION )
                                {
                                    connector.sendMessage( PacketsPosition.CHECKING_MSG );
                                } else
                                {
                                    connector.sendMessage( PacketsPosition.CHECKING_CAPTCHA_MSG );
                                }
                                connector.sendPing();
                        }
                    }

                } catch ( Exception e )
                {
                    bungee.getLogger().log( Level.WARNING, "§8[§6DraimCord§8] §fНепонятная ошибка. Пожалуйста отправте ёё разработчику!", e );
                } finally
                {
                    if ( !TO_REMOVE_SET.isEmpty() )
                    {
                        for ( String remove : TO_REMOVE_SET )
                        {
                            bungee.getDraimCord().removeConnection( remove, null );
                        }
                        TO_REMOVE_SET.clear();
                    }
                }
            }

        }, "DraimCord thread" ) ).start();
    }

    public static void stop()
    {
        if ( thread != null )
        {
            thread.interrupt();
        }
    }

    private static boolean sleep(long time)
    {
        try
        {
            Thread.sleep( time );
        } catch ( InterruptedException ex )
        {
            return false;
        }
        return true;
    }

    public static void startCleanUpThread()
    {
        Thread t = new Thread( () ->
        {
            byte counter = 0;
            while ( !Thread.interrupted() && sleep( 5 * 1000 ) )
            {
                if ( ++counter == 12 )
                {
                    counter = 0;
                    ManyChecksUtils.cleanUP();
                    if ( bungee.getDraimCord() != null )
                    {
                        DraimCord draimCord = bungee.getDraimCord();
                        if ( draimCord.getServerPingUtils() != null )
                        {
                            draimCord.getServerPingUtils().cleanUP();
                        }
                        if ( draimCord.getSql() != null )
                        {
                            draimCord.getSql().tryCleanUP();
                        }
                        if ( draimCord.getGeoIp() != null )
                        {
                            draimCord.getGeoIp().tryClenUP();
                        }
                    }
                }
                FailedUtils.flushQueue();
            }
        }, "CleanUp thread" );
        t.setDaemon( true );
        t.start();
    }
}
