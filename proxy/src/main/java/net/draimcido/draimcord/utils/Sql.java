package net.draimcido.draimcord.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.draimcido.draimcord.DraimCord;
import net.draimcido.draimcord.DraimCordUser;
import net.draimcido.draimcord.config.Settings;
import net.md_5.bungee.BungeeCord;


public class Sql
{

    private final DraimCord draimCord;
    private Connection connection;
    private boolean connecting = false;
    private long nextCleanUp = System.currentTimeMillis() + ( 60000 * 60 * 2 ); // + 2 hours

    private final ExecutorService executor = Executors.newFixedThreadPool( 2, new ThreadFactoryBuilder().setNameFormat( "DraimCord-SQL-%d" ).build() );
    private final Logger logger = BungeeCord.getInstance().getLogger();

    public Sql(DraimCord draimCord)
    {
        this.draimCord = draimCord;
        setupConnect();
    }

    private void setupConnect()
    {

        try
        {
            connecting = true;
            if ( executor.isShutdown() )
            {
                return;
            }
            if ( connection != null && connection.isValid( 3 ) )
            {
                return;
            }
            logger.info( "§8[§6DraimCord§8]§f Connecting to the database..." );
            long start = System.currentTimeMillis();
            if ( Settings.IMP.SQL.STORAGE_TYPE.equalsIgnoreCase( "mysql" ) )
            {
                Settings.SQL s = Settings.IMP.SQL;
                connectToDatabase( String.format( "JDBC:mysql://%s:%s/%s?useSSL=false&useUnicode=true&characterEncoding=utf-8", s.HOSTNAME, String.valueOf( s.PORT ), s.DATABASE ), s.USER, s.PASSWORD );
            } else
            {
                Class.forName( "org.sqlite.JDBC" );
                connectToDatabase( "JDBC:sqlite:DraimCord/database.db", null, null );
            }
            logger.log( Level.INFO, "§8[§6DraimCord§8] §fПодключено ({0} мс)", System.currentTimeMillis() - start );
            createTable();
            alterLastJoinColumn();
            clearOldUsers();
            loadUsers();
        } catch ( SQLException | ClassNotFoundException e )
        {
            logger.log( Level.WARNING, "Can not connect to database or execute sql: ", e );
            connection = null;
        } finally
        {
            connecting = false;
        }
    }

    private void connectToDatabase(String url, String user, String password) throws SQLException
    {
        this.connection = DriverManager.getConnection( url, user, password );
    }

    private void createTable() throws SQLException
    {
        String sql = "CREATE TABLE IF NOT EXISTS `Users` ("
                + "`Name` VARCHAR(16) NOT NULL PRIMARY KEY UNIQUE,"
                + "`Ip` VARCHAR(16) NOT NULL,"
                + "`LastCheck` BIGINT NOT NULL,"
                + "`LastJoin` BIGINT NOT NULL);";

        try ( PreparedStatement statement = connection.prepareStatement( sql ) )
        {
            statement.executeUpdate();
        }
    }

    private void alterLastJoinColumn()
    {
        try ( ResultSet rs = connection.getMetaData().getColumns( null, null, "Users", "LastJoin" ) )
        {
            if ( !rs.next() )
            {
                try ( Statement st = connection.createStatement() )
                {
                    st.executeUpdate( "ALTER TABLE `Users` ADD COLUMN `LastJoin` BIGINT NOT NULL DEFAULT 0;" );
                    st.executeUpdate( "UPDATE `Users` SET LastJoin = LastCheck" );
                }
            }
        } catch ( Exception e )
        {
            logger.log( Level.WARNING, "§8[§6DraimCord§8]§f Ошибка при добавлении столбца в таблицу", e );
        }
    }

    private void clearOldUsers() throws SQLException
    {
        if ( Settings.IMP.SQL.PURGE_TIME <= 0 )
        {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.DATE, -Settings.IMP.SQL.PURGE_TIME );
        long until = calendar.getTimeInMillis();
        try ( PreparedStatement statement = connection.prepareStatement( "SELECT `Name` FROM `Users` WHERE `LastJoin` < " + until + ";" ) )
        {
            ResultSet set = statement.executeQuery();
            while ( set.next() )
            {
                draimCord.removeUser( set.getString( "Name" ) );
            }
        }
        if ( this.connection != null )
        {
            try ( PreparedStatement statement = connection.prepareStatement( "DELETE FROM `Users` WHERE `LastJoin` < " + until + ";" ) )
            {
                logger.log( Level.INFO, "§8[§6DraimCord§8]§f Очищено {0} аккаунтов", statement.executeUpdate() );
            }
        }
    }

