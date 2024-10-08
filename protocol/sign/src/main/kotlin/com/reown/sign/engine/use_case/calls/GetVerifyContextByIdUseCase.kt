package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.storage.verify.VerifyContextStorageRepository
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toEngineDO

internal class GetVerifyContextByIdUseCase(private val verifyContextStorageRepository: VerifyContextStorageRepository) : GetVerifyContextByIdUseCaseInterface {
    override suspend fun getVerifyContext(id: Long): EngineDO.VerifyContext? = verifyContextStorageRepository.get(id)?.toEngineDO()
}

internal interface GetVerifyContextByIdUseCaseInterface {
    suspend fun getVerifyContext(id: Long): EngineDO.VerifyContext?
}