package com.writingapp.data.git

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class GitOperations(private val context: Context) {

    data class GitStatus(
        val isRepo: Boolean = false,
        val branch: String = "",
        val hasUncommitted: Boolean = false,
        val lastCommitMessage: String = "",
        val error: String = ""
    )

    private fun findRepoRoot(): File? {
        val dirs = listOf(
            context.getExternalFilesDir(null),
            context.filesDir
        )
        for (dir in dirs) {
            if (dir != null) {
                var current: File? = dir
                while (current?.parentFile != null) {
                    if (File(current, ".git").exists()) {
                        return current
                    }
                    current = current.parentFile
                }
            }
        }
        return null
    }

    private fun openRepo(): Git? {
        val repoDir = findRepoRoot() ?: return null
        return try {
            val repo: Repository = FileRepositoryBuilder()
                .setGitDir(File(repoDir, ".git"))
                .readEnvironment()
                .findGitDir(repoDir)
                .build()
            Git(repo)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getStatus(): GitStatus = withContext(Dispatchers.IO) {
        val git = openRepo() ?: return@withContext GitStatus(error = "No git repository found")
        try {
            val status = git.status().call()
            val log = git.log().setMaxCount(1).call()
            val lastMsg = log.firstOrNull()?.shortMessage ?: ""
            val branch = git.repository.branch ?: "main"

            GitStatus(
                isRepo = true,
                branch = branch,
                hasUncommitted = status.hasUncommittedChanges(),
                lastCommitMessage = lastMsg
            )
        } catch (e: Exception) {
            GitStatus(error = e.message ?: "Unknown error")
        } finally {
            git.close()
        }
    }

    suspend fun initRepo(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            dir.mkdirs()
            Git.init().setDirectory(dir).call().use { git ->
                val file = File(dir, ".gitignore")
                if (!file.exists()) {
                    file.writeText("*.iml\n.gradle\n/local.properties\n/.idea\n.DS_Store\n/build\n/captures\n.externalNativeBuild\n.cxx\n*.apk\n*.ap_\n*.aab\n")
                }
                git.add().addFilepattern(".").call()
                git.commit().setMessage("Initial commit").call()
            }
            Result.success("Repository initialized at $path")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commit(message: String, authorName: String = "Writer", authorEmail: String = "writer@app"): Result<String> = withContext(Dispatchers.IO) {
        val git = openRepo()
        if (git == null) return@withContext Result.failure(Exception("No git repository"))
        try {
            git.add().addFilepattern(".").call()
            val commitResult = git.commit()
                .setMessage(message)
                .setAuthor(authorName, authorEmail)
                .call()
            Result.success("Committed: ${commitResult.abbreviate(8).name()}")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            git.close()
        }
    }

    suspend fun push(remoteUrl: String, username: String = "", password: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val git = openRepo()
        if (git == null) return@withContext Result.failure(Exception("No git repository"))
        try {
            val pushCommand = git.push()
                .setRemote(remoteUrl)
                .setForce(false)
            if (username.isNotBlank()) {
                pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
            }
            val results = pushCommand.call()
            val successCount = results.count { it.remoteUpdates.isNotEmpty() }
            Result.success("Pushed $successCount ref(s)")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            git.close()
        }
    }

    suspend fun pull(remoteUrl: String, username: String = "", password: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val git = openRepo()
        if (git == null) return@withContext Result.failure(Exception("No git repository"))
        try {
            val pullResult = git.pull()
                .setRemote(remoteUrl)
            if (username.isNotBlank()) {
                pullResult.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
            }
            val result = pullResult.call()
            val merged = result.mergeResult?.mergeStatus?.name ?: "already up-to-date"
            Result.success("Pull: $merged")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            git.close()
        }
    }

    suspend fun log(maxCount: Int = 10): Result<List<String>> = withContext(Dispatchers.IO) {
        val git = openRepo()
        if (git == null) return@withContext Result.failure(Exception("No git repository"))
        try {
            val logs = git.log().setMaxCount(maxCount).call()
            val entries = logs.map { commit ->
                "${commit.abbreviate(8).name()} ${commit.authorIdent.name}: ${commit.shortMessage}"
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            git.close()
        }
    }
}
