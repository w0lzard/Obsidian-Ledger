package com.ryuken.obsidianledger.core.preferences

import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.Security.errSecSuccess

/**
 * Keychain-backed [Settings] — used for the Supabase session store so tokens are
 * encrypted-at-rest on iOS (parity with Android's EncryptedSharedPreferences).
 *
 * Values are stored as generic-password Keychain items (service = this class,
 * account = key) with UTF-8 string payloads; non-string primitives are string-encoded.
 * A key index in NSUserDefaults backs [keys]/[size]/[clear] enumeration, because
 * enumerating Keychain items by attribute query is disproportionate for a settings store.
 * // ponytail: index-based enumeration; switch to kSecMatchLimitAll attribute queries if
 * // this ever backs more than session + a few app preferences.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainSettings(
    private val service: String = "com.ryuken.obsidianledger.keychain"
) : Settings {

    private val indexDefaults = NSUserDefaults.standardUserDefaults
    private val index: MutableSet<String> = (
        indexDefaults.arrayForKey(INDEX_KEY).orEmpty().filterIsInstance<String>()
        ).toMutableSet()

    // ── Keychain primitives ───────────────────────────────────────────

    private fun MemScopeDict() = CFDictionaryCreateMutable(null, 0L, null, null)

    private fun baseQuery(account: String?): CFDictionaryRef? = memScoped {
        val query = MemScopeDict()
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(
            query, kSecAttrService, CFStringCreateWithCString(null, service, UTF8_ENCODING)
        )
        if (account != null) {
            CFDictionaryAddValue(
                query, kSecAttrAccount, CFStringCreateWithCString(null, account, UTF8_ENCODING)
            )
        }
        query
    }

    private fun readRaw(key: String): String? = memScoped {
        val query = baseQuery(key)!!
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        if (data.length == 0uL) "" else data.bytes?.readBytes(data.length.toInt())?.decodeToString()
    }

    private fun keychainContains(key: String): Boolean =
        SecItemCopyMatching(baseQuery(key), null) == errSecSuccess

    private fun writeRaw(key: String, value: String) = memScoped {
        val bytes = value.encodeToByteArray().toUByteArray()
        val add = baseQuery(key)!!
        if (keychainContains(key)) {
            val update = MemScopeDict()
            bytes.usePinned { pinned ->
                val cfData = CFDataCreate(null, pinned.addressOf(0), bytes.size.toLong())
                CFDictionaryAddValue(update, kSecValueData, cfData)
            }
            SecItemUpdate(baseQuery(key), update)
        } else {
            bytes.usePinned { pinned ->
                val cfData = CFDataCreate(null, pinned.addressOf(0), bytes.size.toLong())
                CFDictionaryAddValue(add, kSecValueData, cfData)
            }
            SecItemAdd(add, null)
        }
        index.add(key)
        saveIndex()
    }

    private fun deleteRaw(key: String) {
        SecItemDelete(baseQuery(key))
        index.remove(key)
        saveIndex()
    }

    private fun saveIndex() {
        indexDefaults.setObject(index.toTypedArray(), forKey = INDEX_KEY)
    }

    // ── Settings interface ────────────────────────────────────────────

    override val keys: Set<String> get() = index.toSet()
    override val size: Int get() = index.size

    override fun clear() {
        index.toList().forEach { SecItemDelete(baseQuery(it)) }
        index.clear()
        saveIndex()
    }

    override fun remove(key: String) = deleteRaw(key)
    override fun hasKey(key: String): Boolean = keychainContains(key)

    override fun putString(key: String, value: String) = writeRaw(key, value)
    override fun getString(key: String, default: String): String = readRaw(key) ?: default
    override fun getStringOrNull(key: String): String? = readRaw(key)

    override fun putInt(key: String, value: Int) = writeRaw(key, value.toString())
    override fun getInt(key: String, default: Int): Int = readRaw(key)?.toIntOrNull() ?: default
    override fun getIntOrNull(key: String): Int? = readRaw(key)?.toIntOrNull()

    override fun putLong(key: String, value: Long) = writeRaw(key, value.toString())
    override fun getLong(key: String, default: Long): Long = readRaw(key)?.toLongOrNull() ?: default
    override fun getLongOrNull(key: String): Long? = readRaw(key)?.toLongOrNull()

    override fun putFloat(key: String, value: Float) = writeRaw(key, value.toString())
    override fun getFloat(key: String, default: Float): Float = readRaw(key)?.toFloatOrNull() ?: default
    override fun getFloatOrNull(key: String): Float? = readRaw(key)?.toFloatOrNull()

    override fun putDouble(key: String, value: Double) = writeRaw(key, value.toString())
    override fun getDouble(key: String, default: Double): Double = readRaw(key)?.toDoubleOrNull() ?: default
    override fun getDoubleOrNull(key: String): Double? = readRaw(key)?.toDoubleOrNull()

    override fun putBoolean(key: String, value: Boolean) = writeRaw(key, value.toString())
    override fun getBoolean(key: String, default: Boolean): Boolean =
        readRaw(key)?.toBooleanStrictOrNull() ?: default
    override fun getBooleanOrNull(key: String): Boolean? = readRaw(key)?.toBooleanStrictOrNull()

    private companion object {
        // kCFStringEncodingUTF8
        const val UTF8_ENCODING = 0x08000100u
        const val INDEX_KEY = "com.ryuken.obsidianledger.keychain.index"
    }
}
