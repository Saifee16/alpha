package family.safe.data

import family.safe.policy.Category
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class SettingsStoreTest {
    @Test fun settingsAndPinAttemptsSurviveNewRepository() {
        val file = Files.createTempDirectory("family-test").resolve("settings.json").toFile()
        val store = SettingsStore(file)
        val initial = store.load()
        assertTrue(initial.categories.none { it.enabled })
        assertTrue(initial.sources.none { it.enabled })
        val state = initial.copy(pinHash="opaque-test-record", categories=listOf(Category("custom","Biology",setOf("plant cells"),true)), blocked=setOf("custom block"), attempts=family.safe.settings.PinAttempts(3,9000), requests=listOf(ApprovalRequest("r","unknown subject","pending")))
        store.save(state)
        assertEquals(state, SettingsStore(file).load())
    }
    @Test fun corruptExistingSettingsNeverResetParentSetup() {
        val file = Files.createTempFile("family-corrupt", ".json").toFile()
        file.writeText("{broken")
        assertThrows(Exception::class.java) { SettingsStore(file).load() }
        file.writeText("{}")
        assertThrows(Exception::class.java) { SettingsStore(file).load() }
    }
    @Test fun missingPinCannotUnlockAnAlreadyConfiguredTablet() {
        val file = Files.createTempDirectory("family-pin").resolve("settings.json").toFile()
        val store = SettingsStore(file)
        assertThrows(IllegalArgumentException::class.java) {
            store.save(FamilySettings(categories=listOf(Category("science","Science",setOf("science"),true))))
        }
    }
    @Test fun overlappingRepositoryWritersLeaveOneCompleteSettingsFile() {
        val dir=Files.createTempDirectory("family-overlap")
        val file=dir.resolve("settings.json").toFile()
        val pool=java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val futures=(1..8).map { i -> pool.submit { SettingsStore(file).save(FamilySettings(pinHash="hash-$i")) } }
            futures.forEach { it.get(30,java.util.concurrent.TimeUnit.SECONDS) }
            assertTrue(SettingsStore(file).load().pinHash!!.startsWith("hash-"))
            assertEquals(listOf("settings.json"),dir.toFile().listFiles()!!.map{it.name})
        } finally {pool.shutdownNow()}
    }
}
