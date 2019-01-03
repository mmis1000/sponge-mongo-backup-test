package testmongo.testmongo

import com.google.inject.Inject
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.gridfs.GridFSBuckets
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.event.game.state.GameStartedServerEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.plugin.PluginContainer
import testmongo.testmongo.command.Backup
import testmongo.testmongo.command.BackupMongo
import testmongo.testmongo.command.BackupMongoAsync


@Plugin(id = "test-mongo", name = "Test Mongo")
class TestMongo {
    companion object {
        val client = MongoClient(MongoClientURI("mongodb://localhost"))
        val database = client.getDatabase("test-grid")
        val bukkit = GridFSBuckets.create(database, "saves")

        val asyncClient = com.mongodb.async.client.MongoClients.create("mongodb://localhost")
        val asyncDatabase = asyncClient.getDatabase("test-grid")
        val asyncBukkit = com.mongodb.async.client.gridfs.GridFSBuckets.create(asyncDatabase, "saves")
        lateinit var serverThread: ExecutorCoroutineDispatcher
            private set
    }

    @Inject()
    lateinit var plugin: PluginContainer

    @Listener
    fun onServerStart(event: GameStartedServerEvent) {
    }


    @Listener
    fun onPreInit(event: GamePreInitializationEvent) {
        serverThread = Sponge.getScheduler().createSyncExecutor(this).asCoroutineDispatcher()
        Sponge.getCommandManager().register(plugin, Backup.spec, "backup")
        Sponge.getCommandManager().register(plugin, BackupMongo.spec, "backup-mongo")
        Sponge.getCommandManager().register(plugin, BackupMongoAsync.spec, "backup-mongo-async")
    }
}