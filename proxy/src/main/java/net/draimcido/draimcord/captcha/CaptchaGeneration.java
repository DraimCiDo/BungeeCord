package net.draimcido.draimcord.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import net.draimcido.draimcord.caching.CachedCaptcha;
import net.draimcido.draimcord.caching.PacketUtils;
import net.draimcido.draimcord.captcha.generator.CaptchaPainter;
import net.draimcido.draimcord.captcha.generator.map.CraftMapCanvas;
import net.draimcido.draimcord.captcha.generator.map.MapPalette;
import net.draimcido.draimcord.packets.MapDataPacket;
import net.md_5.bungee.BungeeCord;

@UtilityClass
public class CaptchaGeneration
{
    Random rnd = new Random();

    public void generateImages()
    {
        Font[] fonts = new Font[]
                {
                        new Font( Font.SANS_SERIF, Font.PLAIN, 50 ),
                        new Font( Font.SERIF, Font.PLAIN, 50 ),
                        new Font( Font.MONOSPACED, Font.BOLD, 50 )
                };

        ExecutorService executor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
        CaptchaPainter painter = new CaptchaPainter();
        MapPalette.prepareColors();
        for ( int i = 100; i <= 999; i++ )
        {
            executor.execute( () ->
            {
                String answer = randomAnswer();
                BufferedImage image = painter.draw( fonts[rnd.nextInt( fonts.length )], randomNotWhiteColor(), answer );
                final CraftMapCanvas map = new CraftMapCanvas();
                map.drawImage( 0, 0, image );
                MapDataPacket packet = new MapDataPacket( 0, (byte) 0, map.getMapData() );
                PacketUtils.captchas.createCaptchaPacket( packet, answer );
            } );
        }

        long start = System.currentTimeMillis();
        ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;
        while ( ex.getActiveCount() != 0 )
        {
            BungeeCord.getInstance().getLogger().log( Level.INFO, "§8[§6DraimCord§8] §fГенерация Анти-Бот параметров (Подождите не много)" );
            try
            {
                Thread.sleep( 1000L );
            } catch ( InterruptedException ex1 )
            {
                BungeeCord.getInstance().getLogger().log( Level.WARNING, "§r[§rDraimCord§r] §rНе могу сгенерировать анти-бот. Вырубаю сервер", ex1 );
                System.exit( 0 );
                return;
            }
        }
        CachedCaptcha.generated = true;
        executor.shutdownNow();
        System.gc();
        BungeeCord.getInstance().getLogger().log( Level.INFO, "§8[§6DraimCord§8] §fПараметры для Анти-Бот системы сгенерированы за {0} мс", System.currentTimeMillis() - start );
    }


    private Color randomNotWhiteColor()
    {
        Color color = MapPalette.colors[rnd.nextInt( MapPalette.colors.length )];

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        if ( r == 255 && g == 255 && b == 255 )
        {
            return randomNotWhiteColor();
        }
        if ( r == 220 && g == 220 && b == 220 )
        {
            return randomNotWhiteColor();
        }
        if ( r == 199 && g == 199 && b == 199 )
        {
            return randomNotWhiteColor();
        }
        if ( r == 255 && g == 252 && b == 245 )
        {
            return randomNotWhiteColor();
        }
        if ( r == 220 && g == 217 && b == 211 )
        {
            return randomNotWhiteColor();
        }
        if ( r == 247 && g == 233 && b == 163 )
        {
            return randomNotWhiteColor();
        }
        return color;
    }

    private String randomAnswer()
    {
        if ( rnd.nextBoolean() )
        {
            return Integer.toString( rnd.nextInt( ( 99999 - 10000 ) + 1 ) + 10000 );
        } else
        {
            return Integer.toString( rnd.nextInt( ( 9999 - 1000 ) + 1 ) + 1000 );
        }
    }
}
