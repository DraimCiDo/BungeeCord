package net.draimcido.draimcord.utils;

import java.util.logging.Level;
import lombok.Getter;
import net.md_5.bungee.BungeeCord;

public class FakeOnlineUtils
{

    @Getter
    private static FakeOnlineUtils instance;
    private boolean enabled = false;
    private float multiple = 1.0f;

    public FakeOnlineUtils()
    {
        instance = this;
        String boost = System.getProperty( "onlineBooster" );
        if ( boost == null )
        {
            return;
        }
        try
        {
            multiple = Float.parseFloat( boost );
        } catch ( NumberFormatException e )
        {
            BungeeCord.getInstance().getLogger().log( Level.WARNING, "§f[§6DraimCord§8] §fНе могу активировать фейк онлайн: {0}", e.getMessage() );
            return;
        }
        enabled = true;
    }

    public int getFakeOnline(int online)
    {
        return enabled ? Math.round( online * multiple ) : online;
    }

}
