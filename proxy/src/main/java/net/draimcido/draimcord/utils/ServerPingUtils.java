package net.draimcido.draimcord.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import net.draimcido.draimcord.DraimCord;
import net.draimcido.draimcord.config.Settings;

public class ServerPingUtils
{

    private Cache<InetAddress, Boolean> pingList;
    private boolean enabled = Settings.IMP.SERVER_PING_CHECK.MODE != 2;

    private final DraimCord draimCord;

    public ServerPingUtils(DraimCord draimCord)
    {
        this.draimCord = draimCord;
        pingList = CacheBuilder.newBuilder()
                .concurrencyLevel( Runtime.getRuntime().availableProcessors() )
                .initialCapacity( 100 )
                .expireAfterWrite( Settings.IMP.SERVER_PING_CHECK.CACHE_TIME, TimeUnit.SECONDS )
                .build();
    }

    public boolean needKickOrRemove(InetAddress address)
    {
        boolean present = pingList.getIfPresent( address ) == null;
        if ( !present ) //Убрираем из мапы если есть уже есть в ней.
        {
            pingList.invalidate( address );
        }
        return present;
    }

    public void add(InetAddress address)
    {
        if ( enabled )
        {
            pingList.put( address, true );
        }
    }

    public boolean needCheck()
    {
        return enabled && ( Settings.IMP.SERVER_PING_CHECK.MODE == 0 || draimCord.isUnderAttack() );
    }

    public void clear()
    {
        pingList.invalidateAll();
    }

    public void cleanUP()
    {
        pingList.cleanUp();
    }
}
