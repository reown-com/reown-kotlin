package com.reown.android.internal.common.storage.verify

import android.database.sqlite.SQLiteException
import com.reown.android.internal.common.model.Validation
import com.reown.android.sdk.storage.data.dao.VerifyContextQueries
import com.reown.android.verify.model.VerifyContext
import com.reown.foundation.util.Logger

class VerifyContextStorageRepository(private val verifyContextQueries: VerifyContextQueries, private val logger: Logger) {

    @Throws(SQLiteException::class)
    suspend fun insertOrAbort(verifyContext: VerifyContext) = with(verifyContext) {
        verifyContextQueries.insertOrAbortVerifyContext(id, origin, validation, verifyUrl, isScam)
    }

    suspend fun get(id: Long): VerifyContext? {
        return try {
            verifyContextQueries.getVerifyContextById(id, mapper = this::toVerifyContext).executeAsOneOrNull()
        } catch (e: Exception) {
            null
        }
    }

    @Throws(SQLiteException::class)
    suspend fun getAll(): List<VerifyContext> {
        return verifyContextQueries.geListOfVerifyContexts(mapper = this::toVerifyContext).executeAsList()
    }

    suspend fun delete(id: Long) {
        try {
            verifyContextQueries.deleteVerifyContext(id)
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    private fun toVerifyContext(id: Long, origin: String, validation: Validation, verifyUrl: String, isScam: Boolean?): VerifyContext =
        VerifyContext(id, origin, validation, verifyUrl, isScam)
}