    private void loadUsers() throws SQLException
    {
        try ( PreparedStatement statament = connection.prepareStatement( "SELECT * FROM `Users`;" );
              ResultSet set = statament.executeQuery() )
        {
            int i = 0;
            while ( set.next() )
            {
                String name = set.getString( "Name" );
                String ip = set.getString( "Ip" );
                if ( isInvalidName( name ) )
                {
                    removeUser( "REMOVE FROM `Users` WHERE `Ip` = '" + ip + "' AND `LastCheck` = '" + set.getLong( "LastCheck" ) + "';" );
                    continue;
                }
                long lastCheck = set.getLong( "LastCheck" );
                long lastJoin = set.getLong( "LastJoin" );
                DraimCordUser draimCordUser = new DraimCordUser( name, ip, lastCheck, lastJoin );
                draimCord.addUserToCache( draimCordUser );
                i++;
            }
            logger.log( Level.INFO, "§8[§6DraimCord§8]§f Белый список игроков успешно загружен ({0})", i );
        }
    }

    private boolean isInvalidName(String name)
    {
        return name.contains( "'" ) || name.contains( "\"" );
    }

    private void removeUser(String sql)
    {
        if ( connection != null )
        {
            this.executor.execute( () ->
            {
                try ( PreparedStatement statament = connection.prepareStatement( sql ) )
                {
                    statament.execute();
                } catch ( SQLException ignored )
                {
                }
            } );
        }
    }

    public void saveUser(DraimCordUser draimCordUser)
    {
        if ( connecting || isInvalidName( draimCordUser.getName() ) )
        {
            return;
        }
        if ( connection != null )
        {
            this.executor.execute( () ->
            {
                final long timestamp = System.currentTimeMillis();
                String sql = "SELECT `Name` FROM `Users` where `Name` = '" + draimCordUser.getName() + "' LIMIT 1;";
                try ( Statement statament = connection.createStatement();
                      ResultSet set = statament.executeQuery( sql ) )
                {
                    if ( !set.next() )
                    {
                        sql = "INSERT INTO `Users` (`Name`, `Ip`, `LastCheck`, `LastJoin`) VALUES "
                                + "('" + draimCordUser.getName() + "','" + draimCordUser.getIp() + "',"
                                + "'" + draimCordUser.getLastCheck() + "','" + draimCordUser.getLastJoin() + "');";
                        statament.executeUpdate( sql );
                    } else
                    {
                        sql = "UPDATE `Users` SET `Ip` = '" + draimCordUser.getIp() + "', `LastCheck` = '" + draimCordUser.getLastCheck() + "',"
                                + " `LastJoin` = '" + draimCordUser.getLastJoin() + "' where `Name` = '" + draimCordUser.getName() + "';";
                        statament.executeUpdate( sql );
                    }
                } catch ( SQLException ex )
                {
                    logger.log( Level.WARNING, "§8[§6DraimCord§8]§f Не могу выполнить запрос к базе данных", ex );
                    logger.log( Level.WARNING, sql );
                    executor.execute( () -> setupConnect() );
                }
            } );
        }
    }

    public void tryCleanUP()
    {
        if ( Settings.IMP.SQL.PURGE_TIME > 0 && nextCleanUp - System.currentTimeMillis() <= 0 )
        {
            nextCleanUp = System.currentTimeMillis() + ( 60000 * 60 * 2 ); // + 2 hours
            try
            {
                clearOldUsers();
            } catch ( SQLException ex )
            {
                setupConnect();
                logger.log( Level.WARNING, "§8[§6DraimCord§8]§f Не могу очистить пользователей", ex );
            }
        }
    }

    public void close()
    {
        this.executor.shutdownNow();
        try
        {
            if ( connection != null )
            {
                this.connection.close();
            }
        } catch ( SQLException ignore )
        {
        }
        this.connection = null;
    }
}
