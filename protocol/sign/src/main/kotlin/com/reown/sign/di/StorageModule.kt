@file:JvmSynthetic

package com.reown.sign.di

import com.reown.android.di.sdkBaseStorageModule
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.di.deleteDatabase
import app.cash.sqldelight.ColumnAdapter
import com.reown.sign.SignDatabase
import com.reown.sign.storage.authenticate.AuthenticateResponseTopicRepository
import com.reown.sign.storage.data.dao.namespace.NamespaceDao
import com.reown.sign.storage.data.dao.optionalnamespaces.OptionalNamespaceDao
import com.reown.sign.storage.data.dao.proposal.ProposalDao
import com.reown.sign.storage.data.dao.proposalnamespace.ProposalNamespaceDao
import com.reown.sign.storage.data.dao.session.SessionDao
import com.reown.sign.storage.data.dao.temp.TempNamespaceDao
import com.reown.sign.storage.link_mode.LinkModeStorageRepository
import com.reown.sign.storage.proposal.ProposalStorageRepository
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import com.reown.android.internal.common.scope as wcScope

@JvmSynthetic
internal fun storageModule(dbName: String): Module = module {
    includes(sdkBaseStorageModule(SignDatabase.Schema, dbName))

    fun Scope.createSignDB(): SignDatabase = SignDatabase(
        driver = get(named(dbName)),
        NamespaceDaoAdapter = NamespaceDao.Adapter(
            accountsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            methodsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            eventsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            chainsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST))
        ),
        TempNamespaceDaoAdapter = TempNamespaceDao.Adapter(
            accountsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            methodsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            eventsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            chainsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST))
        ),
        ProposalNamespaceDaoAdapter = ProposalNamespaceDao.Adapter(
            chainsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            methodsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            eventsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST))
        ),
        OptionalNamespaceDaoAdapter = OptionalNamespaceDao.Adapter(
            chainsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            methodsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            eventsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST))
        ),
        SessionDaoAdapter = SessionDao.Adapter(
            propertiesAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_MAP)),
            scoped_propertiesAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_MAP)),
            transport_typeAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_TRANSPORT_TYPE))
        ),
        ProposalDaoAdapter = ProposalDao.Adapter(
            propertiesAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_MAP)),
            scoped_propertiesAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_MAP)),
            iconsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST)),
            authenticationAdapter = object : ColumnAdapter<List<String>, String> {
                private val moshi by lazy { get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)).build() }
                private val listType by lazy { Types.newParameterizedType(List::class.java, String::class.java) }
                private val adapter by lazy { moshi.adapter<List<String>>(listType) }

                override fun decode(databaseValue: String): List<String> =
                    if (databaseValue.isBlank()) emptyList() else runCatching { adapter.fromJson(databaseValue) ?: emptyList() }.getOrDefault(emptyList())

                override fun encode(value: List<String>): String = adapter.toJson(value)
            }
        )
    )

    single {
        try {
            createSignDB().also { signDatabase ->
                wcScope.launch {
                    try {
                        signDatabase.sessionDaoQueries.lastInsertedRow().executeAsOneOrNull()
                    } catch (e: Exception) {
                        deleteDatabase(dbName)
                        createSignDB()
                    }
                }
            }
        } catch (e: Exception) {
            deleteDatabase(dbName)
            createSignDB()
        }
    }

    single {
        get<SignDatabase>().sessionDaoQueries
    }

    single {
        get<SignDatabase>().namespaceDaoQueries
    }

    single {
        get<SignDatabase>().tempNamespaceDaoQueries
    }

    single {
        get<SignDatabase>().proposalNamespaceDaoQueries
    }

    single {
        get<SignDatabase>().optionalNamespaceDaoQueries
    }

    single {
        get<SignDatabase>().proposalDaoQueries
    }

    single {
        get<SignDatabase>().authenticateResponseTopicDaoQueries
    }

    single {
        get<SignDatabase>().linkModeDaoQueries
    }

    single {
        SessionStorageRepository(
            sessionDaoQueries = get(),
            namespaceDaoQueries = get(),
            requiredNamespaceDaoQueries = get(),
            optionalNamespaceDaoQueries = get(),
            tempNamespaceDaoQueries = get()
        )
    }

    single {
        ProposalStorageRepository(
            proposalDaoQueries = get(),
            requiredNamespaceDaoQueries = get(),
            optionalNamespaceDaoQueries = get(),
            moshi = get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)).build()
        )
    }

    single {
        AuthenticateResponseTopicRepository(authenticateResponseTopicDaoQueries = get())
    }

    single {
        LinkModeStorageRepository(linkModeDaoQueries = get())
    }
}
