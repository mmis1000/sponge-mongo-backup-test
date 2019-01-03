package testmongo.testmongo.command

import com.mongodb.async.client.gridfs.helpers.AsyncStreamHelper.toAsyncInputStream
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.world.storage.WorldInfo
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.text.Text
import testmongo.testmongo.TestMongo
import testmongo.testmongo.extensions.spongeText
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class BackupMongoAsync : CommandExecutor {
    companion object {
        val spec: CommandSpec = CommandSpec.builder()
                .executor(BackupMongoAsync())
                .arguments(GenericArguments.world("worldName".spongeText))
                .description(Text.of("just don't give it a fuck"))
                .build()
    }

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        val info = args.getOne<WorldInfo>("worldName").get()

        val savesDir = Sponge.getGame().savesDirectory.toFile()
        val defaultWorld = Sponge.getServer().defaultWorldName

        val worldDir = if (info.worldName.equals(defaultWorld, ignoreCase = true)) {
            File(savesDir, info.worldName)
        } else {
            File(savesDir, defaultWorld + File.separator + info.worldName)
        }

        val fileName = info.worldName + "-" + System.currentTimeMillis().toString() + ".zip"

        try {
            val start = System.currentTimeMillis()
            // val fileOutputStream = FileOutputStream(zipFile)

            val options = GridFSUploadOptions()
                    .chunkSizeBytes(512 * 1024)

            val byteArrayOutputStream = ByteArrayOutputStream()
            val zipOutputStream = ZipOutputStream(byteArrayOutputStream)
            addDir(worldDir, zipOutputStream)
            zipOutputStream.close()

            val asyncStream = toAsyncInputStream(byteArrayOutputStream.toByteArray())
            TestMongo.asyncBukkit.uploadFromStream(fileName, asyncStream, options) { id, err ->

                val end = System.currentTimeMillis()

                if (err != null) {
                    GlobalScope.launch(TestMongo.serverThread) {
                        src.sendMessage(err.message?.spongeText ?: "Unknown error".spongeText)
                    }
                    err.printStackTrace()
                } else {
                    GlobalScope.launch(TestMongo.serverThread) {
                        src.sendMessage("Finished in ${end - start} ms".spongeText)
                        src.sendMessage("The fileId of the uploaded file is: ${id.toHexString()}".spongeText)
                    }

                    asyncStream.close { result, t ->
                    }
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
            src.sendMessage(e.message?.spongeText ?: "Unknown error".spongeText)
        }

        return CommandResult.success()
    }


    @Throws(IOException::class)
    private fun addDir(directory: File, zipOutputStream: ZipOutputStream, rootPath: String = directory.absolutePath) {
        val files = directory.listFiles()
        val buffer = ByteArray(1024)

        for (i in files!!.indices) {
            if (files[i].isDirectory) {
                val name = files[i].name
                if (!Sponge.getServer().getWorldProperties(name).isPresent) {
                    addDir(files[i], zipOutputStream, rootPath)
                }
                continue
            }

            val fileInputStream = FileInputStream(files[i])

            val relativePath = files[i]
                    .absolutePath
                    .replace("$rootPath/", "")

            zipOutputStream.putNextEntry(ZipEntry(relativePath))

            while (true) {
                val length = fileInputStream.read(buffer)
                if (length <= 0) break
                zipOutputStream.write(buffer, 0, length)
            }

            zipOutputStream.closeEntry()
            fileInputStream.close()
        }
    }
}