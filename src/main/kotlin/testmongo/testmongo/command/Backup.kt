package testmongo.testmongo.command

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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class Backup : CommandExecutor {
    companion object {
        val spec: CommandSpec = CommandSpec.builder()
                .executor(Backup())
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

        val zipFile = savesDir.absolutePath + File.separator + info.worldName + ".zip"

        GlobalScope.launch {
            try {
                val start = System.currentTimeMillis()
                val fileOutputStream = FileOutputStream(zipFile)
                val zipOutputStream = ZipOutputStream(fileOutputStream)
                addDir(worldDir, zipOutputStream)
                zipOutputStream.close()
                val end = System.currentTimeMillis()

                launch(TestMongo.serverThread) {
                    src.sendMessage("Finished in ${end - start} ms".spongeText)
                }
            } catch (e: IOException) {
                launch(TestMongo.serverThread) {
                    src.sendMessage(e.message?.spongeText ?: "Unknown error".spongeText)
                }
                e.printStackTrace()
            }
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