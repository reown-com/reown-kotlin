@file:JvmSynthetic

package com.reown.sign.di

import com.reown.android.di.sdkBaseStorageModule
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.di.deleteDatabase
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
            iconsAdapter = get(named(AndroidCommonDITags.COLUMN_ADAPTER_LIST))
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
            optionalNamespaceDaoQueries = get()
        )
    }

    single {
        AuthenticateResponseTopicRepository(authenticateResponseTopicDaoQueries = get())
    }

    single {
        LinkModeStorageRepository(linkModeDaoQueries = get())
    }
}
